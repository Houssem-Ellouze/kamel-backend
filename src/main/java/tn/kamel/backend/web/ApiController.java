package tn.kamel.backend.web;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.kamel.backend.dto.ContactRequest;
import tn.kamel.backend.dto.OrderRequest;
import tn.kamel.backend.model.ContactMessage;
import tn.kamel.backend.model.Order;
import tn.kamel.backend.model.Product;
import tn.kamel.backend.repo.ContactMessageRepository;
import tn.kamel.backend.repo.OrderRepository;
import tn.kamel.backend.repo.ProductRepository;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ProductRepository products;
    private final OrderRepository orders;
    private final ContactMessageRepository messages;
    private final String adminKey;

    public ApiController(ProductRepository products, OrderRepository orders,
                         ContactMessageRepository messages,
                         @Value("${app.admin-key}") String adminKey) {
        this.products = products;
        this.orders = orders;
        this.messages = messages;
        this.adminKey = adminKey;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** Catalogue (même format que PRODUCTS dans le front). */
    @GetMapping("/products")
    public List<Product> products() {
        return products.findAll();
    }

    /** Enregistre une commande/devis. Les noms et prix sont relus en base, jamais pris du client. */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createOrder(@Valid @RequestBody OrderRequest req) {
        List<Order.Line> lines = req.lignes().stream().map(l -> {
            Product p = products.findById(l.k()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produit inconnu : " + l.k()));
            return new Order.Line(p.k(), p.n(), p.p(), l.qty());
        }).toList();

        Order saved = orders.save(new Order(null, req.prenom().trim(), req.nom().trim(), req.tel().trim(),
                req.delai(), req.paiement(), req.note(), lines, "NOUVELLE", Instant.now()));
        return Map.of("id", saved.id());
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> contact(@Valid @RequestBody ContactRequest req) {
        ContactMessage saved = messages.save(new ContactMessage(null, req.nom().trim(),
                req.tel().trim(), req.message().trim(), Instant.now()));
        return Map.of("id", saved.id());
    }

    // ----- Lecture côté atelier (protégée par X-Admin-Key) -----
    @GetMapping("/admin/orders")
    public List<Order> adminOrders(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        checkAdmin(key);
        return orders.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/admin/messages")
    public List<ContactMessage> adminMessages(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        checkAdmin(key);
        return messages.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void checkAdmin(String key) {
        if (adminKey.isBlank() || !adminKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
