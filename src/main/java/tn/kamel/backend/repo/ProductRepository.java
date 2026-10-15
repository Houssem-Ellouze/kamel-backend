package tn.kamel.backend.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.kamel.backend.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {}
