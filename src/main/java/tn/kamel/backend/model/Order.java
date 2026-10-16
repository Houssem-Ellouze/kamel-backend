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
        String note,         // Message / note du client
        List<Line> lignes,   // Liste des produits commandés
        String statut,       // Ex: "NOUVELLE"
        Instant createdAt
) {
    public record Line(
            String k,       // Clé du produit (ex: "mtb", "city")
            String nom,     // Nom du produit
            double prix,    // Prix unitaire
            int qty         // Quantité
    ) {}
}