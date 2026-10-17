package tn.kamel.backend.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import tn.kamel.backend.dto.ContactRequest;
import tn.kamel.backend.dto.OrderRequest;
import tn.kamel.backend.model.*;
import tn.kamel.backend.repo.ContactMessageRepository;
import tn.kamel.backend.repo.OrderRepository;
import tn.kamel.backend.repo.ProductRepository;

/*
 * CORS : géré uniquement par CorsConfig (origines de app.cors.allowed-origins).
 * L'ancien @CrossOrigin(origins = "*") a été retiré : il autorisait n'importe quel site.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ContactMessageRepository contactMessageRepository;

    private final String UPLOAD_DIR = "uploads/";

    /** URL publique du backend (application.properties : app.public-base-url) */
    @Value("${app.public-base-url:https://kamel-backend-2.onrender.com}")
    private String publicBaseUrl;

    public ApiController(ProductRepository productRepository,
                         OrderRepository orderRepository,
                         ContactMessageRepository contactMessageRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.contactMessageRepository = contactMessageRepository;
    }

    // ==========================================
    // 🚲 GESTION DES PRODUITS (CRUD + UPLOAD)
    // ==========================================

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des produits : {}", e.getMessage(), e);
            return List.of();
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(
            @RequestParam("n") String name,
            @RequestParam("c") ProductCategory category,
            @RequestParam(value = "d", required = false) String description,
            @RequestParam(value = "p", required = false) String price,
            @RequestParam(value = "promo", required = false, defaultValue = "false") boolean promo,
            @RequestParam(value = "promoPercent", required = false) Integer promoPercent,
            @RequestParam(value = "new", required = false, defaultValue = "false") boolean isNew,
            @RequestParam(value = "a", required = false, defaultValue = "DISPONIBLE") ProductAvailability availability,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom (n) est obligatoire");
        }

        Product product = new Product();
        product.setK(UUID.randomUUID().toString());
        product.setN(name.trim());
        product.setD(cleanDescription(description));
        product.setC(category.getLabel());
        product.setNew(isNew);
        product.setA(availability.getLabel());
        applyPricing(product, blankToNull(price), promo, promoPercent);

        try {
            if (file != null && !file.isEmpty()) {
                product.setImg(saveFile(file));
            } else {
                log.warn("Création produit '{}' SANS photo (aucun fichier reçu dans le champ 'file')", product.getN());
            }
            log.info("Création produit k={} n={} c={} p={} op={} promo={} new={} description={}",
                    product.getK(), product.getN(), product.getC(), product.getP(),
                    product.getOp(), product.getPromo(), product.isNew(),
                    product.getD() != null ? "oui" : "non");
            return productRepository.save(product);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création du produit : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur : " + e.getMessage());
        }
    }

    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Product updateProduct(
            @PathVariable String id,
            @RequestParam(value = "n", required = false) String name,
            @RequestParam(value = "c", required = false) ProductCategory category,
            @RequestParam(value = "d", required = false) String description,
            @RequestParam(value = "p", required = false) String price,
            @RequestParam(value = "promo", required = false) Boolean promo,
            @RequestParam(value = "promoPercent", required = false) Integer promoPercent,
            @RequestParam(value = "new", required = false) Boolean isNew,
            @RequestParam(value = "a", required = false) ProductAvailability availability,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable : " + id));

        if (name != null && !name.isBlank()) product.setN(name.trim());
        if (category != null) product.setC(category.getLabel());
        if (isNew != null) product.setNew(isNew);
        if (availability != null) product.setA(availability.getLabel());

        if (description != null) {
            product.setD(cleanDescription(description));
        }

        if (promo != null || price != null || promoPercent != null) {
            boolean currentlyPromo = product.getPromo() != null;
            boolean wantPromo = promo != null ? promo : currentlyPromo;

            String normalPrice = blankToNull(price);
            if (normalPrice == null) {
                normalPrice = (currentlyPromo && product.getOp() != null) ? product.getOp() : product.getP();
            }
            Integer percent = promoPercent != null ? promoPercent : percentOf(product.getPromo());
            applyPricing(product, normalPrice, wantPromo, percent);
        }

        try {
            if (file != null && !file.isEmpty()) {
                product.setImg(saveFile(file));
            }
            Product updated = productRepository.save(product);
            log.info("Produit mis à jour avec succès, ID : {}", id);
            return updated;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du produit : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur : " + e.getMessage());
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            log.info("Produit supprimé, ID : {}", id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ==========================================
    // 📦 GESTION DES COMMANDES
    // ==========================================

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createOrder(@RequestBody OrderRequest req) {
        log.info("Requête de commande reçue : {}", req);

        if (req == null || req.lignes() == null || req.lignes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le panier est vide ou la requête est invalide.");
        }

        try {
            List<Order.Line> lines = new ArrayList<>();
            for (var l : req.lignes()) {
                if (l == null || l.k() == null) {
                    continue;
                }

                Product p = productRepository.findByK(l.k()).orElse(null);
                if (p == null) {
                    p = productRepository.findById(l.k()).orElse(null);
                }

                String nomProduit;
                double prix;

                if (p != null) {
                    nomProduit = p.getN();
                    prix = orZero(parseAmount(p.getP()));
                } else {
                    nomProduit = (l.n() != null && !l.n().isBlank()) ? l.n().trim() : "Article " + l.k();
                    prix = orZero(parseAmount(l.p()));
                }

                int qty = l.qty() > 0 ? l.qty() : 1;
                lines.add(new Order.Line(l.k(), nomProduit, prix, qty));
            }

            if (lines.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun article valide dans la commande.");
            }

            Order orderToSave = new Order(
                    null,
                    req.prenom() != null ? req.prenom().trim() : "",
                    req.nom() != null ? req.nom().trim() : "",
                    req.tel() != null ? req.tel().trim() : "",
                    req.delai() != null ? req.delai() : "Dès que possible",
                    req.paiement() != null ? req.paiement() : "Espèces",
                    req.note() != null ? req.note() : "",
                    lines,
                    "NOUVELLE",
                    Instant.now()
            );

            Order saved = orderRepository.save(orderToSave);
            String orderId = saved != null && saved.id() != null ? String.valueOf(saved.id()) : "";

            return Map.of("id", orderId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur détaillée lors de l'enregistrement de la commande", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur : " + e.getMessage(), e);
        }
    }

    // ==========================================
    // ✉️ GESTION DES CONTACTS
    // ==========================================

    /** @Valid : applique les règles de ContactRequest (nom, tel 8-20 caractères, message). Sinon -> 400. */
    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> contact(@Valid @RequestBody ContactRequest req) {
        try {
            ContactMessage msg = new ContactMessage(
                    null,
                    req.nom().trim(),
                    req.tel().trim(),
                    req.message().trim(),
                    Instant.now()
            );

            ContactMessage saved = contactMessageRepository.save(msg);
            String contactId = saved != null && saved.id() != null ? String.valueOf(saved.id()) : "";

            return Map.of("id", contactId);

        } catch (Exception e) {
            log.error("Erreur enregistrement contact : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur sauvegarde contact : " + e.getMessage());
        }
    }

    // ==========================================
    // 🛠️ ESPACE ADMIN (LECTURE)
    // ==========================================

    @GetMapping("/admin/orders")
    public List<Order> adminOrders() {
        try {
            return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } catch (Exception e) {
            log.error("Erreur admin orders : {}", e.getMessage(), e);
            return List.of();
        }
    }

    @GetMapping("/admin/messages")
    public List<ContactMessage> adminMessages() {
        try {
            return contactMessageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } catch (Exception e) {
            log.error("Erreur admin messages : {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ==========================================
    // 🔧 MÉTHODES UTILITAIRES
    // ==========================================

    private String cleanDescription(String d) {
        if (d == null) return null;
        String t = d.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La description est trop longue (maximum " + MAX_DESCRIPTION_LENGTH + " caractères)");
        }
        return t;
    }

    private void applyPricing(Product product, String normalPrice, boolean promo, Integer percent) {
        if (promo) {
            if (percent == null || percent < 1 || percent > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Promotion activée : promoPercent doit être compris entre 1 et 99");
            }
            product.setPromo("-" + percent + "%");

            Double original = parseAmount(normalPrice);
            if (original != null) {
                product.setOp(formatDt(original));
                product.setP(formatDt(original * (100 - percent) / 100.0));
            } else {
                product.setOp(null);
                product.setP(normalPrice);
            }
        } else {
            product.setPromo(null);
            product.setOp(null);
            product.setP(normalPrice);
        }
    }

    private Integer percentOf(String promoLabel) {
        if (promoLabel == null) return null;
        String digits = promoLabel.replaceAll("[^0-9]", "");
        try { return digits.isEmpty() ? null : Integer.valueOf(digits); }
        catch (NumberFormatException e) { return null; }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank() || s.equalsIgnoreCase("null") || s.equals("string")) ? null : s.trim();
    }

    private Double parseAmount(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9.,]", "").replace(',', '.');
        try { return digits.isEmpty() ? null : Double.parseDouble(digits); }
        catch (NumberFormatException e) { return null; }
    }

    private double orZero(Double v) {
        return v != null ? v : 0.0;
    }

    private String formatDt(double v) {
        double r = Math.round(v * 100) / 100.0;
        return (r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r)) + " DT";
    }

    private String saveFile(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String lowerName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean imageByType = contentType != null && contentType.startsWith("image/");
        boolean imageByExt = lowerName.matches(".*\\.(png|jpe?g|webp|gif|avif|jfif|bmp)$");
        if (!imageByType && !imageByExt) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le fichier doit être une image (png, jpg, webp...) — type reçu : " + contentType);
        }

        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        original = original.replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");

        String fileName = System.currentTimeMillis() + "_" + safeName;
        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName).normalize();
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return publicBaseUrl + "/uploads/" + fileName;
    }
}