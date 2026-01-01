package com.enterprise.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application principale MCP Enterprise Demo
 * 
 * Démontre l'intégration de MCP (Model Context Protocol) dans un SI d'entreprise
 * pour la gestion commerciale (Commandes, Factures, Clients).
 * 
 * Architecture :
 * - MCP Server exposant des capacités métier contrôlées
 * - Services métiers isolés de l'IA
 * - Sécurité par rôles (SUPPORT, MANAGER, ADMIN)
 * - Audit complet des interactions IA
 */
@SpringBootApplication
public class McpEnterpriseApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(McpEnterpriseApplication.class, args);
    }
}
