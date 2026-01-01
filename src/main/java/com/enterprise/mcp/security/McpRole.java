package com.enterprise.mcp.security;

import java.util.Set;

/**
 * Définition des rôles MCP et leurs capacités associées
 * 
 * Principe de sécurité : chaque rôle a accès uniquement aux capacités MCP
 * nécessaires à ses fonctions. C'est le RBAC (Role-Based Access Control)
 * appliqué au niveau MCP.
 */
public enum McpRole {
    
    /**
     * Rôle SUPPORT : accès en lecture seule
     * - Peut consulter commandes, factures, clients
     * - Ne peut pas créer ou modifier des données
     */
    SUPPORT(Set.of(
        McpCapability.FIND_ORDER,
        McpCapability.ANALYZE_INVOICE,
        McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY
    )),
    
    /**
     * Rôle MANAGER : accès étendu avec création
     * - Toutes les capacités SUPPORT
     * - Peut créer des commandes
     * - Peut valider des commandes
     */
    MANAGER(Set.of(
        McpCapability.FIND_ORDER,
        McpCapability.ANALYZE_INVOICE,
        McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY,
        McpCapability.CREATE_ORDER,
        McpCapability.VALIDATE_ORDER
    )),
    
    /**
     * Rôle ADMIN : accès complet
     * - Toutes les capacités
     * - Accès aux fonctions d'administration
     */
    ADMIN(Set.of(McpCapability.values()));
    
    private final Set<McpCapability> allowedCapabilities;
    
    McpRole(Set<McpCapability> allowedCapabilities) {
        this.allowedCapabilities = allowedCapabilities;
    }
    
    /**
     * Vérifie si ce rôle a accès à une capacité donnée
     */
    public boolean hasCapability(McpCapability capability) {
        return allowedCapabilities.contains(capability);
    }
    
    /**
     * Retourne l'ensemble des capacités autorisées pour ce rôle
     */
    public Set<McpCapability> getAllowedCapabilities() {
        return Set.copyOf(allowedCapabilities);
    }
}
