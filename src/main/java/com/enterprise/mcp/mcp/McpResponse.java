package com.enterprise.mcp.mcp;

import lombok.Builder;
import lombok.Data;

/**
 * Réponse standardisée pour les capacités MCP
 * 
 * Cette classe encapsule toutes les réponses MCP de manière uniforme.
 * Elle permet à l'IA de comprendre facilement le résultat de ses appels.
 */
@Data
@Builder
public class McpResponse {
    
    private ResponseStatus status;
    private String content;
    private String correlationId;
    private boolean requiresConfirmation;
    
    public enum ResponseStatus {
        SUCCESS,
        NOT_FOUND,
        VALIDATION_FAILED,
        REQUIRES_CONFIRMATION,
        ERROR,
        ACCESS_DENIED
    }
    
    /**
     * Crée une réponse de succès
     */
    public static McpResponse success(String content) {
        return McpResponse.builder()
            .status(ResponseStatus.SUCCESS)
            .content(content)
            .requiresConfirmation(false)
            .build();
    }
    
    /**
     * Crée une réponse "non trouvé"
     */
    public static McpResponse notFound(String message) {
        return McpResponse.builder()
            .status(ResponseStatus.NOT_FOUND)
            .content(message)
            .requiresConfirmation(false)
            .build();
    }
    
    /**
     * Crée une réponse d'échec de validation
     */
    public static McpResponse validationFailed(String message) {
        return McpResponse.builder()
            .status(ResponseStatus.VALIDATION_FAILED)
            .content(message)
            .requiresConfirmation(false)
            .build();
    }
    
    /**
     * Crée une réponse nécessitant confirmation
     */
    public static McpResponse requiresConfirmation(String correlationId, String message) {
        return McpResponse.builder()
            .status(ResponseStatus.REQUIRES_CONFIRMATION)
            .content(message)
            .correlationId(correlationId)
            .requiresConfirmation(true)
            .build();
    }
    
    /**
     * Crée une réponse d'erreur
     */
    public static McpResponse error(String message) {
        return McpResponse.builder()
            .status(ResponseStatus.ERROR)
            .content(message)
            .requiresConfirmation(false)
            .build();
    }
    
    /**
     * Crée une réponse d'accès refusé
     */
    public static McpResponse accessDenied(String message) {
        return McpResponse.builder()
            .status(ResponseStatus.ACCESS_DENIED)
            .content(message)
            .requiresConfirmation(false)
            .build();
    }
    
    /**
     * Vérifie si la réponse est un succès
     */
    public boolean isSuccess() {
        return status == ResponseStatus.SUCCESS;
    }
    
    /**
     * Formate la réponse pour l'IA
     */
    public String toAIFormat() {
        StringBuilder sb = new StringBuilder();
        
        switch (status) {
            case SUCCESS:
                sb.append(content);
                break;
            case NOT_FOUND:
                sb.append("❌ Élément non trouvé\n\n").append(content);
                break;
            case VALIDATION_FAILED:
                sb.append("⚠️ Validation échouée\n\n").append(content);
                break;
            case REQUIRES_CONFIRMATION:
                sb.append("🔔 Confirmation requise\n\n").append(content);
                if (correlationId != null) {
                    sb.append("\n\n[ID de corrélation: ").append(correlationId).append("]");
                }
                break;
            case ERROR:
                sb.append("❌ Erreur\n\n").append(content);
                break;
            case ACCESS_DENIED:
                sb.append("🚫 Accès refusé\n\n").append(content);
                break;
        }
        
        return sb.toString();
    }
}
