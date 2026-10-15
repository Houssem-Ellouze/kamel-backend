package tn.kamel.backend.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.kamel.backend.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {}
