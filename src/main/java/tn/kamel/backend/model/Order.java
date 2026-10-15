package tn.kamel.backend.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
public record Order(
        @Id String id,
        String prenom,
        String nom,
        String tel,
        String delai,
        String paiement,
        String note,
        List<Line> lignes,
        String statut,      // NOUVELLE / CONFIRMEE / ANNULEE
        Instant createdAt
) {
    public record Line(String k, String nom, String prix, int qty) {}
}
