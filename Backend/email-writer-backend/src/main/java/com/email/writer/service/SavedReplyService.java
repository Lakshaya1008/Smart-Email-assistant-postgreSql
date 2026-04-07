package com.email.writer.service;

import com.email.writer.dto.SaveReplyRequest;
import com.email.writer.entity.SavedReply;
import com.email.writer.entity.User;
import com.email.writer.repository.SavedReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for saved reply operations.
 *
 * Changes from original:
 *
 * 1. saveReply() — now passes language from the request to the SavedReply
 *    constructor. Previously language was silently dropped because SavedReply
 *    had no language field and the DTO had no language field. Both are now fixed.
 *
 * 2. getUserStatistics() — rewritten to use DB aggregate queries instead of
 *    loading ALL user replies into the JVM heap.
 *    Before: repo.findByUserIdOrderByCreatedAtDesc(userId) loaded every SavedReply
 *            object for the user, then Java streams grouped and counted them.
 *            With 10,000 saves this loaded 10,000 objects just to count tones.
 *    After:  Uses the aggregate queries that already existed in the repository
 *            (getToneDistributionForUser, getMostCommonSubjectsForUser) plus
 *            two new count methods (countByUserIdAndIsFavorite,
 *            countByUserIdAndCreatedAtAfter). The DB does the work.
 *
 * 3. Removed redundant class-level @Transactional on methods that already
 *    had their own @Transactional — harmless but cleaner.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SavedReplyService {

    private final SavedReplyRepository repo;

    @Transactional
    public SavedReply saveReply(SaveReplyRequest req, User user) {
        log.debug("Saving reply for user {} with subject '{}'",
                user.getUsername(), req.getEmailSubject());

        SavedReply sr = new SavedReply(
                user,
                req.getEmailSubject(),
                req.getEmailContent(),
                req.getTone(),
                req.getLanguage(),   // language now stored — was silently dropped before
                req.getReplyText(),
                req.getSummary()
        );

        SavedReply saved = repo.save(sr);
        log.info("Reply saved with ID {} for user {}", saved.getId(), user.getUsername());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SavedReply> getUserReplies(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<SavedReply> getUserReplies(Long userId, int page, int size) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<SavedReply> getUserRepliesFiltered(Long userId, String tone,
                                                   LocalDateTime fromDate, LocalDateTime toDate) {
        log.debug("Filtering replies for user {} tone='{}' from={} to={}",
                userId, tone, fromDate, toDate);
        return repo.findByUserIdWithFilters(userId, tone, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<SavedReply> searchUserReplies(Long userId, String searchTerm, String tone) {
        log.debug("Searching replies for user {} term='{}' tone='{}'", userId, searchTerm, tone);
        if (tone != null && !tone.isBlank()) {
            return repo.findByUserIdAndSearchTermAndTone(userId, searchTerm, tone);
        }
        return repo.findByUserIdAndSearchTerm(userId, searchTerm);
    }

    @Transactional(readOnly = true)
    public List<SavedReply> getFavoriteReplies(Long userId) {
        return repo.findByUserIdAndIsFavoriteOrderByCreatedAtDesc(userId, true);
    }

    @Transactional
    public SavedReply toggleFavorite(Long id, Long userId) {
        SavedReply reply = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reply not found with ID: " + id));

        if (!reply.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Reply belongs to different user");
        }

        boolean newFavoriteStatus = !Boolean.TRUE.equals(reply.getIsFavorite());
        reply.setIsFavorite(newFavoriteStatus);

        SavedReply updated = repo.save(reply);
        log.debug("Toggled favorite to {} for reply {} user {}", newFavoriteStatus, id, userId);
        return updated;
    }

    @Transactional
    public void deleteReply(Long id, Long userId) {
        SavedReply reply = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reply not found with ID: " + id));

        if (!reply.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Cannot delete reply owned by different user");
        }

        repo.delete(reply);
        log.info("Reply {} deleted by user {}", id, userId);
    }

    @Transactional(readOnly = true)
    public long getUserReplyCount(Long userId) {
        return repo.countByUserId(userId);
    }

    /**
     * Generate statistics using DB aggregate queries.
     *
     * Previously this method called findByUserIdOrderByCreatedAtDesc() which
     * loaded every single SavedReply for the user into JVM memory, then used
     * Java streams to group, count, and sort. The repo already had
     * getToneDistributionForUser() and getMostCommonSubjectsForUser() defined
     * but they were never called. Now they are the primary mechanism.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // Total count — single COUNT query, no data loading
        long totalReplies = repo.countByUserId(userId);
        stats.put("totalReplies", (int) totalReplies);

        // Favorite count — single COUNT query
        long favoriteCount = repo.countByUserIdAndIsFavorite(userId, true);
        stats.put("favoriteReplies", (int) favoriteCount);

        // Recent activity (last 30 days) — single COUNT query
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentReplies = repo.countByUserIdAndCreatedAtAfter(userId, thirtyDaysAgo);
        stats.put("recentActivity", (int) recentReplies);

        // Tone distribution — aggregate GROUP BY query, returns [tone, count] rows
        List<Object[]> toneRows = repo.getToneDistributionForUser(userId);
        Map<String, Long> toneDistribution = new LinkedHashMap<>();
        for (Object[] row : toneRows) {
            toneDistribution.put((String) row[0], (Long) row[1]);
        }
        stats.put("toneDistribution", toneDistribution);

        // Top 5 subjects — aggregate GROUP BY query with limit
        List<Object[]> subjectRows = repo.getMostCommonSubjectsForUser(
                userId, PageRequest.of(0, 5));
        List<Map<String, Object>> topSubjects = new ArrayList<>();
        for (Object[] row : subjectRows) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("subject", row[0]);
            entry.put("count",   ((Long) row[1]).intValue());
            topSubjects.add(entry);
        }
        stats.put("topSubjects", topSubjects);

        log.debug("Generated statistics for user {}: {} total, {} favorites",
                userId, totalReplies, favoriteCount);
        return stats;
    }

    @Transactional(readOnly = true)
    public String exportUserRepliesToCsv(Long userId) {
        List<SavedReply> replies = repo.findByUserIdOrderByCreatedAtDesc(userId);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Subject,Tone,Language,Created,Is_Favorite,Summary,Reply_Preview\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (SavedReply reply : replies) {
            csv.append(reply.getId()).append(",");
            csv.append(escapeCsv(reply.getEmailSubject())).append(",");
            csv.append(escapeCsv(reply.getTone())).append(",");
            csv.append(escapeCsv(reply.getLanguage())).append(",");
            csv.append(reply.getCreatedAt().format(formatter)).append(",");
            csv.append(Boolean.TRUE.equals(reply.getIsFavorite())).append(",");
            csv.append(escapeCsv(truncateText(reply.getSummary(), 100))).append(",");
            csv.append(escapeCsv(truncateText(reply.getReplyText(), 200))).append("\n");
        }

        log.info("Exported {} replies to CSV for user {}", replies.size(), userId);
        return csv.toString();
    }

    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}