package tn.kamel.backend.dto;

import jakarta.validation.constraints.*;

public record ContactRequest(
        @NotBlank @Size(max = 80) String nom,
        @NotBlank @Size(min = 8, max = 20) @Pattern(regexp = "^[0-9 +().-]+$") String tel,
        @NotBlank @Size(max = 2000) String message
) {}
