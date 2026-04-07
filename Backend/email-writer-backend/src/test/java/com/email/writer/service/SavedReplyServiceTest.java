package com.email.writer.service;

import com.email.writer.dto.SaveReplyRequest;
import com.email.writer.entity.SavedReply;
import com.email.writer.entity.User;
import com.email.writer.repository.SavedReplyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedReplyService unit tests")
class SavedReplyServiceTest {

    @Mock private SavedReplyRepository repo;

    @InjectMocks
    private SavedReplyService service;

    private User    testUser;
    private SavedReply testReply;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setIsActive(true);

        testReply = new SavedReply();
        testReply.setId(10L);
        testReply.setUser(testUser);
        testReply.setEmailSubject("Meeting Tomorrow");
        testReply.setEmailContent("Can we meet tomorrow?");
        testReply.setTone("professional");
        testReply.setLanguage("en");
        testReply.setReplyText("Yes, that works for me.");
        testReply.setSummary("Request to meet tomorrow.");
        testReply.setIsFavorite(false);
        testReply.setCreatedAt(LocalDateTime.now());
    }

    // ── saveReply ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveReply — persists language field from request")
    void saveReply_persistsLanguage() {
        SaveReplyRequest req = new SaveReplyRequest();
        req.setEmailSubject("Test Subject");
        req.setEmailContent("Test Content");
        req.setTone("professional");
        req.setLanguage("hi");
        req.setReplyText("Test reply");

        when(repo.save(any(SavedReply.class))).thenAnswer(inv -> {
            SavedReply sr = inv.getArgument(0);
            sr.setId(1L);
            assertThat(sr.getLanguage()).isEqualTo("hi");
            return sr;
        });

        SavedReply saved = service.saveReply(req, testUser);

        assertThat(saved.getId()).isNotNull();
        verify(repo).save(argThat(sr -> "hi".equals(sr.getLanguage())));
    }

    // ── getUserStatistics ─────────────────────────────────────────────────

    @Test
    @DisplayName("getUserStatistics — uses DB aggregate queries not in-heap loading")
    void getUserStatistics_usesAggregateQueries() {
        Long userId = 1L;

        // These are the 5 DB calls that replace the old full-row load
        when(repo.countByUserId(userId)).thenReturn(42L);
        when(repo.countByUserIdAndIsFavorite(userId, true)).thenReturn(7L);
        when(repo.countByUserIdAndCreatedAtAfter(eq(userId), any(LocalDateTime.class))).thenReturn(10L);
        when(repo.getToneDistributionForUser(userId))
                .thenReturn(List.of(
                        new Object[]{"professional", 30L},
                        new Object[]{"casual",       12L}
                ));
        when(repo.getMostCommonSubjectsForUser(eq(userId), any(PageRequest.class)))
                .thenReturn(List.of(
                        new Object[]{"Meeting Tomorrow",   5L},
                        new Object[]{"Project Update",     3L}
                ));

        Map<String, Object> stats = service.getUserStatistics(userId);

        assertThat(stats.get("totalReplies")).isEqualTo(42);
        assertThat(stats.get("favoriteReplies")).isEqualTo(7);
        assertThat(stats.get("recentActivity")).isEqualTo(10);

        @SuppressWarnings("unchecked")
        Map<String, Long> toneMap = (Map<String, Long>) stats.get("toneDistribution");
        assertThat(toneMap).containsEntry("professional", 30L)
                .containsEntry("casual",       12L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subjects = (List<Map<String, Object>>) stats.get("topSubjects");
        assertThat(subjects).hasSize(2);
        assertThat(subjects.get(0).get("subject")).isEqualTo("Meeting Tomorrow");

        // Critical assertion: findByUserIdOrderByCreatedAtDesc must NOT be called
        // (that was the old in-heap approach we replaced)
        verify(repo, never()).findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── toggleFavorite ────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleFavorite — false becomes true")
    void toggleFavorite_false_becomesTrue() {
        testReply.setIsFavorite(false);
        when(repo.findById(10L)).thenReturn(Optional.of(testReply));
        when(repo.save(testReply)).thenReturn(testReply);

        SavedReply result = service.toggleFavorite(10L, 1L);

        assertThat(result.getIsFavorite()).isTrue();
        verify(repo).save(testReply);
    }

    @Test
    @DisplayName("toggleFavorite — true becomes false")
    void toggleFavorite_true_becomesFalse() {
        testReply.setIsFavorite(true);
        when(repo.findById(10L)).thenReturn(Optional.of(testReply));
        when(repo.save(testReply)).thenReturn(testReply);

        SavedReply result = service.toggleFavorite(10L, 1L);

        assertThat(result.getIsFavorite()).isFalse();
    }

    @Test
    @DisplayName("toggleFavorite — reply not found throws RuntimeException")
    void toggleFavorite_notFound_throwsException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleFavorite(99L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reply not found with ID: 99");

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("toggleFavorite — wrong owner throws access denied")
    void toggleFavorite_wrongOwner_throwsAccessDenied() {
        when(repo.findById(10L)).thenReturn(Optional.of(testReply));

        // testReply owner is userId=1, trying to toggle with userId=99
        assertThatThrownBy(() -> service.toggleFavorite(10L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");

        verify(repo, never()).save(any());
    }
}