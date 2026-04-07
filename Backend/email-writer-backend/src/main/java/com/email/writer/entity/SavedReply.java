package com.email.writer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SavedReply — stores generated replies a user chooses to save.
 *
 * Changes:
 * 1. @Data replaced with @Getter @Setter + explicit @ToString/@EqualsAndHashCode.
 *    @Data generated toString() using ALL fields including the lazy User relation,
 *    which triggered LazyInitializationException outside transactions and could
 *    cause infinite recursion if User had a back-reference.
 *
 * 2. @Index added on user_id. Every repository query filters by user.id.
 *    Without this, each query does a full table scan of saved_replies.
 *    With ddl-auto=update, Hibernate will create the index on next startup.
 *
 * 3. language field added. EmailRequest has language, Gemini uses it, but
 *    previously it was silently dropped and never stored. Now persisted.
 */
@Entity
@Table(name = "saved_replies", indexes = {
        @Index(name = "idx_saved_reply_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")       // exclude lazy relation to prevent LazyInitializationException
@EqualsAndHashCode(exclude = "user", onlyExplicitlyIncluded = false)
public class SavedReply {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "saved_reply_seq")
    @SequenceGenerator(name = "saved_reply_seq", sequenceName = "saved_reply_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "email_subject", length = 500)
    private String emailSubject;

    @Column(name = "email_content", columnDefinition = "TEXT")
    private String emailContent;

    @Column(name = "tone")
    private String tone;

    // Added: stores the language the reply was generated in (e.g. "en", "hi", "fr")
    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_favorite")
    private Boolean isFavorite = false;

    // Updated constructor to include language
    public SavedReply(User user, String emailSubject, String emailContent,
                      String tone, String language, String replyText, String summary) {
        this.user         = user;
        this.emailSubject = emailSubject;
        this.emailContent = emailContent;
        this.tone         = tone;
        this.language     = language;
        this.replyText    = replyText;
        this.summary      = summary;
        this.isFavorite   = false;
    }
}