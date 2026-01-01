# Chapitre 8 : Erreurs courantes et anti-patterns

## 🎯 Objectifs du chapitre

- Identifier les erreurs fréquentes lors de l'implémentation MCP
- Comprendre pourquoi ce sont des erreurs
- Savoir les éviter et les corriger

---

## 8.1 Anti-pattern 1 : Exposer trop de capacités

### ❌ Le problème

```java
// MAUVAIS : Exposer TOUT le service comme capacité
@Bean
@Description("Accès complet au service client")
public Function<CustomerServiceRequest, String> customerService() {
    return request -> {
        switch (request.action()) {
            case "find" -> customerService.findById(request.id());
            case "create" -> customerService.create(request.data());
            case "update" -> customerService.update(request.data());
            case "delete" -> customerService.delete(request.id());
            case "listAll" -> customerService.findAll();
            // ... 50 autres actions
        }
    };
}
```

### Pourquoi c'est un problème

1. **Surface d'attaque énorme** : L'IA peut potentiellement tout faire
2. **Difficile à sécuriser** : Comment définir les rôles pour 50 actions ?
3. **Confusion pour le LLM** : Trop de choix = mauvaises décisions
4. **Audit imprécis** : "customerService" ne dit rien sur l'action réelle

### ✅ La solution

```java
// BON : Une capacité = une action métier précise
@Bean
@Description("Recherche un client par son code")
public Function<FindCustomerRequest, String> findCustomer() { ... }

@Bean
@Description("Génère un résumé de l'activité client")
public Function<SummarizeCustomerRequest, String> summarizeCustomerActivity() { ... }

// Pas de capacité pour delete, update, listAll si non nécessaire
```

### Règle d'or

> **Exposez le MINIMUM de capacités nécessaires au cas d'usage**

---

## 8.2 Anti-pattern 2 : Accès direct à la base de données

### ❌ Le problème

```java
// CATASTROPHIQUE : Laisser l'IA exécuter du SQL
@Bean
@Description("Exécute une requête SQL sur la base de données")
public Function<SqlQueryRequest, String> executeSql() {
    return request -> {
        String sql = request.query();  // 💀 SQL venant de l'IA
        return jdbcTemplate.queryForList(sql).toString();
    };
}
```

### Pourquoi c'est un problème

1. **Injection SQL** : L'IA peut générer des requêtes malveillantes
2. **Données sensibles exposées** : `SELECT * FROM users`
3. **Pas de validation métier** : Les règles business sont contournées
4. **Pas d'audit précis** : Impossible de savoir quelles données ont été accédées
5. **Destruction possible** : `DROP TABLE orders`

### ✅ La solution

```java
// BON : Capacités métier précises avec validation
@Bean
@Description("Recherche une commande par son numéro")
public Function<FindOrderRequest, String> findOrder() {
    return request -> {
        // Validation du format
        if (!isValidOrderNumber(request.orderNumber())) {
            return "Format de numéro invalide";
        }
        
        // Appel au service métier (qui gère la sécurité)
        return orderService.findByOrderNumber(request.orderNumber())
            .map(this::formatForAI)
            .orElse("Commande non trouvée");
    };
}
```

### Règle d'or

> **L'IA ne doit JAMAIS avoir accès à la base de données directement**

---

## 8.3 Anti-pattern 3 : Pas de validation des entrées

### ❌ Le problème

```java
// MAUVAIS : Faire confiance aux données de l'IA
public McpResponse createOrder(CreateOrderRequest request) {
    // Pas de validation !
    Order order = Order.builder()
        .customerCode(request.getCustomerCode())  // Peut être null
        .build();
    
    for (var line : request.getLines()) {  // Peut être null
        order.addLine(OrderLine.builder()
            .quantity(line.getQuantity())  // Peut être négatif
            .unitPrice(line.getUnitPrice())  // Peut être null
            .build());
    }
    
    return McpResponse.success("Commande créée");
}
```

### Pourquoi c'est un problème

1. **NullPointerException** : Données manquantes
2. **Données incohérentes** : Quantités négatives, prix à 0
3. **Violations des règles métier** : Client inexistant, limite crédit dépassée
4. **Corruption de données** : Entrées invalides en base

### ✅ La solution

```java
// BON : Validation systématique AVANT toute action
public McpResponse createOrder(CreateOrderRequest request, boolean confirmed) {
    // 1. Validation structurelle
    OrderValidationResult validation = orderService.validateOrderRequest(request);
    
    if (!validation.isValid()) {
        return McpResponse.validationFailed(
            "Validation échouée:\n" + 
            String.join("\n", validation.getErrors())
        );
    }
    
    // 2. Confirmation si action sensible
    if (!confirmed) {
        return McpResponse.requiresConfirmation(
            correlationId,
            request.toConfirmationSummary()
        );
    }
    
    // 3. Création seulement après validation ET confirmation
    Order created = orderService.createOrder(request, username);
    return McpResponse.success("Commande " + created.getOrderNumber() + " créée");
}
```

### Règle d'or

> **Validez TOUJOURS les données côté serveur, jamais confiance au client (IA incluse)**

---

## 8.4 Anti-pattern 4 : Pas d'audit

### ❌ Le problème

```java
// MAUVAIS : Aucune trace de ce que fait l'IA
public McpResponse findOrder(String orderNumber) {
    return orderService.findByOrderNumber(orderNumber)
        .map(order -> McpResponse.success(formatOrder(order)))
        .orElse(McpResponse.notFound("Non trouvé"));
    // Qui a appelé ? Quand ? Avec quoi ? Aucune idée !
}
```

### Pourquoi c'est un problème

1. **Pas de traçabilité** : Impossible de savoir qui a fait quoi
2. **Non-conformité** : RGPD, SOX exigent des traces
3. **Debugging impossible** : Erreur en production = mystère
4. **Détection d'abus impossible** : Comportements anormaux invisibles

### ✅ La solution

```java
// BON : Audit systématique de chaque appel
public McpResponse findOrder(String orderNumber) {
    McpCapability capability = McpCapability.FIND_ORDER;
    
    // 1. Audit START
    String correlationId = auditService.startCapabilityCall(
        securityContext, 
        capability, 
        Map.of("orderNumber", orderNumber)
    );
    
    try {
        Optional<Order> order = orderService.findByOrderNumber(orderNumber);
        
        if (order.isEmpty()) {
            // 2a. Audit COMPLETE (not found)
            auditService.completeCapabilityCall(
                correlationId, capability, "Non trouvé: " + orderNumber
            );
            return McpResponse.notFound("Non trouvé");
        }
        
        // 2b. Audit COMPLETE (success)
        auditService.completeCapabilityCall(
            correlationId, capability, "Trouvé: " + orderNumber
        );
        return McpResponse.success(formatOrder(order.get()));
        
    } catch (Exception e) {
        // 2c. Audit FAILED
        auditService.failCapabilityCall(correlationId, capability, e.getMessage());
        return McpResponse.error("Erreur: " + e.getMessage());
    }
}
```

### Règle d'or

> **Auditez CHAQUE appel MCP : qui, quoi, quand, résultat**

---

## 8.5 Anti-pattern 5 : Confondre MCP et API publique

### ❌ Le problème

```java
// MAUVAIS : Exposer MCP comme une API REST publique
@RestController
@RequestMapping("/api/mcp")  // 💀 Accessible depuis Internet
public class McpPublicController {
    
    @PostMapping("/tools/call")
    public ResponseEntity<String> callTool(@RequestBody ToolCallRequest request) {
        // N'importe qui peut appeler les capacités !
        return capabilityHandler.call(request.tool(), request.params());
    }
}
```

### Pourquoi c'est un problème

1. **Pas d'authentification** : N'importe qui peut appeler
2. **Contournement du LLM** : Appels directs sans contexte
3. **Pas de rate limiting** : Attaques par saturation
4. **Exposition des capacités** : Inventaire des fonctions disponibles

### ✅ La solution

```java
// BON : MCP accessible uniquement via le LLM authentifié
@RestController
@RequestMapping("/api/chat")  // API de chat, pas MCP directement
public class ChatController {
    
    private final ChatClient chatClient;  // Spring AI
    
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            Authentication authentication) {  // Authentification obligatoire
        
        // Le LLM décide quels tools appeler
        String response = chatClient.prompt()
            .user(request.message())
            .call()
            .content();
        
        return ResponseEntity.ok(new ChatResponse(response));
    }
}
```

### Règle d'or

> **MCP est interne au SI, jamais exposé directement sur Internet**

---

## 8.6 Anti-pattern 6 : Réponses non formatées pour l'IA

### ❌ Le problème

```java
// MAUVAIS : Retourner du JSON brut
public McpResponse findOrder(String orderNumber) {
    Order order = orderService.findByOrderNumber(orderNumber).orElseThrow();
    
    // L'IA reçoit du JSON technique
    return McpResponse.success(objectMapper.writeValueAsString(order));
}

// Résultat pour l'IA :
// {"id":123,"orderNumber":"CMD-123","status":"DELIVERED","totalAmount":1500.00,
//  "customer":{"id":456,"customerCode":"CLI-001"...}}
```

### Pourquoi c'est un problème

1. **Difficile à interpréter** : Le LLM doit parser du JSON
2. **Réponse utilisateur pauvre** : L'IA recopie le JSON
3. **Pas de contexte métier** : Juste des données brutes
4. **Informations manquantes** : Relations non résolues

### ✅ La solution

```java
// BON : Formater pour une lecture naturelle
public McpResponse findOrder(String orderNumber) {
    Order order = orderService.findByOrderNumber(orderNumber).orElseThrow();
    
    // Formatage en langage naturel structuré
    String formatted = formatOrderForAI(order);
    return McpResponse.success(formatted);
}

private String formatOrderForAI(Order order) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("=== Commande ").append(order.getOrderNumber()).append(" ===\n\n");
    sb.append("**Client:** ").append(order.getCustomer().getCompanyName()).append("\n");
    sb.append("**Statut:** ").append(order.getStatus()).append("\n");
    sb.append("**Date:** ").append(order.getOrderDate().toLocalDate()).append("\n\n");
    
    sb.append("**Lignes de commande:**\n");
    order.getLines().forEach(line -> {
        sb.append("- ").append(line.getProductName())
          .append(" x ").append(line.getQuantity())
          .append(" = ").append(String.format("%,.2f €", line.getLineTotal()))
          .append("\n");
    });
    
    sb.append("\n**Total HT:** ").append(String.format("%,.2f €", order.getTotalAmount()));
    
    return sb.toString();
}
```

### Règle d'or

> **Formatez les réponses pour qu'elles soient directement utilisables par l'IA**

---

## 8.7 Anti-pattern 7 : Actions sans confirmation

### ❌ Le problème

```java
// DANGEREUX : Créer sans confirmation
public McpResponse createOrder(CreateOrderRequest request) {
    // Validation OK
    if (orderService.validateOrderRequest(request).isValid()) {
        // Création IMMEDIATE sans demander confirmation
        Order order = orderService.createOrder(request, username);
        return McpResponse.success("Commande créée: " + order.getOrderNumber());
    }
    return McpResponse.error("Validation échouée");
}
```

### Pourquoi c'est un problème

1. **Erreur d'interprétation** : Le LLM a mal compris la demande
2. **Pas de vérification humaine** : L'utilisateur n'a pas validé les détails
3. **Irréversible** : La commande est créée, difficile à annuler
4. **Responsabilité floue** : Qui a validé cette action ?

### ✅ La solution

```java
// BON : Workflow en deux étapes
public McpResponse createOrder(CreateOrderRequest request, boolean confirmed) {
    
    // Validation
    OrderValidationResult validation = orderService.validateOrderRequest(request);
    if (!validation.isValid()) {
        return McpResponse.validationFailed(validation.toExplanation());
    }
    
    // Étape 1 : Demander confirmation
    if (!confirmed) {
        String summary = request.toConfirmationSummary();
        auditService.logConfirmationRequired(correlationId, capability, summary);
        
        return McpResponse.requiresConfirmation(
            correlationId,
            summary + "\n\n⚠️ Confirmez-vous cette création?"
        );
    }
    
    // Étape 2 : Création après confirmation explicite
    auditService.logConfirmationReceived(correlationId, true, username);
    Order order = orderService.createOrder(request, username);
    
    return McpResponse.success("✅ Commande " + order.getOrderNumber() + " créée");
}
```

### Règle d'or

> **Toute action qui modifie des données DOIT être confirmée explicitement**

---

## 8.8 Checklist anti-patterns

Avant de déployer votre MCP Server, vérifiez :

```
┌─────────────────────────────────────────────────────────────────┐
│                    CHECKLIST MCP ENTREPRISE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ □ Capacités minimales (pas d'accès "tout-en-un")                │
│ □ Pas d'accès direct à la base de données                       │
│ □ Validation côté serveur systématique                          │
│ □ Audit de chaque appel (start/complete/fail)                   │
│ □ MCP non exposé publiquement                                   │
│ □ Réponses formatées pour l'IA                                  │
│ □ Confirmation pour les actions d'écriture                      │
│ □ RBAC configuré avec moindre privilège                         │
│ □ Logs structurés avec corrélation                              │
│ □ Données sensibles masquées dans les logs                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 Points clés à retenir

1. **Moindre privilège** : Exposer le minimum de capacités
2. **Isolation** : L'IA n'accède jamais à la DB directement
3. **Validation** : Toujours côté serveur
4. **Audit** : Tout est tracé
5. **Confirmation** : Pour les actions qui modifient
6. **Formatage** : Réponses lisibles par l'IA

---

## 🎯 Quiz de validation

1. Pourquoi est-il dangereux d'exposer une capacité "executeSql" ?
2. Quelle est la différence entre une API publique et MCP ?
3. Pourquoi formater les réponses en texte plutôt qu'en JSON ?
4. Quand faut-il demander confirmation à l'utilisateur ?

---

[← Chapitre précédent](./07-cas-concrets.md) | [Chapitre suivant →](./09-carriere.md)
