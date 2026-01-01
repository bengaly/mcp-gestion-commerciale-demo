package com.enterprise.mcp.mcp;

import com.enterprise.mcp.audit.McpAuditService;
import com.enterprise.mcp.domain.entity.Order;
import com.enterprise.mcp.domain.entity.Product;
import com.enterprise.mcp.security.McpCapability;
import com.enterprise.mcp.security.McpSecurityContext;
import com.enterprise.mcp.service.CustomerService;
import com.enterprise.mcp.service.InvoiceService;
import com.enterprise.mcp.service.OrderService;
import com.enterprise.mcp.service.ProductService;
import com.enterprise.mcp.service.dto.CreateOrderRequest;
import com.enterprise.mcp.service.dto.CustomerActivitySummary;
import com.enterprise.mcp.service.dto.InvoiceAnalysis;
import com.enterprise.mcp.service.dto.OrderValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Gestionnaire central des capacités MCP
 * 
 * Ce composant fait le lien entre les appels MCP et les services métiers.
 * Il assure :
 * - La vérification des droits d'accès
 * - L'audit des appels
 * - La transformation des résultats pour l'IA
 * 
 * C'est la SEULE porte d'entrée pour l'IA vers le SI.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpCapabilityHandler {
    
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final ProductService productService;
    private final McpAuditService auditService;
    private final McpSecurityContext securityContext;
    
    /**
     * Capacité : Rechercher une commande
     * 
     * @param orderNumber Numéro de la commande à rechercher
     * @return Informations sur la commande formatées pour l'IA
     */
    public McpResponse findOrder(String orderNumber) {
        McpCapability capability = McpCapability.FIND_ORDER;
        
        // Vérification des droits
        securityContext.requireCapability(capability);
        
        // Audit - début
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability, Map.of("orderNumber", orderNumber)
        );
        
        try {
            Optional<Order> orderOpt = orderService.findByOrderNumber(orderNumber);
            
            if (orderOpt.isEmpty()) {
                String result = String.format("Aucune commande trouvée avec le numéro: %s", orderNumber);
                auditService.completeCapabilityCall(correlationId, capability, result);
                return McpResponse.notFound(result);
            }
            
            Order order = orderOpt.get();
            String formattedResult = formatOrderForAI(order);
            
            auditService.completeCapabilityCall(correlationId, capability, 
                "Commande trouvée: " + orderNumber);
            
            return McpResponse.success(formattedResult);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur lors de la recherche: " + e.getMessage());
        }
    }
    
    /**
     * Capacité : Analyser une facture
     * 
     * @param invoiceNumber Numéro de la facture à analyser
     * @return Analyse détaillée formatée pour l'IA
     */
    public McpResponse analyzeInvoice(String invoiceNumber) {
        McpCapability capability = McpCapability.ANALYZE_INVOICE;
        
        securityContext.requireCapability(capability);
        
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability, Map.of("invoiceNumber", invoiceNumber)
        );
        
        try {
            Optional<InvoiceAnalysis> analysisOpt = invoiceService.analyzeInvoice(invoiceNumber);
            
            if (analysisOpt.isEmpty()) {
                String result = String.format("Aucune facture trouvée avec le numéro: %s", invoiceNumber);
                auditService.completeCapabilityCall(correlationId, capability, result);
                return McpResponse.notFound(result);
            }
            
            InvoiceAnalysis analysis = analysisOpt.get();
            String formattedResult = analysis.toNaturalLanguageReport();
            
            auditService.completeCapabilityCall(correlationId, capability, 
                "Facture analysée: " + invoiceNumber + " - Risque: " + analysis.getRiskLevel());
            
            return McpResponse.success(formattedResult);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur lors de l'analyse: " + e.getMessage());
        }
    }
    
    /**
     * Capacité : Résumer l'activité d'un client
     * 
     * @param customerCode Code du client
     * @return Résumé complet de l'activité client
     */
    public McpResponse summarizeCustomerActivity(String customerCode) {
        McpCapability capability = McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY;
        
        securityContext.requireCapability(capability);
        
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability, Map.of("customerCode", customerCode)
        );
        
        try {
            Optional<CustomerActivitySummary> summaryOpt = customerService.summarizeActivity(customerCode);
            
            if (summaryOpt.isEmpty()) {
                String result = String.format("Aucun client trouvé avec le code: %s", customerCode);
                auditService.completeCapabilityCall(correlationId, capability, result);
                return McpResponse.notFound(result);
            }
            
            CustomerActivitySummary summary = summaryOpt.get();
            String formattedResult = summary.toNaturalLanguageSummary();
            
            auditService.completeCapabilityCall(correlationId, capability, 
                "Résumé généré pour: " + customerCode);
            
            return McpResponse.success(formattedResult);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur lors de la génération du résumé: " + e.getMessage());
        }
    }
    
    /**
     * Capacité : Créer une commande (AVEC CONFIRMATION)
     * 
     * Cette capacité est en deux étapes :
     * 1. Validation et demande de confirmation
     * 2. Création effective après confirmation
     * 
     * @param request Données de la commande à créer
     * @return Résultat de la validation ou confirmation de création
     */
    public McpResponse createOrder(CreateOrderRequest request, boolean confirmed) {
        McpCapability capability = McpCapability.CREATE_ORDER;
        
        securityContext.requireCapability(capability);
        
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability, 
            Map.of("customerCode", request.getCustomerCode(), "confirmed", confirmed)
        );
        
        try {
            // Étape 1 : Validation
            OrderValidationResult validation = orderService.validateOrderRequest(request);
            
            if (!validation.isValid()) {
                auditService.completeCapabilityCall(correlationId, capability, 
                    "Validation échouée: " + String.join(", ", validation.getErrors()));
                return McpResponse.validationFailed(validation.toExplanation());
            }
            
            // Si non confirmé, demander confirmation avec détails produits enrichis
            if (!confirmed) {
                String confirmationSummary = generateEnrichedConfirmationSummary(request);
                auditService.logConfirmationRequired(correlationId, capability, confirmationSummary);
                
                return McpResponse.requiresConfirmation(
                    correlationId,
                    confirmationSummary + "\n\n" + validation.toExplanation() +
                    "\n\n⚠️ Confirmez-vous la création de cette commande?"
                );
            }
            
            // Étape 2 : Création après confirmation
            auditService.logConfirmationReceived(correlationId, true, securityContext.getUsername());
            
            Order createdOrder = orderService.createOrder(request, securityContext.getUsername());
            
            String successMessage = String.format(
                "✅ Commande créée avec succès!\n\n" +
                "Numéro de commande: %s\n" +
                "Client: %s\n" +
                "Montant total: %,.2f €\n" +
                "Statut: %s\n\n" +
                "La commande est en attente de validation.",
                createdOrder.getOrderNumber(),
                createdOrder.getCustomer().getCompanyName(),
                createdOrder.getTotalAmount(),
                createdOrder.getStatus()
            );
            
            auditService.completeCapabilityCall(correlationId, capability, 
                "Commande créée: " + createdOrder.getOrderNumber());
            
            return McpResponse.success(successMessage);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur lors de la création: " + e.getMessage());
        }
    }
    
    /**
     * Formate une commande pour l'affichage IA
     */
    private String formatOrderForAI(Order order) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== Commande ").append(order.getOrderNumber()).append(" ===\n\n");
        sb.append("**Client:** ").append(order.getCustomer().getCompanyName()).append("\n");
        sb.append("**Statut:** ").append(order.getStatus()).append("\n");
        sb.append("**Date de commande:** ").append(order.getOrderDate().toLocalDate()).append("\n");
        
        if (order.getExpectedDeliveryDate() != null) {
            sb.append("**Livraison prévue:** ").append(order.getExpectedDeliveryDate().toLocalDate()).append("\n");
        }
        
        sb.append("\n**Lignes de commande:**\n");
        order.getLines().forEach(line -> {
            sb.append("- ").append(line.getProductName())
              .append(" (").append(line.getProductCode()).append(")")
              .append(" x ").append(line.getQuantity())
              .append(" @ ").append(String.format("%,.2f €", line.getUnitPrice()))
              .append(" = ").append(String.format("%,.2f €", line.getLineTotal()))
              .append("\n");
        });
        
        sb.append("\n**Total HT:** ").append(String.format("%,.2f €", order.getTotalAmount())).append("\n");
        sb.append("**TVA:** ").append(String.format("%,.2f €", order.getTaxAmount())).append("\n");
        sb.append("**Total TTC:** ").append(String.format("%,.2f €", 
            order.getTotalAmount().add(order.getTaxAmount()))).append("\n");
        
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            sb.append("\n**Notes:** ").append(order.getNotes()).append("\n");
        }
        
        sb.append("\n**Adresse de livraison:** ").append(order.getShippingAddress()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Génère un résumé de confirmation enrichi avec les détails des produits
     * récupérés depuis la base de données
     */
    private String generateEnrichedConfirmationSummary(CreateOrderRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Confirmation de commande ===\n\n");
        sb.append("**Client:** ").append(request.getCustomerCode()).append("\n");
        
        if (request.getShippingAddress() != null) {
            sb.append("**Adresse de livraison:** ").append(request.getShippingAddress()).append("\n");
        }
        
        sb.append("\n**Articles:**\n");
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        
        for (CreateOrderRequest.OrderLineRequest line : request.getLines()) {
            // Récupérer les infos produit depuis la base
            Product product = productService.findByProductCode(line.getProductCode()).orElse(null);
            
            String productName = product != null ? product.getName() : line.getProductCode();
            java.math.BigDecimal unitPrice = line.getUnitPrice() != null ? line.getUnitPrice() 
                : (product != null ? product.getUnitPrice() : java.math.BigDecimal.ZERO);
            int quantity = line.getQuantity() != null ? line.getQuantity() : 1;
            
            java.math.BigDecimal lineTotal = unitPrice.multiply(new java.math.BigDecimal(quantity));
            if (line.getDiscountPercent() != null && line.getDiscountPercent().compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal discount = lineTotal.multiply(line.getDiscountPercent()).divide(new java.math.BigDecimal("100"));
                lineTotal = lineTotal.subtract(discount);
            }
            total = total.add(lineTotal);
            
            sb.append("- **").append(productName).append("** (")
              .append(line.getProductCode()).append(")")
              .append(" x ").append(quantity)
              .append(" @ ").append(String.format("%,.2f €", unitPrice))
              .append(" = ").append(String.format("%,.2f €", lineTotal))
              .append("\n");
        }
        
        sb.append("\n**Total HT:** ").append(String.format("%,.2f €", total));
        sb.append("\n**TVA (20%):** ").append(String.format("%,.2f €", total.multiply(new java.math.BigDecimal("0.20"))));
        sb.append("\n**Total TTC:** ").append(String.format("%,.2f €", total.multiply(new java.math.BigDecimal("1.20"))));
        
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            sb.append("\n\n**Notes:** ").append(request.getNotes());
        }
        
        return sb.toString();
    }
}
