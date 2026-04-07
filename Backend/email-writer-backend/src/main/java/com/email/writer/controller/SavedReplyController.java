package com.email.writer.controller;

import com.email.writer.dto.SaveReplyRequest;
import com.email.writer.entity.SavedReply;
import com.email.writer.entity.User;
import com.email.writer.service.AuthService;
import com.email.writer.service.SavedReplyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Saved reply CRUD, search, favorites, stats, export — versioned at /api/v1/replies.
 */
@RestController
@RequestMapping("/api/v1/replies")
@RequiredArgsConstructor
@Slf4j
public class SavedReplyController {

    private final SavedReplyService savedReplyService;
    private final AuthService       authService;

    @PostMapping("/save")
    public ResponseEntity<?> saveReply(
            @Valid @RequestBody SaveReplyRequest request,
            Authentication authentication) {
        User currentUser = authService.getCurrentUser(authentication);
        SavedReply saved = savedReplyService.saveReply(request, currentUser);
        log.info("Reply saved for user {} with ID {}", currentUser.getUsername(), saved.getId());
        return ResponseEntity.ok(Map.of(
                "message",   "Reply saved successfully",
                "id",        saved.getId(),
                "createdAt", saved.getCreatedAt()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserReplies(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tone,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        User user = authService.getCurrentUser(authentication);

        if (tone != null || fromDate != null || toDate != null) {
            List<SavedReply> filtered = savedReplyService.getUserRepliesFiltered(
                    user.getId(), tone, fromDate, toDate);
            return ResponseEntity.ok(Map.of(
                    "content", filtered, "total", filtered.size(), "filtered", true));
        }

        Page<SavedReply> p = savedReplyService.getUserReplies(user.getId(), page, size);
        return ResponseEntity.ok(Map.of(
                "content",       p.getContent(),
                "totalPages",    p.getTotalPages(),
                "totalElements", p.getTotalElements(),
                "currentPage",   page,
                "size",          size
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchReplies(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "tone", required = false) String tone,
            Authentication authentication) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "missing_query_param",
                    "message", "Query parameter 'q' is required and cannot be empty."
            ));
        }
        User currentUser = authService.getCurrentUser(authentication);
        List<SavedReply> replies = savedReplyService.searchUserReplies(currentUser.getId(), q, tone);
        return ResponseEntity.ok(Map.of(
                "results", replies, "query", q,
                "tone", tone != null ? tone : "", "total", replies.size()
        ));
    }

    @GetMapping("/favorites")
    public ResponseEntity<?> getFavoriteReplies(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        List<SavedReply> favorites = savedReplyService.getFavoriteReplies(user.getId());
        return ResponseEntity.ok(Map.of("favorites", favorites, "total", favorites.size()));
    }

    @PutMapping("/{id}/favorite")
    public ResponseEntity<?> toggleFavorite(@PathVariable Long id, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        SavedReply reply = savedReplyService.toggleFavorite(id, user.getId());
        String action = reply.getIsFavorite() ? "Added to favorites" : "Removed from favorites";
        return ResponseEntity.ok(Map.of("message", action, "isFavorite", reply.getIsFavorite()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReply(@PathVariable Long id, Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        savedReplyService.deleteReply(id, user.getId());
        log.info("Reply {} deleted by user {}", id, user.getUsername());
        return ResponseEntity.ok(Map.of("message", "Reply deleted successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        Map<String, Object> stats = savedReplyService.getUserStatistics(user.getId());
        stats.put("username", user.getUsername());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportReplies(Authentication authentication) {
        User user = authService.getCurrentUser(authentication);
        String csvData = savedReplyService.exportUserRepliesToCsv(user.getId());
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition",
                        "attachment; filename=saved_replies_" + user.getUsername() + ".csv")
                .body(csvData);
    }
}