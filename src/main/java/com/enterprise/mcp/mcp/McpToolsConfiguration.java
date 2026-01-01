package com.enterprise.mcp.mcp;

import com.enterprise.mcp.service.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * Configuration des outils MCP exposés à Spring AI
 * 
 * Cette classe définit les "Tools" (outils) que l'IA peut utiliser.
 * Chaque outil correspond à une capacité MCP et est mappé vers le handler approprié.
 * 
 * IMPORTANT : C'est ici que se fait le lien entre Spring AI et notre système MCP.
 * Les outils sont automatiquement découverts par Spring AI et proposés au LLM.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class McpToolsConfiguration {
    
    private final McpCapabilityHandler capabilityHandler;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
            .defaultFunctions(
                "findOrder",
                "analyzeInvoice",
                "summarizeCustomerActivity",
                "createOrder"
            )
            .build();
    }
    
    /**
     * Outil : Rechercher une commande
     * 
     * Le LLM peut appeler cet outil pour rechercher une commande par son numéro.
     */
    @Bean
    @Description("Recherche une commande par son numéro. Retourne les détails complets de la commande incluant le client, les lignes, les montants et le statut.")
    public Function<FindOrderRequest, String> findOrder() {
        return request -> {
            log.info("Tool findOrder appelé avec: {}", request.orderNumber());
            McpResponse response = capabilityHandler.findOrder(request.orderNumber());
            return response.toAIFormat();
        };
    }
    
    /**
     * Outil : Analyser une facture
     * 
     * Le LLM peut appeler cet outil pour obtenir une analyse détaillée d'une facture.
     */
    @Bean
    @Description("Analyse une facture en profondeur. Retourne le statut de paiement, les indicateurs de risque, les recommandations d'action et l'historique du client.")
    public Function<AnalyzeInvoiceRequest, String> analyzeInvoice() {
        return request -> {
            log.info("Tool analyzeInvoice appelé avec: {}", request.invoiceNumber());
            McpResponse response = capabilityHandler.analyzeInvoice(request.invoiceNumber());
            return response.toAIFormat();
        };
    }
    
    /**
     * Outil : Résumer l'activité d'un client
     * 
     * Le LLM peut appeler cet outil pour obtenir un résumé complet de l'activité d'un client.
     */
    @Bean
    @Description("Génère un résumé complet de l'activité d'un client incluant ses commandes récentes, ses factures, sa situation financière et ses indicateurs de fidélité.")
    public Function<SummarizeCustomerRequest, String> summarizeCustomerActivity() {
        return request -> {
            log.info("Tool summarizeCustomerActivity appelé avec: {}", request.customerCode());
            McpResponse response = capabilityHandler.summarizeCustomerActivity(request.customerCode());
            return response.toAIFormat();
        };
    }
    
    /**
     * Outil : Créer une commande
     * 
     * Le LLM peut appeler cet outil pour créer une nouvelle commande.
     * ATTENTION : Cet outil nécessite une confirmation utilisateur.
     */
    @Bean
    @Description("Crée une nouvelle commande pour un client. Nécessite le code client, les lignes de commande avec produits et quantités. Demande confirmation avant création effective.")
    public Function<CreateOrderToolRequest, String> createOrder() {
        return request -> {
            log.info("Tool createOrder appelé pour client: {}", request.customerCode());
            
            // Conversion de la requête tool vers la requête service
            CreateOrderRequest serviceRequest = convertToServiceRequest(request);
            
            McpResponse response = capabilityHandler.createOrder(serviceRequest, request.confirmed());
            return response.toAIFormat();
        };
    }
    
    /**
     * Convertit une requête tool en requête service
     */
    private CreateOrderRequest convertToServiceRequest(CreateOrderToolRequest toolRequest) {
        List<CreateOrderRequest.OrderLineRequest> lines = toolRequest.lines().stream()
            .map(line -> CreateOrderRequest.OrderLineRequest.builder()
                .productCode(line.productCode())
                .productName(line.productName())
                .quantity(line.quantity())
                .unitPrice(line.unitPrice())
                .discountPercent(line.discountPercent())
                .build())
            .toList();
        
        return CreateOrderRequest.builder()
            .customerCode(toolRequest.customerCode())
            .lines(lines)
            .shippingAddress(toolRequest.shippingAddress())
            .notes(toolRequest.notes())
            .build();
    }
    
    // === Records pour les paramètres des outils ===
    
    /**
     * Paramètres pour la recherche de commande
     * @param orderNumber Numéro de la commande à rechercher (ex: CMD-20240115-ABC123)
     */
    public record FindOrderRequest(
        String orderNumber
    ) {}
    
    /**
     * Paramètres pour l'analyse de facture
     * @param invoiceNumber Numéro de la facture à analyser (ex: FAC-2024-001234)
     */
    public record AnalyzeInvoiceRequest(
        String invoiceNumber
    ) {}
    
    /**
     * Paramètres pour le résumé client
     * @param customerCode Code unique du client (ex: CLI-001)
     */
    public record SummarizeCustomerRequest(
        String customerCode
    ) {}
    
    /**
     * Paramètres pour la création de commande
     * @param customerCode Code du client pour la commande
     * @param lines Liste des lignes de commande
     * @param shippingAddress Adresse de livraison (optionnel)
     * @param notes Notes additionnelles (optionnel)
     * @param confirmed True si l'utilisateur a confirmé la création
     */
    public record CreateOrderToolRequest(
        String customerCode,
        List<OrderLineToolRequest> lines,
        String shippingAddress,
        String notes,
        boolean confirmed
    ) {}
    
    /**
     * Ligne de commande
     * @param productCode Code produit
     * @param productName Nom du produit
     * @param quantity Quantité commandée
     * @param unitPrice Prix unitaire HT
     * @param discountPercent Pourcentage de remise (optionnel)
     */
    public record OrderLineToolRequest(
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent
    ) {}
}
