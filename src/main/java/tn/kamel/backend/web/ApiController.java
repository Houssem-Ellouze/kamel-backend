package tn.kamel.backend.web;

import java.io.File;
import java.io.IOException;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

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
@CrossOrigin(origins = "*")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final ObjectMapper objectMapper;

    private final String UPLOAD_DIR = "uploads/";

    public ApiController(ProductRepository productRepository,
                         OrderRepository orderRepository,
                         ContactMessageRepository contactMessageRepository,
                         ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // 🚲 GESTION DES PRODUITS (CRUD + UPLOAD)
    // ==========================================

    @GetMapping("/products")
    public List<Product> getAllProducts() {
        try {
            return productRepository.findAll();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des produits : {}", e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * 2. AJOUTER un produit avec une image optionnelle
     */
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(
            @RequestPart("product") String productInput,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Product product;
        try {
            product = objectMapper.readValue(productInput, Product.class);
        } catch (Exception e) {
            product = new Product();
            product.setN(productInput);
        }

        // Si l'ID est "string" ou vide, on le met à null pour que MongoDB en génère un vrai
        if (product.getId() != null && (product.getId().isEmpty() || product.getId().equals("string"))) {
            product.setId(null);
        }

        // CORRECTION ROBUSTE : S'assurer que le champ 'k' n'est jamais null pour éviter l'index unique MongoDB (DuplicateKeyException)
        if (product.getK() == null || product.getK().trim().isEmpty() || product.getK().equalsIgnoreCase("null")) {
            product.setK(UUID.randomUUID().toString());
        }

        try {
            if (file != null && !file.isEmpty()) {
                String imageUrl = saveFile(file);
                product.setImg(imageUrl);
            }

            log.info("Création d'un nouveau produit avec la clé : {} - Nom : {}", product.getK(), product.getN());
            return productRepository.save(product);
        } catch (Exception e) {
            log.error("ERREUR CRITIQUE LORS DE LA CRÉATION DU PRODUIT : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur : " + e.getMessage());
        }
    }

    /**
     * 3. MODIFIER un produit existant
     */
    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> updateProduct(
            @PathVariable String id,
            @RequestPart("product") String productInput,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isPresent()) {
            try {
                Product productDetails;
                try {
                    productDetails = objectMapper.readValue(productInput, Product.class);
                } catch (Exception e) {
                    productDetails = new Product();
                    productDetails.setN(productInput);
                }

                Product product = optionalProduct.get();
                product.setId(id); // On s'assure de garder l'ID existant

                if (productDetails.getN() != null) product.setN(productDetails.getN());
                if (productDetails.getC() != null) product.setC(productDetails.getC());
                if (productDetails.getD() != null) product.setD(productDetails.getD());
                if (productDetails.getP() != null) product.setP(productDetails.getP());
                if (productDetails.getOp() != null) product.setOp(productDetails.getOp());
                if (productDetails.getPromo() != null) product.setPromo(productDetails.getPromo());
                product.setNew(productDetails.isNew());
                if (productDetails.getA() != null) product.setA(productDetails.getA());

                if (productDetails.getK() != null && !productDetails.getK().trim().isEmpty()) {
                    product.setK(productDetails.getK());
                }

                if (file != null && !file.isEmpty()) {
                    String imageUrl = saveFile(file);
                    product.setImg(imageUrl);
                } else if (productDetails.getImg() != null) {
                    product.setImg(productDetails.getImg());
                }

                Product updatedProduct = productRepository.save(product);
                log.info("Produit mis à jour avec succès, ID : {}", id);
                return ResponseEntity.ok(updatedProduct);

            } catch (Exception e) {
                log.error("Erreur lors de la mise à jour du produit : {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
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

    /** Sauvegarde physique du fichier image */
    private String saveFile(MultipartFile file) throws IOException {
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "http://localhost:9000/uploads/" + fileName;
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

                Product p = productRepository.findById(l.k()).orElse(null);

                String nomProduit;
                double numericPrice = 0.0;

                if (p != null) {
                    nomProduit = p.getN();
                    try {
                        String cleanPrice = p.getP() != null ? p.getP().replaceAll("[^0-9.]", "") : "0";
                        if (!cleanPrice.isEmpty()) {
                            numericPrice = Double.parseDouble(cleanPrice);
                        }
                    } catch (Exception ignored) {}
                } else {
                    log.warn("Produit avec la clé '{}' introuvable dans le catalogue.", l.k());
                    nomProduit = "Article " + l.k();
                    numericPrice = 0.0;
                }

                int qty = l.qty() > 0 ? l.qty() : 1;
                lines.add(new Order.Line(l.k(), nomProduit, numericPrice, qty));
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

            log.info("Commande enregistrée avec succès, ID : {}", orderId);
            return Map.of("id", orderId);

        } catch (Exception e) {
            log.error("Erreur détaillée lors de l'enregistrement de la commande", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur : " + e.getMessage(), e);
        }
    }

    // ==========================================
    // ✉️ GESTION DES CONTACTS
    // ==========================================

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> contact(@RequestBody ContactRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La requête de contact est vide");
        }

        try {
            ContactMessage msg = new ContactMessage(
                    null,
                    req.nom() != null ? req.nom().trim() : "",
                    req.tel() != null ? req.tel().trim() : "",
                    req.message() != null ? req.message().trim() : "",
                    Instant.now()
            );

            ContactMessage saved = contactMessageRepository.save(msg);
            String contactId = saved != null && saved.id() != null ? String.valueOf(saved.id()) : "";

            return Map.of("id", contactId);

        } catch (Exception e) {
            log.error("Erreur enregistrement contact : {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur sauvegarde contact : " + e.getMessage());
        }
    }

    // ==========================================
    // 🛠️ ESPACE ADMIN (LECTURE)
    // ==========================================

    @GetMapping("/admin/orders")
    public List<Order> adminOrders() {
        try {
            if (orderRepository == null) return List.of();
            return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } catch (Exception e) {
            log.error("Erreur admin orders : {}", e.getMessage());
            return List.of();
        }
    }

    @GetMapping("/admin/messages")
    public List<ContactMessage> adminMessages() {
        try {
            if (contactMessageRepository == null) return List.of();
            return contactMessageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } catch (Exception e) {
            log.error("Erreur admin messages : {}", e.getMessage());
            return List.of();
        }
    }
}