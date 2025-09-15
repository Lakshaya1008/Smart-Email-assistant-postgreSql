package com.email.writer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO used to save a generated reply to DB.
 */
@Data
public class SaveReplyRequest {

    @NotBlank(message = "Email subject is required")
    private String emailSubject;

    @NotBlank(message = "Email content is required")
    private String emailContent;

    private String tone; // Optional

    @NotBlank(message = "Reply text is required")
    private String replyText;

    private String summary; // Optional
}
