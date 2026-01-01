# Chapitre 4 : Architecture détaillée

## 🎯 Objectifs du chapitre

- Comprendre l'architecture globale du système
- Identifier le rôle de chaque composant
- Maîtriser les flux de données

---

## 4.1 Vue d'ensemble de l'architecture

### Diagramme architectural complet

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UTILISATEURS                                    │
│    ┌──────────┐    ┌──────────┐    ┌──────────┐                            │
│    │ Support  │    │ Manager  │    │  Admin   │                            │
│    │  (RBAC)  │    │  (RBAC)  │    │  (RBAC)  │                            │
│    └────┬─────┘    └────┬─────┘    └────┬─────┘                            │
└─────────┼───────────────┼───────────────┼──────────────────────────────────┘
          │               │               │
          └───────────────┼───────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           COUCHE PRÉSENTATION                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        ChatController                                │    │
│  │                    (REST API + Auth Spring Security)                 │    │
│  └───────────────────────────────┬─────────────────────────────────────┘    │
└──────────────────────────────────┼──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              COUCHE MCP                                      │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    McpToolsConfiguration                             │    │
│  │              (Déclaration des Tools Spring AI)                       │    │
│  └───────────────────────────────┬─────────────────────────────────────┘    │
│                                  │                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    McpCapabilityHandler                              │    │
│  │    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │    │
│  │    │ findOrder   │  │analyzeInv.  │  │summarizeCust│  │createOrder │    │
│  │    └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬─────┘    │
│  └───────────┼────────────────┼────────────────┼───────────────┼────────┘    │
│              │                │                │               │             │
│  ┌───────────┴────────────────┴────────────────┴───────────────┴────────┐    │
│  │                       SÉCURITÉ & AUDIT                               │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐      │    │
│  │  │McpSecurityContext│ │  McpAuditService │  │McpAccessDenied │      │    │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘      │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          COUCHE APPLICATION                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │  OrderService   │  │ InvoiceService  │  │ CustomerService │             │
│  │                 │  │                 │  │                 │             │
│  │ • findByNumber  │  │ • analyzeInvoice│  │ • summarize     │             │
│  │ • createOrder   │  │ • findByNumber  │  │   Activity      │             │
│  │ • validateOrder │  │ • recordPayment │  │ • canPlaceOrder │             │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘             │
└───────────┼────────────────────┼────────────────────┼───────────────────────┘
            │                    │                    │
            └────────────────────┼────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            COUCHE DOMAINE                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │    Customer     │  │     Order       │  │    Invoice      │             │
│  │    (Entity)     │  │    (Entity)     │  │    (Entity)     │             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │   OrderLine     │  │  InvoiceLine    │  │    Enums        │             │
│  │    (Entity)     │  │    (Entity)     │  │(Status, Segment)│             │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘             │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         COUCHE INFRASTRUCTURE                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                     Spring Data JPA Repositories                     │    │
│  │  CustomerRepository  │  OrderRepository  │  InvoiceRepository       │    │
│  └───────────────────────────────┬─────────────────────────────────────┘    │
│                                  │                                           │
│  ┌───────────────────────────────▼─────────────────────────────────────┐    │
│  │                         H2 Database                                  │    │
│  │                     (In-memory pour démo)                            │    │
│  └──────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4.2 Flux d'un appel MCP

### Séquence détaillée : findOrder

```
┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐    ┌────────┐
│  User  │    │  Chat  │    │Security│    │Capability│   │ Order  │    │  Audit │
│        │    │Controller│  │Context │    │Handler  │   │Service │    │Service │
└───┬────┘    └───┬────┘    └───┬────┘    └───┬────┘    └───┬────┘    └───┬────┘
    │             │             │             │             │             │
    │ GET /test/  │             │             │             │             │
    │ find-order/ │             │             │             │             │
    │ CMD-123     │             │             │             │             │
    │────────────►│             │             │             │             │
    │             │             │             │             │             │
    │             │ 1. Init     │             │             │             │
    │             │    context  │             │             │             │
    │             │────────────►│             │             │             │
    │             │             │             │             │             │
    │             │ 2. Call     │             │             │             │
    │             │    findOrder│             │             │             │
    │             │─────────────────────────►│             │             │
    │             │             │             │             │             │
    │             │             │ 3. Check   │             │             │
    │             │             │◄───────────│             │             │
    │             │             │  capability│             │             │
    │             │             │────────────►│             │             │
    │             │             │     OK     │             │             │
    │             │             │             │             │             │
    │             │             │             │ 4. Start   │             │
    │             │             │             │    audit   │             │
    │             │             │             │────────────────────────►│
    │             │             │             │             │             │
    │             │             │             │ 5. Call    │             │
    │             │             │             │   service  │             │
    │             │             │             │────────────►│             │
    │             │             │             │             │             │
    │             │             │             │◄────────────│             │
    │             │             │             │   Order    │             │
    │             │             │             │             │             │
    │             │             │             │ 6. Format  │             │
    │             │             │             │   response │             │
    │             │             │             │             │             │
    │             │             │             │ 7. Complete│             │
    │             │             │             │    audit   │             │
    │             │             │             │────────────────────────►│
    │             │             │             │             │             │
    │             │◄─────────────────────────│             │             │
    │             │  McpResponse              │             │             │
    │             │                           │             │             │
    │◄────────────│                           │             │             │
    │ JSON Response                           │             │             │
    │             │             │             │             │             │
```

---

## 4.3 Structure des packages

```
src/main/java/com/enterprise/mcp/
│
├── McpEnterpriseApplication.java     # Point d'entrée Spring Boot
│
├── domain/                            # COUCHE DOMAINE
│   ├── entity/                        # Entités JPA
│   │   ├── Customer.java             
│   │   ├── Order.java                
│   │   ├── OrderLine.java            
│   │   ├── Invoice.java              
│   │   └── InvoiceLine.java          
│   │
│   └── repository/                    # Repositories Spring Data
│       ├── CustomerRepository.java   
│       ├── OrderRepository.java      
│       └── InvoiceRepository.java    
│
├── service/                           # COUCHE APPLICATION
│   ├── CustomerService.java          # Logique métier clients
│   ├── OrderService.java             # Logique métier commandes
│   ├── InvoiceService.java           # Logique métier factures
│   │
│   └── dto/                           # Objets de transfert
│       ├── CustomerActivitySummary.java
│       ├── InvoiceAnalysis.java
│       ├── CreateOrderRequest.java
│       └── OrderValidationResult.java
│
├── mcp/                               # COUCHE MCP
│   ├── McpCapabilityHandler.java     # Gestionnaire des capacités
│   ├── McpResponse.java              # Réponse standardisée
│   └── McpToolsConfiguration.java    # Configuration Spring AI Tools
│
├── security/                          # SÉCURITÉ MCP
│   ├── McpRole.java                  # Enum des rôles
│   ├── McpCapability.java            # Enum des capacités
│   ├── McpSecurityContext.java       # Contexte de sécurité
│   └── McpAccessDeniedException.java # Exception accès refusé
│
├── audit/                             # AUDIT
│   └── McpAuditService.java          # Service d'audit
│
├── config/                            # CONFIGURATION
│   ├── SecurityConfig.java           # Spring Security
│   └── DataInitializer.java          # Données de démo
│
└── controller/                        # COUCHE PRÉSENTATION
    └── ChatController.java           # API REST
```

---

## 4.4 Rôle de chaque composant

### McpCapabilityHandler

**Responsabilités :**
- Point d'entrée unique pour toutes les capacités MCP
- Vérification des droits d'accès
- Coordination de l'audit
- Formatage des réponses pour l'IA

```java
@Component
public class McpCapabilityHandler {
    
    public McpResponse findOrder(String orderNumber) {
        // 1. Vérifier les droits
        securityContext.requireCapability(McpCapability.FIND_ORDER);
        
        // 2. Démarrer l'audit
        String correlationId = auditService.startCapabilityCall(...);
        
        try {
            // 3. Appeler le service métier
            Optional<Order> order = orderService.findByOrderNumber(orderNumber);
            
            // 4. Formater la réponse
            String formatted = formatOrderForAI(order.get());
            
            // 5. Compléter l'audit
            auditService.completeCapabilityCall(correlationId, ...);
            
            return McpResponse.success(formatted);
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, ...);
            return McpResponse.error(e.getMessage());
        }
    }
}
```

### McpSecurityContext

**Responsabilités :**
- Maintenir le contexte utilisateur pour la requête
- Vérifier les permissions
- Fournir les infos pour l'audit

```java
@Component
@RequestScope  // Important : un contexte par requête HTTP
public class McpSecurityContext {
    
    private String userId;
    private McpRole role;
    
    public void requireCapability(McpCapability capability) {
        if (!role.hasCapability(capability)) {
            throw new McpAccessDeniedException("Accès refusé");
        }
    }
}
```

### McpAuditService

**Responsabilités :**
- Tracer tous les appels MCP
- Enregistrer les succès et échecs
- Gérer les confirmations
- Fournir des statistiques

```java
@Service
public class McpAuditService {
    
    public String startCapabilityCall(McpSecurityContext ctx, 
                                       McpCapability capability,
                                       Map<String, Object> params) {
        // Génère un ID de corrélation unique
        // Enregistre le début de l'appel
        // Log structuré pour monitoring
    }
    
    public void completeCapabilityCall(String correlationId, ...) {
        // Enregistre la fin avec succès
    }
    
    public void failCapabilityCall(String correlationId, ...) {
        // Enregistre l'échec avec l'erreur
    }
}
```

---

## 4.5 Les DTOs clés

### McpResponse

Structure standardisée pour toutes les réponses MCP :

```java
@Data
@Builder
public class McpResponse {
    private ResponseStatus status;    // SUCCESS, ERROR, REQUIRES_CONFIRMATION...
    private String content;           // Le texte pour l'IA
    private String correlationId;     // Pour le suivi
    private boolean requiresConfirmation;
    
    public String toAIFormat() {
        // Formate la réponse de manière lisible par l'IA
    }
}
```

### CustomerActivitySummary

Agrégation des données client pour l'IA :

```java
@Data
@Builder
public class CustomerActivitySummary {
    private Customer customer;
    private int totalOrders;
    private BigDecimal totalRevenue;
    private List<Order> recentOrders;
    private int totalInvoices;
    private BigDecimal totalOutstanding;
    private boolean hasOverdueInvoices;
    
    public String toNaturalLanguageSummary() {
        // Génère un texte structuré pour l'IA
    }
}
```

### InvoiceAnalysis

Analyse détaillée d'une facture :

```java
@Data
@Builder
public class InvoiceAnalysis {
    private Invoice invoice;
    private String riskLevel;         // AUCUN, FAIBLE, MOYEN, ÉLEVÉ, CRITIQUE
    private long daysOverdue;
    private List<String> recommendations;
    private BigDecimal customerTotalOutstanding;
    
    public String toNaturalLanguageReport() {
        // Génère un rapport d'analyse pour l'IA
    }
}
```

---

## 4.6 Intégration Spring AI

### Configuration des Tools

```java
@Configuration
public class McpToolsConfiguration {
    
    @Bean
    @Description("Recherche une commande par son numéro")
    public Function<FindOrderRequest, String> findOrder() {
        return request -> {
            McpResponse response = capabilityHandler.findOrder(
                request.orderNumber()
            );
            return response.toAIFormat();
        };
    }
    
    // Records pour les paramètres
    public record FindOrderRequest(
        @ToolParam(description = "Numéro de commande (ex: CMD-20240115-ABC)")
        String orderNumber
    ) {}
}
```

### Comment le LLM découvre les Tools

1. Spring AI expose automatiquement les beans `Function<Input, Output>`
2. L'annotation `@Description` fournit le contexte au LLM
3. `@ToolParam` décrit chaque paramètre
4. Le LLM choisit le bon tool en fonction de la question utilisateur

---

## 4.7 Patterns architecturaux utilisés

### 1. Adapter Pattern

Les capacités MCP sont des **adaptateurs** entre l'IA et les services métiers :

```
LLM ──► McpCapabilityHandler (Adapter) ──► OrderService
        └── Traduit les appels IA en appels métier
        └── Traduit les réponses métier en texte IA
```

### 2. Facade Pattern

`McpCapabilityHandler` est une **façade** qui simplifie l'accès aux services :

```
                    ┌── OrderService
McpCapabilityHandler ├── InvoiceService
                    └── CustomerService
```

### 3. Chain of Responsibility

Le flux de traitement forme une chaîne :

```
Request → Security → Audit → Business → Format → Audit → Response
```

### 4. Strategy Pattern

Les rôles définissent des stratégies d'accès différentes :

```java
enum McpRole {
    SUPPORT(Set.of(FIND_ORDER, ANALYZE_INVOICE)),
    MANAGER(Set.of(FIND_ORDER, ANALYZE_INVOICE, CREATE_ORDER)),
    ADMIN(Set.of(McpCapability.values()));
}
```

---

## 📝 Points clés à retenir

1. **Architecture en couches** : Présentation → MCP → Application → Domaine → Infrastructure
2. **Séparation des responsabilités** : Chaque composant a un rôle précis
3. **MCP = Adapter + Facade** : Traduit et simplifie l'accès aux services
4. **Audit systématique** : Chaque appel est tracé de bout en bout
5. **DTOs spécialisés** : Formatage adapté pour l'IA

---

## 🎯 Quiz de validation

1. Quel composant vérifie les droits d'accès aux capacités ?
2. Pourquoi `McpSecurityContext` est-il `@RequestScope` ?
3. Quel design pattern représente `McpCapabilityHandler` ?
4. Comment le LLM découvre-t-il les capacités disponibles ?

---

[← Chapitre précédent](./03-cas-usage.md) | [Chapitre suivant →](./05-implementation.md)
