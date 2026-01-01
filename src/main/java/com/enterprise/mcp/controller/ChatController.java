package com.enterprise.mcp.controller;

import com.enterprise.mcp.mcp.McpCapabilityHandler;
import com.enterprise.mcp.mcp.McpResponse;
import com.enterprise.mcp.security.McpAccessDeniedException;
import com.enterprise.mcp.security.McpCapability;
import com.enterprise.mcp.security.McpRole;
import com.enterprise.mcp.security.McpSecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contrôleur pour l'interface de chat avec l'IA
 * 
 * Ce contrôleur expose les endpoints pour interagir avec l'assistant IA.
 * En production, il serait connecté à un vrai LLM via Spring AI.
 * Pour la démo, il permet de tester directement les capacités MCP.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final McpCapabilityHandler capabilityHandler;
    private final McpSecurityContext securityContext;
    private final ChatClient chatClient;

    private static final int MAX_TURNS_PER_CONVERSATION = 20;
    private static final Map<String, Deque<ConversationTurn>> CONVERSATIONS = new ConcurrentHashMap<>();
    
    /**
     * Endpoint principal de chat
     * En production : connecté au LLM qui orchestre les appels MCP
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        
        // Initialisation du contexte de sécurité
        initializeSecurityContext(authentication);
        
        log.info("Message reçu de {}: {}", authentication.getName(), request.message());
        
        // Pour la démo, on simule une réponse
        // En production, ce serait le LLM qui analyserait le message
        // et appellerait les capacités MCP appropriées
        String response = "Je suis l'assistant IA de gestion commerciale. " +
            "Je peux vous aider à:\n" +
            "- Rechercher des commandes (findOrder)\n" +
            "- Analyser des factures (analyzeInvoice)\n" +
            "- Résumer l'activité client (summarizeCustomerActivity)\n" +
            "- Créer des commandes (createOrder) - si vous avez les droits\n\n" +
            "Que souhaitez-vous faire?";
        
        return ResponseEntity.ok(new ChatResponse(response, null, null));
    }

    @PostMapping("/llm/message")
    public ResponseEntity<ChatResponse> chatWithLlm(
            @RequestBody ChatRequest request,
            Authentication authentication) {

        initializeSecurityContext(authentication);

        log.info("LLM message reçu de {}: {}", authentication.getName(), request.message());

        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
            ? UUID.randomUUID().toString()
            : request.conversationId();

        String conversationKey = authentication.getName() + ":" + conversationId;
        Deque<ConversationTurn> history = CONVERSATIONS.computeIfAbsent(conversationKey, k -> new ArrayDeque<>());

        McpRole role = securityContext.getRole();
        String allowedTools = role.getAllowedCapabilities().stream()
            .map(McpCapability::getName)
            .sorted()
            .reduce((a, b) -> a + ", " + b)
            .orElse("(aucun)");

        StringBuilder historyText = new StringBuilder();
        if (!history.isEmpty()) {
            historyText.append("Historique de conversation (le plus ancien en premier):\n");
            for (ConversationTurn turn : history) {
                historyText.append("Utilisateur: ").append(turn.userMessage()).append("\n");
                historyText.append("Assistant: ").append(turn.assistantMessage()).append("\n\n");
            }
        }

        try {
            String response = chatClient
                .prompt()
                .system(s -> s.text(
                    "Tu es un assistant interne de gestion commerciale. " +
                    "Contexte de sécurité: l'utilisateur courant a le rôle '" + role.name() + "'. " +
                    "Outils AUTORISÉS pour ce rôle: " + allowedTools + ". " +
                    "Règle impérative: n'appelle JAMAIS un outil en dehors de la liste AUTORISÉE, même si l'utilisateur le demande. " +
                    "Si une action requiert un outil non autorisé, explique que l'utilisateur n'a pas les droits et propose une alternative (ex: consulter commandes/factures, ou demander un rôle MANAGER/ADMIN). " +
                    "Pour les outils qui nécessitent confirmation (ex: createOrder), si tu es autorisé à l'appeler, appelle d'abord l'outil avec confirmed=false et demande confirmation avant de poursuivre.\n\n" +
                    historyText))
                .user(request.message())
                .call()
                .content();

            history.addLast(new ConversationTurn(request.message(), response));
            while (history.size() > MAX_TURNS_PER_CONVERSATION) {
                history.removeFirst();
            }

            return ResponseEntity.ok(new ChatResponse(response, null, conversationId));
        } catch (McpAccessDeniedException e) {
            log.warn("Accès refusé pendant l'exécution LLM pour {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.status(403).body(new ChatResponse(e.getMessage(), null, conversationId));
        }
    }
    
    /**
     * Test direct de la capacité findOrder
     */
    @GetMapping("/test/find-order/{orderNumber}")
    public ResponseEntity<McpResponse> testFindOrder(
            @PathVariable String orderNumber,
            Authentication authentication) {
        
        initializeSecurityContext(authentication);
        return ResponseEntity.ok(capabilityHandler.findOrder(orderNumber));
    }
    
    /**
     * Test direct de la capacité analyzeInvoice
     */
    @GetMapping("/test/analyze-invoice/{invoiceNumber}")
    public ResponseEntity<McpResponse> testAnalyzeInvoice(
            @PathVariable String invoiceNumber,
            Authentication authentication) {
        
        initializeSecurityContext(authentication);
        return ResponseEntity.ok(capabilityHandler.analyzeInvoice(invoiceNumber));
    }
    
    /**
     * Test direct de la capacité summarizeCustomerActivity
     */
    @GetMapping("/test/customer-summary/{customerCode}")
    public ResponseEntity<McpResponse> testCustomerSummary(
            @PathVariable String customerCode,
            Authentication authentication) {
        
        initializeSecurityContext(authentication);
        return ResponseEntity.ok(capabilityHandler.summarizeCustomerActivity(customerCode));
    }
    
    /**
     * Récupère les capacités disponibles pour l'utilisateur courant
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getCapabilities(Authentication authentication) {
        initializeSecurityContext(authentication);
        
        McpRole role = getRoleFromAuthentication(authentication);
        
        return ResponseEntity.ok(Map.of(
            "user", authentication.getName(),
            "role", role.name(),
            "capabilities", role.getAllowedCapabilities().stream()
                .map(cap -> Map.of(
                    "name", cap.getName(),
                    "description", cap.getDescription(),
                    "requiresConfirmation", cap.requiresConfirmation()
                ))
                .toList()
        ));
    }
    
    /**
     * Initialise le contexte de sécurité MCP à partir de l'authentification Spring
     */
    private void initializeSecurityContext(Authentication authentication) {
        McpRole role = getRoleFromAuthentication(authentication);
        
        securityContext.initialize(
            authentication.getName(),
            authentication.getName(),
            role,
            UUID.randomUUID().toString(),
            "127.0.0.1" // En production : récupérer la vraie IP
        );
    }
    
    /**
     * Convertit le rôle Spring Security en rôle MCP
     */
    private McpRole getRoleFromAuthentication(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority().replace("ROLE_", "");
            try {
                return McpRole.valueOf(role);
            } catch (IllegalArgumentException ignored) {
                // Continuer avec le prochain rôle
            }
        }
        return McpRole.SUPPORT; // Rôle par défaut
    }
    
    // DTOs
    public record ChatRequest(String message, String conversationId) {}
    public record ChatResponse(String response, String correlationId, String conversationId) {}

    private record ConversationTurn(String userMessage, String assistantMessage) {}
}
