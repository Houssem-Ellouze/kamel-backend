package tn.kamel.backend.model;

public enum ProductAvailability {
    DISPONIBLE("Disponible"), SUR_DEMANDE("Sur demande");

    private final String label;
    ProductAvailability(String label) { this.label = label; }
    public String getLabel() { return label; }
}