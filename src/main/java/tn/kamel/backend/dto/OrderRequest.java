package tn.kamel.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(
        @NotBlank @Size(max = 60) String prenom,
        @NotBlank @Size(max = 60) String nom,
        @NotBlank @Size(min = 8, max = 20) @Pattern(regexp = "^[0-9 +().-]+$") String tel,
        @Size(max = 40) String delai,
        @Size(max = 60) String paiement,
        @Size(max = 1000) String note,
        @NotEmpty @Size(max = 30) List<@Valid LineRequest> lignes
) {
    public record LineRequest(
            @NotBlank String k,
            @Min(1) @Max(99) int qty,
            @Size(max = 120) String n,      // nom du produit envoyé par le site (secours)
            @Size(max = 60) String p        // prix affiché envoyé par le site (secours)
    ) {}
}