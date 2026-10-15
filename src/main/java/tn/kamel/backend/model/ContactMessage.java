package tn.kamel.backend.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("messages")
public record ContactMessage(
        @Id String id,
        String nom,
        String tel,
        String message,
        Instant createdAt
) {}
