package tn.kamel.backend.model;

public enum ProductCategory {
    VELOS("VÉLOS"),
    MOTOS("MOTOS"),
    ACCESSOIRES("ACCESSOIRES"),
    TROTTINETTES("TROTTINETTES");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}