package com.email.writer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO used to save a generated reply to the database.
 *
 * Changes:
 * 1. language field added — was missing, so language was silently dropped
 *    on every save. Now passed through and stored in SavedReply entity.
 *
 * 2. @Size(max = 500) added to emailSubject — the SavedReply entity has
 *    @Column(length = 500) but the DTO had no corresponding validation.
 *    Without this, a subject > 500 chars bypasses @NotBlank, reaches Hibernate,
 *    and causes a DataException with the raw SQL error sent to the client.
 */
@Data
public class SaveReplyRequest {

    @NotBlank(message = "Email subject is required")
    @Size(max = 500, message = "Email subject must not exceed 500 characters")
    private String emailSubject;

    @NotBlank(message = "Email content is required")
    private String emailContent;

    private String tone;     // Optional

    private String language; // Optional — e.g. "en", "hi", "fr"

    @NotBlank(message = "Reply text is required")
    private String replyText;

    private String summary;  // Optional
}