# Chapitre 6 : Sécurité et Audit

## 🎯 Objectifs du chapitre

- Maîtriser le modèle de sécurité RBAC pour MCP
- Implémenter un système d'audit complet
- Appliquer les bonnes pratiques entreprise

---

## 6.1 Pourquoi la sécurité MCP est critique

### Les risques sans sécurité

```
┌─────────────────────────────────────────────────────────────────┐
│                    RISQUES SANS SÉCURITÉ MCP                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 🔓 ACCÈS NON CONTRÔLÉ                                           │
│    └─► N'importe qui peut appeler n'importe quelle capacité     │
│                                                                  │
│ 📝 PAS DE TRAÇABILITÉ                                           │
│    └─► Impossible de savoir qui a fait quoi                     │
│                                                                  │
│ ⚡ ACTIONS NON VALIDÉES                                          │
│    └─► L'IA peut créer/modifier des données sans contrôle       │
│                                                                  │
│ 🎭 USURPATION D'IDENTITÉ                                        │
│    └─► Pas de lien entre l'utilisateur et les actions IA        │
│                                                                  │
│ 📊 NON-CONFORMITÉ                                                │
│    └─► RGPD, SOX, audits internes impossibles                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Les principes de sécurité MCP

1. **Moindre privilège** : Chaque rôle n'a accès qu'aux capacités nécessaires
2. **Défense en profondeur** : Plusieurs couches de vérification
3. **Audit complet** : Tout est tracé
4. **Confirmation explicite** : Les actions sensibles nécessitent validation

---

## 6.2 Le modèle RBAC (Role-Based Access Control)

### Définition des capacités

```java
public enum McpCapability {
    
    // Capacités de lecture
    FIND_ORDER("findOrder", "Rechercher une commande", false),
    ANALYZE_INVOICE("analyzeInvoice", "Analyser une facture", false),
    SUMMARIZE_CUSTOMER_ACTIVITY("summarizeCustomerActivity", 
                                 "Résumé client", false),
    
    // Capacités d'écriture (confirmation requise)
    CREATE_ORDER("createOrder", "Créer une commande", true),
    VALIDATE_ORDER("validateOrder", "Valider une commande", true),
    CANCEL_ORDER("cancelOrder", "Annuler une commande", true),
    RECORD_PAYMENT("recordPayment", "Enregistrer un paiement", true);
    
    private final String name;
    private final String description;
    private final boolean requiresConfirmation;
    
    /**
     * Indique si cette capacité nécessite une confirmation
     * explicite de l'utilisateur avant exécution
     */
    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }
}
```

### Définition des rôles

```java
public enum McpRole {
    
    /**
     * SUPPORT : Lecture seule
     * Cas d'usage : Répondre aux questions clients
     */
    SUPPORT(Set.of(
        McpCapability.FIND_ORDER,
        McpCapability.ANALYZE_INVOICE,
        McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY
    )),
    
    /**
     * MANAGER : Lecture + Création
     * Cas d'usage : Gérer les commandes clients
     */
    MANAGER(Set.of(
        McpCapability.FIND_ORDER,
        McpCapability.ANALYZE_INVOICE,
        McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY,
        McpCapability.CREATE_ORDER,
        McpCapability.VALIDATE_ORDER
    )),
    
    /**
     * ADMIN : Accès complet
     * Cas d'usage : Administration système
     */
    ADMIN(Set.of(McpCapability.values()));
    
    private final Set<McpCapability> allowedCapabilities;
    
    public boolean hasCapability(McpCapability capability) {
        return allowedCapabilities.contains(capability);
    }
    
    public Set<McpCapability> getAllowedCapabilities() {
        return Set.copyOf(allowedCapabilities);
    }
}
```

### Matrice de permissions

```
┌─────────────────────────────────────────────────────────────────┐
│                    MATRICE RBAC COMPLÈTE                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Capacité                  │ SUPPORT │ MANAGER │ ADMIN │ Conf.  │
│  ──────────────────────────┼─────────┼─────────┼───────┼──────  │
│  findOrder                 │   ✅    │   ✅    │  ✅   │  Non   │
│  analyzeInvoice            │   ✅    │   ✅    │  ✅   │  Non   │
│  summarizeCustomerActivity │   ✅    │   ✅    │  ✅   │  Non   │
│  createOrder               │   ❌    │   ✅    │  ✅   │  OUI   │
│  validateOrder             │   ❌    │   ✅    │  ✅   │  OUI   │
│  cancelOrder               │   ❌    │   ❌    │  ✅   │  OUI   │
│  recordPayment             │   ❌    │   ❌    │  ✅   │  OUI   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6.3 Le contexte de sécurité

### McpSecurityContext

```java
@Component
@RequestScope  // Un contexte par requête HTTP
@Getter
@Slf4j
public class McpSecurityContext {
    
    private String userId;
    private String username;
    private McpRole role;
    private String sessionId;
    private String clientIp;
    
    /**
     * Initialise le contexte pour une requête
     */
    public void initialize(String userId, String username, 
                          McpRole role, String sessionId, String clientIp) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        
        log.debug("Contexte MCP initialisé - User: {}, Role: {}", username, role);
    }
    
    /**
     * Vérifie l'accès à une capacité
     */
    public boolean hasCapability(McpCapability capability) {
        if (role == null) {
            log.warn("Tentative d'accès sans rôle défini: {}", capability);
            return false;
        }
        return role.hasCapability(capability);
    }
    
    /**
     * Vérifie l'accès et lève une exception si refusé
     */
    public void requireCapability(McpCapability capability) {
        if (!hasCapability(capability)) {
            throw new McpAccessDeniedException(
                String.format("Accès refusé à '%s' pour le rôle '%s'", 
                    capability.getName(), role)
            );
        }
    }
    
    /**
     * Pour l'audit
     */
    public String toAuditString() {
        return String.format("User[id=%s, name=%s, role=%s, session=%s, ip=%s]",
            userId, username, role, sessionId, clientIp);
    }
}
```

### Initialisation du contexte

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private final McpSecurityContext securityContext;
    
    @GetMapping("/test/find-order/{orderNumber}")
    public ResponseEntity<McpResponse> testFindOrder(
            @PathVariable String orderNumber,
            Authentication authentication) {
        
        // Initialiser le contexte de sécurité
        initializeSecurityContext(authentication);
        
        // Le handler vérifiera automatiquement les droits
        return ResponseEntity.ok(capabilityHandler.findOrder(orderNumber));
    }
    
    private void initializeSecurityContext(Authentication auth) {
        McpRole role = getRoleFromAuthentication(auth);
        
        securityContext.initialize(
            auth.getName(),           // userId
            auth.getName(),           // username
            role,                     // McpRole
            UUID.randomUUID().toString(),  // sessionId
            "127.0.0.1"              // clientIp (à récupérer de la requête)
        );
    }
    
    private McpRole getRoleFromAuthentication(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String roleName = authority.getAuthority().replace("ROLE_", "");
            try {
                return McpRole.valueOf(roleName);
            } catch (IllegalArgumentException ignored) {}
        }
        return McpRole.SUPPORT;  // Rôle par défaut
    }
}
```

---

## 6.4 Le système d'audit

### Pourquoi auditer ?

1. **Conformité** : RGPD, SOX, audits internes
2. **Debugging** : Comprendre les erreurs
3. **Sécurité** : Détecter les comportements anormaux
4. **Analyse** : Statistiques d'utilisation

### McpAuditService

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class McpAuditService {
    
    // En production : persistance en base + ELK/Splunk
    private final ConcurrentLinkedQueue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> usageStats = new ConcurrentHashMap<>();
    
    /**
     * Enregistre le début d'un appel
     */
    public String startCapabilityCall(McpSecurityContext context, 
                                       McpCapability capability,
                                       Map<String, Object> parameters) {
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
        usageStats.merge(capability.getName(), 1, Integer::sum);
        
        log.info("[AUDIT-START] {} | User: {} | Role: {} | Capability: {}",
            correlationId, context.getUsername(), context.getRole(), 
            capability.getName());
        
        return correlationId;
    }
    
    /**
     * Enregistre la fin réussie
     */
    public void completeCapabilityCall(String correlationId, 
                                        McpCapability capability,
                                        String resultSummary) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .capability(capability.getName())
            .resultSummary(resultSummary)
            .status(AuditStatus.COMPLETED)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-COMPLETE] {} | Capability: {} | Result: {}",
            correlationId, capability.getName(), truncate(resultSummary, 100));
    }
    
    /**
     * Enregistre un échec
     */
    public void failCapabilityCall(String correlationId,
                                    McpCapability capability,
                                    String error) {
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
    public void logAccessDenied(McpSecurityContext context, 
                                McpCapability capability) {
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
            correlationId, context.getUsername(), context.getRole(), 
            capability.getName());
    }
    
    /**
     * Enregistre une demande de confirmation
     */
    public void logConfirmationRequired(String correlationId,
                                         McpCapability capability,
                                         String actionSummary) {
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
     * Enregistre la réception d'une confirmation
     */
    public void logConfirmationReceived(String correlationId,
                                         boolean confirmed,
                                         String confirmedBy) {
        AuditEntry entry = AuditEntry.builder()
            .correlationId(correlationId)
            .timestamp(LocalDateTime.now())
            .resultSummary(confirmed ? 
                "CONFIRMÉ par " + confirmedBy : 
                "REJETÉ par " + confirmedBy)
            .status(confirmed ? AuditStatus.CONFIRMED : AuditStatus.REJECTED)
            .build();
        
        auditLog.add(entry);
        
        log.info("[AUDIT-CONFIRMATION] {} | Confirmed: {} | By: {}",
            correlationId, confirmed, confirmedBy);
    }
    
    /**
     * Masque les données sensibles
     */
    private Map<String, Object> sanitizeParameters(Map<String, Object> params) {
        if (params == null) return Map.of();
        
        Map<String, Object> sanitized = new HashMap<>(params);
        List<String> sensitiveFields = List.of(
            "password", "creditCard", "ssn", "token", "secret"
        );
        
        for (String field : sensitiveFields) {
            if (sanitized.containsKey(field)) {
                sanitized.put(field, "***MASKED***");
            }
        }
        
        return sanitized;
    }
    
    private String generateCorrelationId() {
        return "MCP-" + System.currentTimeMillis() + "-" + 
            UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    // Statuts d'audit
    public enum AuditStatus {
        STARTED,
        COMPLETED,
        FAILED,
        ACCESS_DENIED,
        PENDING_CONFIRMATION,
        CONFIRMED,
        REJECTED
    }
    
    @Data
    @Builder
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
    }
}
```

---

## 6.5 Le workflow de confirmation

### Pourquoi confirmer ?

Les actions qui **modifient des données** doivent être confirmées explicitement pour éviter :
- Les erreurs de compréhension du LLM
- Les actions non désirées
- Les problèmes de responsabilité

### Séquence de confirmation

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  User   │    │   LLM   │    │   MCP   │    │  Audit  │
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │
     │ "Crée une    │              │              │
     │  commande"   │              │              │
     │─────────────►│              │              │
     │              │              │              │
     │              │ createOrder  │              │
     │              │ confirmed=   │              │
     │              │ false        │              │
     │              │─────────────►│              │
     │              │              │              │
     │              │              │ logConfirm   │
     │              │              │ Required     │
     │              │              │─────────────►│
     │              │              │              │
     │              │◄─────────────│              │
     │              │ REQUIRES_    │              │
     │              │ CONFIRMATION │              │
     │              │              │              │
     │◄─────────────│              │              │
     │ "Voulez-vous │              │              │
     │  confirmer   │              │              │
     │  cette       │              │              │
     │  commande?"  │              │              │
     │              │              │              │
     │ "Oui"        │              │              │
     │─────────────►│              │              │
     │              │              │              │
     │              │ createOrder  │              │
     │              │ confirmed=   │              │
     │              │ true         │              │
     │              │─────────────►│              │
     │              │              │              │
     │              │              │ logConfirm   │
     │              │              │ Received     │
     │              │              │─────────────►│
     │              │              │              │
     │              │              │ Création...  │
     │              │              │              │
     │              │◄─────────────│              │
     │              │  SUCCESS     │              │
     │              │              │              │
     │◄─────────────│              │              │
     │ "Commande    │              │              │
     │  créée!"     │              │              │
```

### Implémentation

```java
public McpResponse createOrder(CreateOrderRequest request, boolean confirmed) {
    McpCapability capability = McpCapability.CREATE_ORDER;
    
    // Vérifier les droits
    securityContext.requireCapability(capability);
    
    String correlationId = auditService.startCapabilityCall(
        securityContext, capability,
        Map.of("customerCode", request.getCustomerCode(), "confirmed", confirmed)
    );
    
    try {
        // Valider la demande
        OrderValidationResult validation = orderService.validateOrderRequest(request);
        
        if (!validation.isValid()) {
            return McpResponse.validationFailed(validation.toExplanation());
        }
        
        // Si pas confirmé → demander confirmation
        if (!confirmed) {
            String summary = request.toConfirmationSummary();
            
            auditService.logConfirmationRequired(correlationId, capability, summary);
            
            return McpResponse.requiresConfirmation(
                correlationId,
                summary + "\n\n⚠️ Confirmez-vous cette création?"
            );
        }
        
        // Confirmé → exécuter
        auditService.logConfirmationReceived(
            correlationId, true, securityContext.getUsername()
        );
        
        Order created = orderService.createOrder(
            request, securityContext.getUsername()
        );
        
        auditService.completeCapabilityCall(
            correlationId, capability, "Commande créée: " + created.getOrderNumber()
        );
        
        return McpResponse.success("✅ Commande " + created.getOrderNumber() + " créée!");
        
    } catch (Exception e) {
        auditService.failCapabilityCall(correlationId, capability, e.getMessage());
        return McpResponse.error("Erreur: " + e.getMessage());
    }
}
```

---

## 6.6 Bonnes pratiques entreprise

### 1. Logs structurés

```java
// ❌ Mauvais
log.info("Commande créée");

// ✅ Bon
log.info("[AUDIT-COMPLETE] {} | User: {} | Capability: {} | Result: {}",
    correlationId, username, capability, result);
```

### 2. Corrélation de bout en bout

```java
// Génération d'un ID de corrélation unique
String correlationId = "MCP-" + timestamp + "-" + randomSuffix;

// Utilisé dans tous les logs de la requête
startCapabilityCall(correlationId, ...);
completeCapabilityCall(correlationId, ...);
```

### 3. Masquage des données sensibles

```java
private Map<String, Object> sanitizeParameters(Map<String, Object> params) {
    List<String> sensitiveFields = List.of(
        "password", "creditCard", "ssn", "token"
    );
    // Remplacer par ***MASKED***
}
```

### 4. Rétention et archivage

```yaml
# En production
audit:
  retention:
    hot: 30d      # Elasticsearch/rapide
    warm: 90d     # S3/stockage froid
    archive: 7y   # Conformité légale
```

---

## 📝 Points clés à retenir

1. **RBAC obligatoire** : Chaque rôle a des capacités définies
2. **Contexte par requête** : `@RequestScope` pour isoler les utilisateurs
3. **Audit systématique** : START → COMPLETE/FAIL
4. **Confirmation explicite** : Pour toutes les actions d'écriture
5. **Logs structurés** : Avec corrélation de bout en bout

---

## 🎯 Quiz de validation

1. Pourquoi `McpSecurityContext` est-il `@RequestScope` ?
2. Quelles capacités nécessitent une confirmation et pourquoi ?
3. À quoi sert le `correlationId` dans l'audit ?
4. Pourquoi masquer certains paramètres dans les logs ?

---

[← Chapitre précédent](./05-implementation.md) | [Chapitre suivant →](./07-cas-concrets.md)
