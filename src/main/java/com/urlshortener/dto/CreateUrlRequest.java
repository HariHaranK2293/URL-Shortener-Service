package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUrlRequest {

    @NotBlank(message = "Original URL is required")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$", message = "Custom alias must be 3-20 characters, alphanumeric, underscore, or hyphen")
    private String customAlias;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private LocalDateTime expiresAt;

    @NotBlank(message = "API key is required")
    private String apiKey;
}
