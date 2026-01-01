# Chapitre 2 : Concepts fondamentaux MCP

## 🎯 Objectifs du chapitre

- Maîtriser le vocabulaire MCP
- Comprendre l'architecture Client/Server
- Appréhender le cycle de vie d'un appel MCP

---

## 2.1 Architecture MCP Client/Server

### Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE MCP                              │
│                                                                  │
│  ┌─────────────────┐         ┌─────────────────────────────┐    │
│  │   MCP CLIENT    │         │       MCP SERVER            │    │
│  │                 │         │                             │    │
│  │  ┌───────────┐  │  JSON   │  ┌───────────────────────┐  │    │
│  │  │    LLM    │  │◄───────►│  │    Capabilities       │  │    │
│  │  │  (GPT-4,  │  │  RPC    │  │  ┌─────┐ ┌─────┐     │  │    │
│  │  │  Claude)  │  │         │  │  │Tool1│ │Tool2│ ... │  │    │
│  │  └───────────┘  │         │  │  └─────┘ └─────┘     │  │    │
│  │                 │         │  └───────────────────────┘  │    │
│  └─────────────────┘         │                             │    │
│                              │  ┌───────────────────────┐  │    │
│                              │  │   Resources           │  │    │
│                              │  │   (données statiques) │  │    │
│                              │  └───────────────────────┘  │    │
│                              │                             │    │
│                              │  ┌───────────────────────┐  │    │
│                              │  │   Prompts             │  │    │
│                              │  │   (templates)         │  │    │
│                              │  └───────────────────────┘  │    │
│                              └─────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### MCP Client

Le **MCP Client** est l'entité qui consomme les capacités MCP. Typiquement :
- Un LLM (GPT-4, Claude, etc.)
- Une application d'orchestration IA
- Un agent autonome

**Responsabilités :**
- Découvrir les capacités disponibles
- Formuler des requêtes structurées
- Interpréter les réponses

### MCP Server

Le **MCP Server** expose les capacités métier. Dans notre cas, c'est une application Spring Boot.

**Responsabilités :**
- Déclarer les capacités disponibles
- Valider les requêtes
- Exécuter les actions
- Retourner des réponses formatées

---

## 2.2 Les trois primitives MCP

### 1. Tools (Capacités/Outils)

Les **Tools** sont des fonctions que l'IA peut appeler. C'est la primitive la plus importante.

```java
// Exemple de Tool en Spring AI
@Bean
@Description("Recherche une commande par son numéro")
public Function<FindOrderRequest, String> findOrder() {
    return request -> {
        // Logique métier
        return orderService.findByNumber(request.orderNumber())
            .map(Order::toDescription)
            .orElse("Commande non trouvée");
    };
}
```

**Caractéristiques d'un Tool :**
- **Nom** : Identifiant unique (ex: `findOrder`)
- **Description** : Explication en langage naturel pour le LLM
- **Paramètres** : Entrées typées avec descriptions
- **Retour** : Texte formaté pour le LLM

### 2. Resources (Ressources)

Les **Resources** sont des données statiques accessibles par l'IA.

```
Exemples de Resources :
- Documentation produit
- Catalogue de prix
- FAQ
- Politiques d'entreprise
```

**Différence Tool vs Resource :**
| Aspect | Tool | Resource |
|--------|------|----------|
| Nature | Dynamique (exécution) | Statique (lecture) |
| Paramètres | Oui | Non (URI fixe) |
| Effet de bord | Possible | Non |

### 3. Prompts (Templates)

Les **Prompts** sont des templates réutilisables pour guider le LLM.

```
Exemple de Prompt :
Nom: analyze_customer_risk
Template: "Analyse le risque du client {customerCode} en te basant sur :
- Son historique de paiement
- Ses commandes récentes  
- Son encours actuel
Fournis une note de risque de 1 à 10."
```

---

## 2.3 Cycle de vie d'un appel MCP

### Séquence complète

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  User   │     │   LLM   │     │   MCP   │     │ Service │
│         │     │         │     │ Server  │     │ Métier  │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ "Montre-moi   │               │               │
     │  la commande  │               │               │
     │  CMD-123"     │               │               │
     │──────────────►│               │               │
     │               │               │               │
     │               │ 1. Analyse    │               │
     │               │    intent     │               │
     │               │               │               │
     │               │ 2. Sélection  │               │
     │               │    tool:      │               │
     │               │    findOrder  │               │
     │               │               │               │
     │               │ 3. Appel MCP  │               │
     │               │──────────────►│               │
     │               │ {orderNumber: │               │
     │               │  "CMD-123"}   │               │
     │               │               │               │
     │               │               │ 4. Validation │
     │               │               │    sécurité   │
     │               │               │               │
     │               │               │ 5. Audit      │
     │               │               │    start      │
     │               │               │               │
     │               │               │ 6. Appel      │
     │               │               │    service    │
     │               │               │──────────────►│
     │               │               │               │
     │               │               │◄──────────────│
     │               │               │    Order      │
     │               │               │               │
     │               │               │ 7. Formatage  │
     │               │               │    réponse    │
     │               │               │               │
     │               │               │ 8. Audit      │
     │               │               │    complete   │
     │               │               │               │
     │               │◄──────────────│               │
     │               │ "=== Commande │               │
     │               │  CMD-123 ===" │               │
     │               │               │               │
     │               │ 9. Formulation│               │
     │               │    réponse    │               │
     │               │    naturelle  │               │
     │               │               │               │
     │◄──────────────│               │               │
     │ "Voici les    │               │               │
     │  détails de   │               │               │
     │  la commande" │               │               │
     │               │               │               │
```

### Les 9 étapes détaillées

| # | Étape | Responsable | Description |
|---|-------|-------------|-------------|
| 1 | Analyse intent | LLM | Comprend ce que veut l'utilisateur |
| 2 | Sélection tool | LLM | Choisit la capacité appropriée |
| 3 | Appel MCP | LLM → MCP | Envoie la requête structurée |
| 4 | Validation | MCP Server | Vérifie droits et paramètres |
| 5 | Audit start | MCP Server | Trace le début de l'appel |
| 6 | Appel service | MCP → Service | Exécute la logique métier |
| 7 | Formatage | MCP Server | Prépare la réponse pour l'IA |
| 8 | Audit complete | MCP Server | Trace la fin de l'appel |
| 9 | Formulation | LLM | Génère la réponse utilisateur |

---

## 2.4 Gouvernance MCP

### Principe de moindre privilège

```
┌─────────────────────────────────────────────────────────────────┐
│                    MATRICE DE CAPACITÉS                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Capacité              │ SUPPORT │ MANAGER │ ADMIN              │
│  ──────────────────────┼─────────┼─────────┼──────              │
│  findOrder             │    ✅   │    ✅   │   ✅               │
│  analyzeInvoice        │    ✅   │    ✅   │   ✅               │
│  summarizeCustomer     │    ✅   │    ✅   │   ✅               │
│  createOrder           │    ❌   │    ✅   │   ✅               │
│  validateOrder         │    ❌   │    ✅   │   ✅               │
│  cancelOrder           │    ❌   │    ❌   │   ✅               │
│  deleteCustomer        │    ❌   │    ❌   │   ✅               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Confirmation des actions sensibles

Certaines capacités nécessitent une confirmation explicite :

```java
public enum McpCapability {
    FIND_ORDER("findOrder", false),           // Lecture seule
    CREATE_ORDER("createOrder", true),        // ⚠️ Confirmation requise
    CANCEL_ORDER("cancelOrder", true);        // ⚠️ Confirmation requise
    
    private final boolean requiresConfirmation;
}
```

**Workflow avec confirmation :**

```
User: "Crée une commande pour TechCorp avec 5 licences"

LLM → MCP: createOrder(customerCode="CLI-001", ...)

MCP → LLM: {
  status: "REQUIRES_CONFIRMATION",
  summary: "Commande de 12 500€ pour TechCorp",
  correlationId: "MCP-xxx"
}

LLM → User: "Je vais créer une commande de 12 500€ pour TechCorp.
             Confirmez-vous cette action?"

User: "Oui, confirme"

LLM → MCP: createOrder(..., confirmed=true)

MCP → LLM: "✅ Commande CMD-xxx créée avec succès"
```

---

## 2.5 Orchestration

### Appels multiples

Un LLM peut orchestrer plusieurs appels MCP pour répondre à une question complexe :

```
User: "Analyse la situation du client TechCorp"

LLM pense: "Je dois appeler plusieurs capacités"

1. summarizeCustomerActivity("CLI-001")
   → Résumé de l'activité client

2. Pour chaque facture impayée trouvée:
   analyzeInvoice("FAC-xxx")
   → Analyse de risque

3. Synthèse finale pour l'utilisateur
```

### Chaînage intelligent

```java
// Le LLM peut chaîner les appels logiquement
// Étape 1: Trouver le client
summarizeCustomerActivity("CLI-001")

// Si des factures en retard sont mentionnées
// Étape 2: Analyser chaque facture
analyzeInvoice("FAC-2024-000100")
analyzeInvoice("FAC-2024-000101")

// Étape 3: Synthèse
// Le LLM combine toutes les informations
```

---

## 2.6 Format des échanges

### Requête MCP (JSON-RPC)

```json
{
  "jsonrpc": "2.0",
  "method": "tools/call",
  "params": {
    "name": "findOrder",
    "arguments": {
      "orderNumber": "CMD-20240115-TC001"
    }
  },
  "id": "req-123"
}
```

### Réponse MCP

```json
{
  "jsonrpc": "2.0",
  "result": {
    "content": [
      {
        "type": "text",
        "text": "=== Commande CMD-20240115-TC001 ===\n\nClient: TechCorp Solutions\nStatut: DELIVERED\n..."
      }
    ]
  },
  "id": "req-123"
}
```

---

## 📝 Points clés à retenir

1. **MCP = Client + Server** avec protocole standardisé
2. **3 primitives** : Tools, Resources, Prompts
3. **Cycle de vie** : Validation → Audit → Exécution → Formatage
4. **Gouvernance** : RBAC + Confirmation pour actions sensibles
5. **Orchestration** : Le LLM peut chaîner plusieurs appels

---

## 🎯 Quiz de validation

1. Quelle est la différence entre un Tool et une Resource ?
2. Quelles sont les responsabilités du MCP Server ?
3. Pourquoi certaines capacités nécessitent-elles une confirmation ?
4. Dans quel format sont échangés les messages MCP ?

---

[← Chapitre précédent](./01-introduction.md) | [Chapitre suivant →](./03-cas-usage.md)
