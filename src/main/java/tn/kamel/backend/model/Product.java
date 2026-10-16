package tn.kamel.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String n;       // Nom du produit
    private String c;       // Catégorie
    private String d;       // Description
    private String p;       // Prix
    private String op;      // Ancien prix (promotion)
    private String promo;   // Badge promo (ex: "-20%")
    private boolean isNew;  // Vrai si c'est une nouveauté
    private String a;       // Disponibilité ("Disponible" ou "Sur demande")
    private String img;     // URL ou chemin de l'image
    private String k;       // Clé unique / Slug

    // Constructeur vide (nécessaire pour Spring / MongoDB)
    public Product() {}

    // Constructeur avec paramètres
    public Product(String id, String n, String c, String d, String p, String op, String promo, boolean isNew, String a, String img, String k) {
        this.id = id;
        this.n = n;
        this.c = c;
        this.d = d;
        this.p = p;
        this.op = op;
        this.promo = promo;
        this.isNew = isNew;
        this.a = a;
        this.img = img;
        this.k = k;
    }

    // --- Getters et Setters ---

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getN() {
        return n;
    }
    public void setN(String n) {
        this.n = n;
    }

    public String getC() {
        return c;
    }
    public void setC(String c) {
        this.c = c;
    }

    public String getD() {
        return d;
    }
    public void setD(String d) {
        this.d = d;
    }

    public String getP() {
        return p;
    }
    public void setP(String p) {
        this.p = p;
    }

    public String getOp() {
        return op;
    }
    public void setOp(String op) {
        this.op = op;
    }

    public String getPromo() {
        return promo;
    }
    public void setPromo(String promo) {
        this.promo = promo;
    }

    public boolean isNew() {
        return isNew;
    }
    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public String getA() {
        return a;
    }
    public void setA(String a) {
        this.a = a;
    }

    public String getImg() {
        return img;
    }
    public void setImg(String img) {
        this.img = img;
    }

    public String getK() {
        return k;
    }
    public void setK(String k) {
        this.k = k;
    }
}