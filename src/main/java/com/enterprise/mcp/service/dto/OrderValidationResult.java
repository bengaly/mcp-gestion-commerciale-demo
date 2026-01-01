package com.enterprise.mcp.service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de la validation d'une commande
 * 
 * Ce DTO est crucial pour le workflow MCP : il permet à l'IA de comprendre
 * si une commande peut être créée et quels sont les éventuels problèmes.
 */
@Data
@Builder
public class OrderValidationResult {
    
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private BigDecimal estimatedTotal;
    
    /**
     * Crée un résultat de validation valide
     */
    public static OrderValidationResult valid(List<String> warnings, BigDecimal estimatedTotal) {
        return OrderValidationResult.builder()
            .valid(true)
            .errors(new ArrayList<>())
            .warnings(warnings != null ? warnings : new ArrayList<>())
            .estimatedTotal(estimatedTotal)
            .build();
    }
    
    /**
     * Crée un résultat de validation invalide
     */
    public static OrderValidationResult invalid(List<String> errors) {
        return OrderValidationResult.builder()
            .valid(false)
            .errors(errors != null ? errors : new ArrayList<>())
            .warnings(new ArrayList<>())
            .estimatedTotal(BigDecimal.ZERO)
            .build();
    }
    
    /**
     * Génère un message explicatif pour l'IA
     */
    public String toExplanation() {
        StringBuilder sb = new StringBuilder();
        
        if (valid) {
            sb.append("✅ La commande est valide et peut être créée.\n");
            sb.append("Montant estimé: ").append(String.format("%,.2f €", estimatedTotal)).append("\n");
            
            if (!warnings.isEmpty()) {
                sb.append("\n⚠️ Points d'attention:\n");
                warnings.forEach(w -> sb.append("- ").append(w).append("\n"));
            }
        } else {
            sb.append("❌ La commande ne peut pas être créée.\n\n");
            sb.append("Erreurs:\n");
            errors.forEach(e -> sb.append("- ").append(e).append("\n"));
        }
        
        return sb.toString();
    }
}
