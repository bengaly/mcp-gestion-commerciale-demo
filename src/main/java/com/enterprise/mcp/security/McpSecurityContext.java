package com.enterprise.mcp.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Contexte de sécurité pour les appels MCP
 * 
 * Ce composant maintient le contexte de sécurité pour chaque requête MCP.
 * Il permet de tracer qui fait quoi et d'appliquer les restrictions de rôle.
 */
@Component
@RequestScope
@Getter
@Slf4j
public class McpSecurityContext {
    
    private String userId;
    private String username;
    private McpRole role;
    private String sessionId;
    private String clientIp;
    
    /**
     * Initialise le contexte de sécurité pour une requête
     */
    public void initialize(String userId, String username, McpRole role, String sessionId, String clientIp) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        
        log.debug("Contexte MCP initialisé - User: {}, Role: {}, Session: {}", username, role, sessionId);
    }
    
    /**
     * Vérifie si l'utilisateur courant a accès à une capacité
     */
    public boolean hasCapability(McpCapability capability) {
        if (role == null) {
            log.warn("Tentative d'accès sans rôle défini pour la capacité: {}", capability);
            return false;
        }
        
        boolean hasAccess = role.hasCapability(capability);
        
        if (!hasAccess) {
            log.warn("Accès refusé - User: {}, Role: {}, Capacité: {}", username, role, capability);
        }
        
        return hasAccess;
    }
    
    /**
     * Vérifie l'accès et lève une exception si non autorisé
     */
    public void requireCapability(McpCapability capability) {
        if (!hasCapability(capability)) {
            throw new McpAccessDeniedException(
                String.format("Accès refusé à la capacité '%s' pour le rôle '%s'", 
                    capability.getName(), role)
            );
        }
    }
    
    /**
     * Retourne une représentation pour l'audit
     */
    public String toAuditString() {
        return String.format("User[id=%s, name=%s, role=%s, session=%s, ip=%s]",
            userId, username, role, sessionId, clientIp);
    }
}
