# Chapitre 10 : Exercices pratiques

## 🎯 Objectifs du chapitre

- Mettre en pratique les concepts appris
- Étendre le projet existant
- Consolider les compétences MCP

---

## 10.1 Exercice 1 : Ajouter une capacité MCP

### Objectif

Ajouter une nouvelle capacité `listOverdueInvoices` qui retourne la liste des factures en retard.

### Spécifications

| Élément | Valeur |
|---------|--------|
| Nom | `listOverdueInvoices` |
| Description | Liste toutes les factures en retard de paiement |
| Paramètres | Aucun |
| Rôles autorisés | SUPPORT, MANAGER, ADMIN |
| Confirmation | Non |

### Étapes à suivre

1. **Ajouter la capacité dans l'enum**

```java
// McpCapability.java
LIST_OVERDUE_INVOICES("listOverdueInvoices", 
    "Liste les factures en retard de paiement", false),
```

2. **Ajouter aux rôles appropriés**

```java
// McpRole.java - Ajouter à SUPPORT, MANAGER, ADMIN
SUPPORT(Set.of(
    McpCapability.FIND_ORDER,
    McpCapability.ANALYZE_INVOICE,
    McpCapability.SUMMARIZE_CUSTOMER_ACTIVITY,
    McpCapability.LIST_OVERDUE_INVOICES  // Nouveau
)),
```

3. **Implémenter dans le handler**

```java
// McpCapabilityHandler.java
public McpResponse listOverdueInvoices() {
    McpCapability capability = McpCapability.LIST_OVERDUE_INVOICES;
    
    securityContext.requireCapability(capability);
    
    String correlationId = auditService.startCapabilityCall(
        securityContext, capability, Map.of()
    );
    
    try {
        List<Invoice> overdueInvoices = invoiceService.findOverdueInvoices();
        
        if (overdueInvoices.isEmpty()) {
            auditService.completeCapabilityCall(correlationId, capability, 
                "Aucune facture en retard");
            return McpResponse.success("✅ Aucune facture en retard actuellement.");
        }
        
        String formatted = formatOverdueInvoicesForAI(overdueInvoices);
        auditService.completeCapabilityCall(correlationId, capability, 
            overdueInvoices.size() + " factures en retard");
        
        return McpResponse.success(formatted);
        
    } catch (Exception e) {
        auditService.failCapabilityCall(correlationId, capability, e.getMessage());
        return McpResponse.error("Erreur: " + e.getMessage());
    }
}

private String formatOverdueInvoicesForAI(List<Invoice> invoices) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Factures en retard (").append(invoices.size()).append(") ===\n\n");
    
    BigDecimal totalOverdue = BigDecimal.ZERO;
    
    for (Invoice inv : invoices) {
        sb.append("📌 **").append(inv.getInvoiceNumber()).append("**\n");
        sb.append("   Client: ").append(inv.getCustomer().getCompanyName()).append("\n");
        sb.append("   Montant: ").append(String.format("%,.2f €", inv.getRemainingAmount())).append("\n");
        sb.append("   Retard: ").append(inv.getDaysOverdue()).append(" jours\n\n");
        totalOverdue = totalOverdue.add(inv.getRemainingAmount());
    }
    
    sb.append("---\n");
    sb.append("**Total en retard:** ").append(String.format("%,.2f €", totalOverdue));
    
    return sb.toString();
}
```

4. **Configurer le Tool Spring AI**

```java
// McpToolsConfiguration.java
@Bean
@Description("Liste toutes les factures en retard de paiement avec leur montant et délai")
public Function<Void, String> listOverdueInvoices() {
    return ignored -> {
        log.info("Tool listOverdueInvoices appelé");
        McpResponse response = capabilityHandler.listOverdueInvoices();
        return response.toAIFormat();
    };
}
```

5. **Tester**

```bash
curl -u support:support123 http://localhost:8080/api/chat/test/overdue-invoices
```

### Critères de validation

- [ ] La capacité est déclarée dans l'enum
- [ ] Les rôles appropriés y ont accès
- [ ] L'audit trace l'appel
- [ ] La réponse est formatée pour l'IA
- [ ] Le test curl fonctionne

---

## 10.2 Exercice 2 : Restreindre un rôle

### Objectif

Créer un nouveau rôle `VIEWER` qui ne peut que consulter les commandes (pas les factures ni les clients).

### Spécifications

| Rôle | Capacités autorisées |
|------|---------------------|
| VIEWER | findOrder uniquement |

### Étapes à suivre

1. **Ajouter le rôle**

```java
// McpRole.java
VIEWER(Set.of(
    McpCapability.FIND_ORDER
)),
```

2. **Configurer l'utilisateur**

```java
// SecurityConfig.java
UserDetails viewer = User.builder()
    .username("viewer")
    .password(passwordEncoder.encode("viewer123"))
    .roles("VIEWER")
    .build();

return new InMemoryUserDetailsManager(support, manager, admin, viewer);
```

3. **Tester les restrictions**

```bash
# Doit fonctionner
curl -u viewer:viewer123 http://localhost:8080/api/chat/test/find-order/CMD-20240115-TC001

# Doit échouer (ACCESS_DENIED)
curl -u viewer:viewer123 http://localhost:8080/api/chat/test/analyze-invoice/FAC-2024-000123

# Doit échouer (ACCESS_DENIED)
curl -u viewer:viewer123 http://localhost:8080/api/chat/test/customer-summary/CLI-001
```

### Critères de validation

- [ ] Le rôle VIEWER existe
- [ ] findOrder fonctionne pour VIEWER
- [ ] analyzeInvoice est refusé pour VIEWER
- [ ] summarizeCustomerActivity est refusé pour VIEWER
- [ ] L'audit trace les refus d'accès

---

## 10.3 Exercice 3 : Améliorer l'audit

### Objectif

Ajouter des statistiques d'utilisation par utilisateur et par capacité.

### Spécifications

Nouveau endpoint `/api/admin/audit/stats` qui retourne :
- Nombre d'appels par capacité
- Nombre d'appels par utilisateur
- Taux de succès/échec
- Derniers accès refusés

### Étapes à suivre

1. **Enrichir McpAuditService**

```java
// McpAuditService.java

// Statistiques par utilisateur
private final Map<String, Integer> userUsageStats = new ConcurrentHashMap<>();

// Dans startCapabilityCall, ajouter :
userUsageStats.merge(context.getUsername(), 1, Integer::sum);

// Compteur d'échecs
private final AtomicInteger failureCount = new AtomicInteger(0);
private final AtomicInteger successCount = new AtomicInteger(0);

// Dans completeCapabilityCall :
successCount.incrementAndGet();

// Dans failCapabilityCall :
failureCount.incrementAndGet();

// Nouvelle méthode
public Map<String, Object> getDetailedStatistics() {
    return Map.of(
        "byCapability", Map.copyOf(capabilityUsageStats),
        "byUser", Map.copyOf(userUsageStats),
        "totalSuccess", successCount.get(),
        "totalFailure", failureCount.get(),
        "successRate", calculateSuccessRate(),
        "recentAccessDenied", getRecentAccessDenied(5)
    );
}

private double calculateSuccessRate() {
    int total = successCount.get() + failureCount.get();
    return total > 0 ? (double) successCount.get() / total * 100 : 100.0;
}

private List<AuditEntry> getRecentAccessDenied(int count) {
    return auditLog.stream()
        .filter(e -> e.getStatus() == AuditStatus.ACCESS_DENIED)
        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
        .limit(count)
        .toList();
}
```

2. **Créer l'endpoint admin**

```java
// ChatController.java ou nouveau AdminController.java
@GetMapping("/api/admin/audit/stats")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map<String, Object>> getAuditStats() {
    return ResponseEntity.ok(auditService.getDetailedStatistics());
}
```

3. **Tester**

```bash
curl -u admin:admin123 http://localhost:8080/api/admin/audit/stats
```

### Critères de validation

- [ ] Les statistiques par capacité sont calculées
- [ ] Les statistiques par utilisateur sont calculées
- [ ] Le taux de succès est correct
- [ ] Les derniers accès refusés sont listés
- [ ] Seul ADMIN peut accéder à l'endpoint

---

## 10.4 Exercice 4 : Étendre le cas d'usage

### Objectif

Ajouter une capacité `generateMonthlyReport` qui génère un rapport mensuel d'activité.

### Spécifications

| Élément | Valeur |
|---------|--------|
| Nom | `generateMonthlyReport` |
| Paramètres | month (YYYY-MM), customerCode (optionnel) |
| Rôles | MANAGER, ADMIN |
| Confirmation | Non |

### Contenu du rapport

- Nombre de commandes du mois
- Chiffre d'affaires du mois
- Top 3 clients
- Factures émises
- Taux de recouvrement

### Étapes suggérées

1. Créer le DTO `MonthlyReport`
2. Ajouter la méthode dans un service (ou créer `ReportService`)
3. Ajouter la capacité MCP
4. Implémenter le handler
5. Configurer le Tool
6. Tester

### Exemple de sortie attendue

```
=== Rapport Mensuel - Janvier 2024 ===

**Période:** 01/01/2024 - 31/01/2024

**Activité commerciale:**
- Commandes passées: 45
- Chiffre d'affaires: 125 000,00 €
- Panier moyen: 2 777,78 €

**Top 3 clients:**
1. Grand Groupe SA - 65 000,00 €
2. TechCorp Solutions - 35 000,00 €
3. PME Innovation - 15 000,00 €

**Facturation:**
- Factures émises: 38
- Montant total facturé: 118 000,00 €
- Factures payées: 30
- Taux de recouvrement: 79%

**Points d'attention:**
⚠️ 3 factures en retard pour un total de 12 000,00 €
```

---

## 10.5 Exercice 5 : Tests unitaires

### Objectif

Écrire des tests unitaires pour `McpCapabilityHandler`.

### Tests à implémenter

```java
@SpringBootTest
class McpCapabilityHandlerTest {
    
    @Autowired
    private McpCapabilityHandler handler;
    
    @MockBean
    private McpSecurityContext securityContext;
    
    @MockBean
    private McpAuditService auditService;
    
    @Test
    void findOrder_shouldReturnOrder_whenOrderExists() {
        // Given
        when(securityContext.getRole()).thenReturn(McpRole.SUPPORT);
        when(auditService.startCapabilityCall(any(), any(), any()))
            .thenReturn("MCP-TEST-001");
        
        // When
        McpResponse response = handler.findOrder("CMD-20240115-TC001");
        
        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getContent()).contains("TechCorp");
        
        verify(auditService).startCapabilityCall(any(), any(), any());
        verify(auditService).completeCapabilityCall(any(), any(), any());
    }
    
    @Test
    void findOrder_shouldReturnNotFound_whenOrderDoesNotExist() {
        // Given
        when(securityContext.getRole()).thenReturn(McpRole.SUPPORT);
        
        // When
        McpResponse response = handler.findOrder("CMD-INEXISTANT");
        
        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.NOT_FOUND);
    }
    
    @Test
    void createOrder_shouldRequireConfirmation_whenNotConfirmed() {
        // Given
        when(securityContext.getRole()).thenReturn(McpRole.MANAGER);
        CreateOrderRequest request = createValidOrderRequest();
        
        // When
        McpResponse response = handler.createOrder(request, false);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.REQUIRES_CONFIRMATION);
        assertThat(response.isRequiresConfirmation()).isTrue();
    }
    
    @Test
    void createOrder_shouldCreate_whenConfirmed() {
        // Given
        when(securityContext.getRole()).thenReturn(McpRole.MANAGER);
        when(securityContext.getUsername()).thenReturn("test-user");
        CreateOrderRequest request = createValidOrderRequest();
        
        // When
        McpResponse response = handler.createOrder(request, true);
        
        // Then
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getContent()).contains("créée avec succès");
    }
    
    @Test
    void createOrder_shouldFail_whenSupportRole() {
        // Given
        when(securityContext.getRole()).thenReturn(McpRole.SUPPORT);
        doThrow(new McpAccessDeniedException("Accès refusé"))
            .when(securityContext).requireCapability(McpCapability.CREATE_ORDER);
        
        // When/Then
        assertThrows(McpAccessDeniedException.class, () -> {
            handler.createOrder(createValidOrderRequest(), false);
        });
    }
}
```

### Critères de validation

- [ ] Test de succès pour findOrder
- [ ] Test de not found pour findOrder
- [ ] Test de confirmation requise pour createOrder
- [ ] Test de création après confirmation
- [ ] Test de refus d'accès pour rôle non autorisé

---

## 10.6 Projet final : MCP complet

### Objectif

Implémenter un MCP Server complet pour un nouveau domaine métier de votre choix.

### Suggestions de domaines

1. **Gestion RH** : employés, congés, formations
2. **Support IT** : tickets, incidents, assets
3. **E-commerce** : produits, stocks, promotions
4. **Santé** : patients, rendez-vous, prescriptions

### Exigences

- [ ] Minimum 4 capacités
- [ ] Au moins 3 rôles différents
- [ ] Audit complet
- [ ] Au moins 1 capacité avec confirmation
- [ ] Tests unitaires
- [ ] Documentation

### Livrables

1. Code source complet
2. README avec instructions
3. Documentation des capacités
4. Tests (min 70% couverture)
5. Exemples de conversations

---

## 📝 Récapitulatif de la formation

### Ce que vous avez appris

1. ✅ **Concepts MCP** : Client/Server, Tools, Resources, Prompts
2. ✅ **Architecture** : Couches, patterns, flux de données
3. ✅ **Implémentation** : Spring Boot, Spring AI, handlers
4. ✅ **Sécurité** : RBAC, validation, confirmation
5. ✅ **Audit** : Traçabilité, logs structurés
6. ✅ **Bonnes pratiques** : Anti-patterns, checklist
7. ✅ **Carrière** : CV, entretien, évolution

### Prochaines étapes

1. **Pratiquez** : Faites tous les exercices
2. **Contribuez** : Participez à des projets open source Spring AI
3. **Partagez** : Écrivez des articles, donnez des talks
4. **Évoluez** : Visez architecte

---

## 🎓 Certificat de complétion

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│              FORMATION MCP POUR INGÉNIEURS JAVA                  │
│                                                                  │
│                     CERTIFICAT DE COMPLÉTION                     │
│                                                                  │
│  Cette formation atteste de la maîtrise des concepts suivants:  │
│                                                                  │
│  • Model Context Protocol (MCP)                                  │
│  • Intégration IA en entreprise                                  │
│  • Spring Boot & Spring AI                                       │
│  • Sécurité et gouvernance IA                                    │
│  • Architecture enterprise-ready                                 │
│                                                                  │
│  Durée: ~10 heures                                               │
│  Niveau: Ingénieur Java confirmé                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

**Félicitations ! Vous êtes maintenant prêt à intégrer l'IA dans vos projets Java de manière sécurisée et professionnelle.**

---

[← Chapitre précédent](./09-carriere.md) | [Retour à l'index](./00-index.md)
