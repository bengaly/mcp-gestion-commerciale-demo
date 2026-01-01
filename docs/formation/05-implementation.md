# Chapitre 5 : Implémentation Spring Boot

## 🎯 Objectifs du chapitre

- Maîtriser l'implémentation concrète du projet
- Comprendre chaque composant en détail
- Savoir reproduire le projet from scratch

---

## 5.1 Configuration du projet

### pom.xml - Dépendances clés

```xml
<!-- Spring AI - MCP Server -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>

<!-- Spring AI - OpenAI (pour le LLM) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>${spring-ai.version}</version>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  application:
    name: mcp-enterprise-demo
  
  datasource:
    url: jdbc:h2:mem:enterprisedb
    driver-class-name: org.h2.Driver

# Configuration Spring AI
spring.ai:
  openai:
    api-key: ${OPENAI_API_KEY:your-api-key}
    chat:
      options:
        model: gpt-4
        temperature: 0.7

# Audit
audit:
  enabled: true
  log-level: INFO
```

---

## 5.2 Les entités du domaine

### Customer.java

```java
@Entity
@Table(name = "customers")
@Data
@Builder
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String customerCode;  // CLI-001
    
    @Column(nullable = false)
    private String companyName;
    
    private String contactName;
    private String email;
    private String phone;
    private String address;
    
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;  // ACTIVE, INACTIVE, SUSPENDED
    
    @Enumerated(EnumType.STRING)
    private CustomerSegment segment;  // STANDARD, PREMIUM, VIP, ENTERPRISE
    
    private Double creditLimit;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Points clés :**
- `customerCode` : Identifiant métier unique (différent de l'ID technique)
- `segment` : Permet d'adapter le traitement (ex: VIP = prioritaire)
- `creditLimit` : Utilisé pour valider les commandes

### Order.java

```java
@Entity
@Table(name = "orders")
@Data
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String orderNumber;  // CMD-20240115-ABC123
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderLine> lines = new ArrayList<>();
    
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryDate;
    
    private String createdBy;  // Traçabilité
    
    // Méthode métier
    public void calculateTotals() {
        this.totalAmount = lines.stream()
            .map(OrderLine::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.taxAmount = totalAmount.multiply(new BigDecimal("0.20"));
    }
}
```

**Points clés :**
- `orderNumber` : Généré automatiquement avec un format lisible
- `createdBy` : Trace qui a créé la commande (important pour l'audit)
- `calculateTotals()` : Logique métier dans l'entité

### Invoice.java

```java
@Entity
@Table(name = "invoices")
@Data
@Builder
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;  // FAC-2024-000123
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;  // Peut être null (facture manuelle)
    
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;
    
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate paidDate;
    
    // Méthodes métier
    public boolean isOverdue() {
        return status != InvoiceStatus.PAID 
            && dueDate != null 
            && LocalDate.now().isAfter(dueDate);
    }
    
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }
}
```

---

## 5.3 Les services métiers

### OrderService.java - Création de commande

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    
    /**
     * Valide une demande de création AVANT de créer
     * Crucial pour le workflow MCP avec confirmation
     */
    public OrderValidationResult validateOrderRequest(CreateOrderRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 1. Vérifier le client
        Optional<Customer> customerOpt = customerRepository
            .findByCustomerCode(request.getCustomerCode());
        
        if (customerOpt.isEmpty()) {
            errors.add("Client non trouvé: " + request.getCustomerCode());
            return OrderValidationResult.invalid(errors);
        }
        
        Customer customer = customerOpt.get();
        
        // 2. Vérifier le statut client
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            errors.add("Client non actif: " + customer.getStatus());
        }
        
        // 3. Vérifier les lignes
        if (request.getLines() == null || request.getLines().isEmpty()) {
            errors.add("Au moins une ligne requise");
        }
        
        // 4. Calculer le total estimé
        BigDecimal estimatedTotal = calculateEstimatedTotal(request);
        
        // 5. Vérifier la limite de crédit
        if (!customerService.canPlaceOrder(request.getCustomerCode(), estimatedTotal)) {
            errors.add("Dépassement limite de crédit");
        }
        
        // 6. Avertissements
        if (estimatedTotal.compareTo(new BigDecimal("10000")) > 0) {
            warnings.add("Commande > 10 000€ - validation managériale recommandée");
        }
        
        if (errors.isEmpty()) {
            return OrderValidationResult.valid(warnings, estimatedTotal);
        }
        return OrderValidationResult.invalid(errors);
    }
    
    /**
     * Crée la commande après validation ET confirmation
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request, String createdBy) {
        // Re-valider par sécurité
        OrderValidationResult validation = validateOrderRequest(request);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Commande invalide");
        }
        
        Customer customer = customerRepository
            .findByCustomerCode(request.getCustomerCode())
            .orElseThrow();
        
        // Créer la commande
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .customer(customer)
            .status(OrderStatus.PENDING_VALIDATION)
            .createdBy(createdBy)
            .build();
        
        // Ajouter les lignes
        for (var lineRequest : request.getLines()) {
            OrderLine line = OrderLine.builder()
                .productCode(lineRequest.getProductCode())
                .productName(lineRequest.getProductName())
                .quantity(lineRequest.getQuantity())
                .unitPrice(lineRequest.getUnitPrice())
                .build();
            order.addLine(line);
        }
        
        order.calculateTotals();
        
        return orderRepository.save(order);
    }
    
    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = UUID.randomUUID().toString()
            .substring(0, 8).toUpperCase();
        return "CMD-" + datePrefix + "-" + suffix;
    }
}
```

### InvoiceService.java - Analyse de facture

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Analyse détaillée d'une facture pour l'IA
     */
    public Optional<InvoiceAnalysis> analyzeInvoice(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
            .map(invoice -> {
                InvoiceAnalysis.InvoiceAnalysisBuilder builder = InvoiceAnalysis.builder()
                    .invoice(invoice)
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .customerName(invoice.getCustomer().getCompanyName())
                    .status(invoice.getStatus().name())
                    .totalAmount(invoice.getTotalAmount())
                    .paidAmount(invoice.getPaidAmount())
                    .remainingAmount(invoice.getRemainingAmount())
                    .isOverdue(invoice.isOverdue())
                    .daysOverdue(invoice.getDaysOverdue());
                
                // Calcul du pourcentage payé
                BigDecimal paidPercent = invoice.getPaidAmount()
                    .divide(invoice.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                builder.paidPercentage(paidPercent);
                
                // Évaluation du risque
                builder.riskLevel(evaluateRiskLevel(invoice));
                
                // Recommandations
                builder.recommendations(generateRecommendations(invoice));
                
                return builder.build();
            });
    }
    
    private String evaluateRiskLevel(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return "AUCUN";
        }
        
        if (invoice.isOverdue()) {
            long days = invoice.getDaysOverdue();
            if (days > 90) return "CRITIQUE";
            if (days > 60) return "ÉLEVÉ";
            if (days > 30) return "MOYEN";
            return "FAIBLE";
        }
        
        return "NORMAL";
    }
    
    private List<String> generateRecommendations(Invoice invoice) {
        List<String> recommendations = new ArrayList<>();
        
        if (invoice.isOverdue()) {
            long days = invoice.getDaysOverdue();
            if (days <= 15) {
                recommendations.add("Envoyer une première relance");
            } else if (days <= 30) {
                recommendations.add("Deuxième relance + appel téléphonique");
            } else if (days <= 60) {
                recommendations.add("Escalader au service recouvrement");
            } else {
                recommendations.add("Transférer au contentieux");
            }
        }
        
        return recommendations;
    }
}
```

### CustomerService.java - Résumé d'activité

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Génère un résumé complet de l'activité client
     */
    public Optional<CustomerActivitySummary> summarizeActivity(String customerCode) {
        return customerRepository.findByCustomerCode(customerCode)
            .map(customer -> {
                Long customerId = customer.getId();
                
                // Commandes
                List<Order> recentOrders = orderRepository
                    .findRecentOrdersByCustomer(customerId);
                Long totalOrders = orderRepository
                    .countOrdersByCustomer(customerId);
                Double totalRevenue = orderRepository
                    .getTotalRevenueByCustomer(customerId);
                
                // Factures
                List<Invoice> recentInvoices = invoiceRepository
                    .findRecentInvoicesByCustomer(customerId);
                List<Invoice> unpaidInvoices = invoiceRepository
                    .findUnpaidInvoicesByCustomer(customerId);
                Double totalPaid = invoiceRepository
                    .getTotalPaidByCustomer(customerId);
                Double totalOutstanding = invoiceRepository
                    .getTotalOutstandingByCustomer(customerId);
                
                return CustomerActivitySummary.builder()
                    .customer(customer)
                    .totalOrders(totalOrders.intValue())
                    .totalRevenue(BigDecimal.valueOf(totalRevenue != null ? totalRevenue : 0))
                    .recentOrders(recentOrders.stream().limit(5).toList())
                    .totalPaid(BigDecimal.valueOf(totalPaid != null ? totalPaid : 0))
                    .totalOutstanding(BigDecimal.valueOf(totalOutstanding != null ? totalOutstanding : 0))
                    .unpaidInvoicesCount(unpaidInvoices.size())
                    .recentInvoices(recentInvoices.stream().limit(5).toList())
                    .hasOverdueInvoices(unpaidInvoices.stream().anyMatch(Invoice::isOverdue))
                    .generatedAt(LocalDateTime.now())
                    .build();
            });
    }
    
    /**
     * Vérifie si un client peut passer une commande
     */
    public boolean canPlaceOrder(String customerCode, BigDecimal orderAmount) {
        return customerRepository.findByCustomerCode(customerCode)
            .map(customer -> {
                // Client actif ?
                if (customer.getStatus() != CustomerStatus.ACTIVE) {
                    return false;
                }
                
                // Limite de crédit ?
                Double outstanding = invoiceRepository
                    .getTotalOutstandingByCustomer(customer.getId());
                
                if (outstanding != null && customer.getCreditLimit() != null) {
                    BigDecimal totalExposure = BigDecimal.valueOf(outstanding)
                        .add(orderAmount);
                    return totalExposure.compareTo(
                        BigDecimal.valueOf(customer.getCreditLimit())) <= 0;
                }
                
                return true;
            })
            .orElse(false);
    }
}
```

---

## 5.4 Le handler MCP

### McpCapabilityHandler.java

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class McpCapabilityHandler {
    
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final McpAuditService auditService;
    private final McpSecurityContext securityContext;
    
    /**
     * Capacité : findOrder
     */
    public McpResponse findOrder(String orderNumber) {
        McpCapability capability = McpCapability.FIND_ORDER;
        
        // 1. Vérifier les droits
        securityContext.requireCapability(capability);
        
        // 2. Audit start
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability, 
            Map.of("orderNumber", orderNumber)
        );
        
        try {
            // 3. Appeler le service
            Optional<Order> orderOpt = orderService.findByOrderNumber(orderNumber);
            
            if (orderOpt.isEmpty()) {
                String msg = "Commande non trouvée: " + orderNumber;
                auditService.completeCapabilityCall(correlationId, capability, msg);
                return McpResponse.notFound(msg);
            }
            
            // 4. Formater pour l'IA
            String formatted = formatOrderForAI(orderOpt.get());
            
            // 5. Audit complete
            auditService.completeCapabilityCall(
                correlationId, capability, "Commande trouvée: " + orderNumber
            );
            
            return McpResponse.success(formatted);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Capacité : createOrder (avec confirmation)
     */
    public McpResponse createOrder(CreateOrderRequest request, boolean confirmed) {
        McpCapability capability = McpCapability.CREATE_ORDER;
        
        securityContext.requireCapability(capability);
        
        String correlationId = auditService.startCapabilityCall(
            securityContext, capability,
            Map.of("customerCode", request.getCustomerCode(), "confirmed", confirmed)
        );
        
        try {
            // Validation
            OrderValidationResult validation = orderService.validateOrderRequest(request);
            
            if (!validation.isValid()) {
                auditService.completeCapabilityCall(
                    correlationId, capability, "Validation échouée"
                );
                return McpResponse.validationFailed(validation.toExplanation());
            }
            
            // Si pas confirmé, demander confirmation
            if (!confirmed) {
                String summary = request.toConfirmationSummary();
                auditService.logConfirmationRequired(correlationId, capability, summary);
                
                return McpResponse.requiresConfirmation(
                    correlationId,
                    summary + "\n\nConfirmez-vous cette création?"
                );
            }
            
            // Création après confirmation
            auditService.logConfirmationReceived(
                correlationId, true, securityContext.getUsername()
            );
            
            Order created = orderService.createOrder(
                request, securityContext.getUsername()
            );
            
            String successMsg = String.format(
                "✅ Commande %s créée avec succès!\nMontant: %,.2f €",
                created.getOrderNumber(), created.getTotalAmount()
            );
            
            auditService.completeCapabilityCall(
                correlationId, capability, "Commande créée: " + created.getOrderNumber()
            );
            
            return McpResponse.success(successMsg);
            
        } catch (Exception e) {
            auditService.failCapabilityCall(correlationId, capability, e.getMessage());
            return McpResponse.error("Erreur: " + e.getMessage());
        }
    }
    
    private String formatOrderForAI(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Commande ").append(order.getOrderNumber()).append(" ===\n\n");
        sb.append("Client: ").append(order.getCustomer().getCompanyName()).append("\n");
        sb.append("Statut: ").append(order.getStatus()).append("\n");
        sb.append("Date: ").append(order.getOrderDate().toLocalDate()).append("\n\n");
        
        sb.append("Lignes:\n");
        order.getLines().forEach(line -> {
            sb.append("- ").append(line.getProductName())
              .append(" x ").append(line.getQuantity())
              .append(" = ").append(String.format("%,.2f €", line.getLineTotal()))
              .append("\n");
        });
        
        sb.append("\nTotal HT: ").append(String.format("%,.2f €", order.getTotalAmount()));
        return sb.toString();
    }
}
```

---

## 5.5 Configuration des Tools Spring AI

### McpToolsConfiguration.java

```java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class McpToolsConfiguration {
    
    private final McpCapabilityHandler capabilityHandler;
    
    @Bean
    @Description("Recherche une commande par son numéro. " +
                 "Retourne les détails complets de la commande.")
    public Function<FindOrderRequest, String> findOrder() {
        return request -> {
            log.info("Tool findOrder appelé: {}", request.orderNumber());
            McpResponse response = capabilityHandler.findOrder(request.orderNumber());
            return response.toAIFormat();
        };
    }
    
    @Bean
    @Description("Analyse une facture en profondeur. " +
                 "Retourne le statut, les risques et les recommandations.")
    public Function<AnalyzeInvoiceRequest, String> analyzeInvoice() {
        return request -> {
            log.info("Tool analyzeInvoice appelé: {}", request.invoiceNumber());
            McpResponse response = capabilityHandler.analyzeInvoice(request.invoiceNumber());
            return response.toAIFormat();
        };
    }
    
    @Bean
    @Description("Génère un résumé complet de l'activité d'un client.")
    public Function<SummarizeCustomerRequest, String> summarizeCustomerActivity() {
        return request -> {
            log.info("Tool summarizeCustomer appelé: {}", request.customerCode());
            McpResponse response = capabilityHandler.summarizeCustomerActivity(
                request.customerCode()
            );
            return response.toAIFormat();
        };
    }
    
    @Bean
    @Description("Crée une nouvelle commande. Nécessite confirmation.")
    public Function<CreateOrderToolRequest, String> createOrder() {
        return request -> {
            log.info("Tool createOrder appelé pour: {}", request.customerCode());
            CreateOrderRequest serviceRequest = convertToServiceRequest(request);
            McpResponse response = capabilityHandler.createOrder(
                serviceRequest, request.confirmed()
            );
            return response.toAIFormat();
        };
    }
    
    // Records pour les paramètres des Tools
    public record FindOrderRequest(
        @ToolParam(description = "Numéro de commande (ex: CMD-20240115-ABC)")
        String orderNumber
    ) {}
    
    public record AnalyzeInvoiceRequest(
        @ToolParam(description = "Numéro de facture (ex: FAC-2024-000123)")
        String invoiceNumber
    ) {}
    
    public record SummarizeCustomerRequest(
        @ToolParam(description = "Code client (ex: CLI-001)")
        String customerCode
    ) {}
    
    public record CreateOrderToolRequest(
        @ToolParam(description = "Code client")
        String customerCode,
        @ToolParam(description = "Lignes de commande")
        List<OrderLineToolRequest> lines,
        @ToolParam(description = "Adresse livraison (optionnel)")
        String shippingAddress,
        @ToolParam(description = "True si confirmé par l'utilisateur")
        boolean confirmed
    ) {}
}
```

---

## 📝 Points clés à retenir

1. **Séparation claire** : Service métier ≠ Handler MCP ≠ Configuration Tools
2. **Validation systématique** : Toujours valider avant d'exécuter
3. **Formatage pour l'IA** : Les réponses doivent être lisibles par le LLM
4. **Annotations Spring AI** : `@Description` et `@ToolParam` guident le LLM
5. **Audit intégré** : Chaque étape est tracée

---

## 🎯 Quiz de validation

1. Pourquoi séparer validation et création dans `OrderService` ?
2. Quel est le rôle de `@Description` sur un Bean Function ?
3. Pourquoi le handler fait-il un `requireCapability` ?
4. Comment le LLM sait-il quels paramètres passer au tool ?

---

[← Chapitre précédent](./04-architecture.md) | [Chapitre suivant →](./06-securite-audit.md)
