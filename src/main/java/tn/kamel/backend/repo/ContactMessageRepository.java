package tn.kamel.backend.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.kamel.backend.model.ContactMessage;

public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {}
