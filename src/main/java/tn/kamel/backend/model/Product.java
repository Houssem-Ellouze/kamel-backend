package tn.kamel.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Mêmes noms de champs que le tableau PRODUCTS du front (k = clé unique, ex. "mtb"). */
@Document("products")
public record Product(
        @Id String k,
        String n,        // nom
        String c,        // catégorie : VÉLOS / MOTOS / TROTTINETTES
        String d,        // description
        String p,        // prix affiché
        String op,       // ancien prix (promo)
        String promo,    // badge promo, ex. "-20%"
        boolean isNew,   // badge NOUVEAU
        String a,        // disponibilité
        String img,
        String il
) {}
