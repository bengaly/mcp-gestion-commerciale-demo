package com.enterprise.mcp.security;

/**
 * Énumération des capacités MCP disponibles dans le système
 * 
 * Chaque capacité représente une action spécifique que l'IA peut demander.
 * Ces capacités sont le SEUL moyen pour l'IA d'interagir avec le SI.
 */
public enum McpCapability {
    
    /**
     * Recherche et consultation d'une commande
     */
    FIND_ORDER("findOrder", "Rechercher une commande par son numéro", false),
    
    /**
     * Analyse détaillée d'une facture
     */
    ANALYZE_INVOICE("analyzeInvoice", "Analyser une facture avec indicateurs de risque", false),
    
    /**
     * Résumé de l'activité d'un client
     */
    SUMMARIZE_CUSTOMER_ACTIVITY("summarizeCustomerActivity", "Générer un résumé de l'activité client", false),
    
    /**
     * Création d'une nouvelle commande
     */
    CREATE_ORDER("createOrder", "Créer une nouvelle commande", true),
    
    /**
     * Validation d'une commande en attente
     */
    VALIDATE_ORDER("validateOrder", "Valider une commande en attente", true),
    
    /**
     * Annulation d'une commande
     */
    CANCEL_ORDER("cancelOrder", "Annuler une commande", true),
    
    /**
     * Enregistrement d'un paiement
     */
    RECORD_PAYMENT("recordPayment", "Enregistrer un paiement sur une facture", true);
    
    private final String name;
    private final String description;
    private final boolean requiresConfirmation;
    
    McpCapability(String name, String description, boolean requiresConfirmation) {
        this.name = name;
        this.description = description;
        this.requiresConfirmation = requiresConfirmation;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Indique si cette capacité nécessite une confirmation utilisateur
     * avant exécution (pour les actions qui modifient des données)
     */
    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }
    
    /**
     * Recherche une capacité par son nom
     */
    public static McpCapability fromName(String name) {
        for (McpCapability capability : values()) {
            if (capability.getName().equals(name)) {
                return capability;
            }
        }
        throw new IllegalArgumentException("Capacité MCP inconnue: " + name);
    }
}
