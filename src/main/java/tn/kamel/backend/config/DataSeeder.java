package tn.kamel.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tn.kamel.backend.model.Product;
import tn.kamel.backend.repo.ProductRepository;

/** Au premier démarrage, remplit la collection "products" avec products.json. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProductRepository repo;
    private final ObjectMapper mapper;

    public DataSeeder(ProductRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() > 0) return;
        try (InputStream in = new ClassPathResource("products.json").getInputStream()) {
            Product[] items = mapper.readValue(in, Product[].class);
            repo.saveAll(Arrays.asList(items));
            log.info("{} produits importés dans MongoDB", items.length);
        }
    }
}
