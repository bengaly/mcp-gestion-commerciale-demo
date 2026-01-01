package com.enterprise.mcp.audit;

import com.enterprise.mcp.security.McpCapability;
import com.enterprise.mcp.security.McpSecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service d'audit pour les appels MCP
 * 
 * Ce service trace TOUS les appels MCP pour assurer :
 * - La traçabilité des actions IA
 * - La conformité réglementaire
 * - Le debugging et l'analyse
 * - La détection d'anomalies
 * 
 * En production, ce service serait connecté à un système de logging
 * centralisé (ELK, Splunk, etc.) et une base de données d'audit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpAuditService {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // En production : remplacer par une persistence réelle
    private final ConcurrentLinkedQueue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> capabilityUsageStats = new ConcurrentHashMap<>();
    
    /**
     * Enregistre le début d'un appel de capacité MCP
     */
    public String startCapabilityCall(McpSecurityContext context, McpCapability capability, Map<String, Object> parameters) {
        String correlationId = generateCorrelationId();
        
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .userId(context.getUserId())
            .username(context.getUsername())
            .role(context.getRole().name())
            .sessionId(context.getSessionId())
            .clientIp(context.getClientIp())
            .capability(capability.getName())
            .parameters(sanitizeParameters(parameters))
            .status(AuditStatus.STARTED)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-START] {} | User: {} | Role: {} | Capability: {} | Params: {}",
            correlationId, context.getUsername(), context.getRole(), capability.getName(), 
            sanitizeParameters(parameters));
        
        // Mise à jour des statistiques
        capabilityUsageStats.merge(capability.getName(), 1, Integer::sum);
        
        return correlationId;
    }
    
    /**
     * Enregistre la fin réussie d'un appel de capacité MCP
     */
    public void completeCapabilityCall(String correlationId, McpCapability capability, String resultSummary) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .capability(capability.getName())
            .resultSummary(resultSummary)
            .status(AuditStatus.COMPLETED)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-COMPLETE] {} | Capability: {} | Result: {}",
            correlationId, capability.getName(), truncate(resultSummary, 200));
    }
    
    /**
     * Enregistre l'échec d'un appel de capacité MCP
     */
    public void failCapabilityCall(String correlationId, McpCapability capability, String error) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .capability(capability.getName())
            .errorMessage(error)
            .status(AuditStatus.FAILED)
            .build();
        
        auditLog.add(entry);
        
        log.error("[AUDIT-FAILED] {} | Capability: {} | Error: {}",
            correlationId, capability.getName(), error);
    }
    
    /**
     * Enregistre un refus d'accès
     */
    public void logAccessDenied(McpSecurityContext context, McpCapability capability) {
        String correlationId = generateCorrelationId();
        
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .userId(context.getUserId())
            .username(context.getUsername())
            .role(context.getRole() != null ? context.getRole().name() : "NONE")
            .capability(capability.getName())
            .status(AuditStatus.ACCESS_DENIED)
            .build();
        
        auditLog.add(entry);
        
        log.warn("[AUDIT-ACCESS-DENIED] {} | User: {} | Role: {} | Capability: {}",
            correlationId, context.getUsername(), context.getRole(), capability.getName());
    }
    
    /**
     * Enregistre une action nécessitant confirmation
     */
    public void logConfirmationRequired(String correlationId, McpCapability capability, String actionSummary) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .capability(capability.getName())
            .resultSummary("CONFIRMATION REQUISE: " + actionSummary)
            .status(AuditStatus.PENDING_CONFIRMATION)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-CONFIRM-REQUIRED] {} | Capability: {} | Action: {}",
            correlationId, capability.getName(), actionSummary);
    }
    
    /**
     * Enregistre la confirmation d'une action
     */
    public void logConfirmationReceived(String correlationId, boolean confirmed, String confirmedBy) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .resultSummary(confirmed ? "CONFIRMÉ par " + confirmedBy : "REJETÉ par " + confirmedBy)
            .status(confirmed ? AuditStatus.CONFIRMED : AuditStatus.REJECTED)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-CONFIRMATION] {} | Confirmed: {} | By: {}",
            correlationId, confirmed, confirmedBy);
    }
    
    /**
     * Retourne les statistiques d'utilisation des capacités
     */
    public Map<String, Integer> getUsageStatistics() {
        return Map.copyOf(capabilityUsageStats);
    }
    
    /**
     * Retourne les N dernières entrées d'audit
     */
    public java.util.List<AuditEntry> getRecentAuditEntries(int count) {
        return auditLog.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(count)
            .toList();
    }
    
    private String generateCorrelationId() {
        return "MCP-" + System.currentTimeMillis() + "-" + 
            java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private Map<String, Object> sanitizeParameters(Map<String, Object> parameters) {
        if (parameters == null) return Map.of();
        
        // Masquer les données sensibles
        Map<String, Object> sanitized = new java.util.HashMap<>(parameters);
        
        // Liste des champs sensibles à masquer
        java.util.List<String> sensitiveFields = java.util.List.of(
            "password", "creditCard", "ssn", "token", "secret"
        );
        
        for (String field : sensitiveFields) {
            if (sanitized.containsKey(field)) {
                sanitized.put(field, "***MASKED***");
            }
        }
        
        return sanitized;
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    public enum AuditStatus {
        STARTED,
        COMPLETED,
        FAILED,
        ACCESS_DENIED,
        PENDING_CONFIRMATION,
        CONFIRMED,
        REJECTED
    }
    
    @lombok.Data
    @lombok.Builder
    public static class AuditEntry {
        private String correlationId;
        private LocalDateTime timestamp;
        private String userId;
        private String username;
        private String role;
        private String sessionId;
        private String clientIp;
        private String capability;
        private Map<String, Object> parameters;
        private String resultSummary;
        private String errorMessage;
        private AuditStatus status;
        
        public String toLogString() {
            return String.format("[%s] %s | %s | User: %s | Role: %s | Status: %s",
                timestamp.format(FORMATTER), correlationId, capability, username, role, status);
        }
    }
}
