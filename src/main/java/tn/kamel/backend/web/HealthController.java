package tn.kamel.backend.web;

import java.time.Instant;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final MongoTemplate mongo;

    public HealthController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @GetMapping("/health/db")
    public Map<String, Object> db() {
        try {
            mongo.executeCommand("{ ping: 1 }");
            mongo.getCollection("healthcheck")
                    .insertOne(new Document("t", Instant.now().toString()));
            return Map.of("ok", true, "db", mongo.getDb().getName());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}