package tn.kamel.backend.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.kamel.backend.model.Product;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findByK(String k);
}
