# MCP Enterprise Demo - Gestion Commerciale avec IA

## 🎯 Présentation

Ce projet démontre l'intégration de **MCP (Model Context Protocol)** dans un Système d'Information d'entreprise pour la gestion commerciale (Commandes, Factures, Clients).

**Objectif** : Créer un assistant IA interne capable de :
- Répondre à des questions métier en langage naturel
- Analyser commandes et factures
- Résumer l'activité d'un client
- Déclencher des actions contrôlées

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        UTILISATEUR                               │
│                    (Support / Manager / Admin)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ASSISTANT IA (LLM)                          │
│                   (Spring AI + OpenAI/Claude)                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MCP SERVER                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              CAPACITÉS MCP (Tools)                       │    │
│  │  • findOrder          • analyzeInvoice                   │    │
│  │  • summarizeCustomer  • createOrder                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              SÉCURITÉ & GOUVERNANCE                      │    │
│  │  • Contrôle d'accès (RBAC)  • Audit des appels          │    │
│  │  • Validation métier        • Traçabilité               │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SERVICES MÉTIERS                               │
│  ┌───────────────┐ ┌───────────────┐ ┌───────────────┐         │
│  │ OrderService  │ │InvoiceService │ │CustomerService│         │
│  └───────────────┘ └───────────────┘ └───────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      BASE DE DONNÉES                             │
│                    (H2 en mémoire pour la démo)                  │
└─────────────────────────────────────────────────────────────────┘
```

## 🔐 Rôles et Capacités

| Rôle | findOrder | analyzeInvoice | summarizeCustomer | createOrder | validateOrder |
|------|-----------|----------------|-------------------|-------------|---------------|
| SUPPORT | ✅ | ✅ | ✅ | ❌ | ❌ |
| MANAGER | ✅ | ✅ | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ |

## 🚀 Démarrage rapide

### Prérequis

- Java 17+
- Maven 3.8+
- (Optionnel) Clé API OpenAI pour le LLM

### Installation

```bash
# Cloner le projet
cd mcp-enterprise-demo

# Compiler
mvn clean install

# Lancer l'application
mvn spring-boot:run
```

### Configuration OpenAI (optionnel)

```bash
export OPENAI_API_KEY=votre-clé-api
```

## 📡 Endpoints de test

### Authentification

Utilisateurs de démonstration :
- `support` / `support123` (rôle SUPPORT)
- `manager` / `manager123` (rôle MANAGER)
- `admin` / `admin123` (rôle ADMIN)

### Tester les capacités MCP

```bash
# Rechercher une commande
curl -u manager:manager123 http://localhost:8080/api/chat/test/find-order/CMD-20240115-TC001

# Analyser une facture
curl -u support:support123 http://localhost:8080/api/chat/test/analyze-invoice/FAC-2024-000123

# Résumé client
curl -u support:support123 http://localhost:8080/api/chat/test/customer-summary/CLI-001

# Voir les capacités disponibles
curl -u manager:manager123 http://localhost:8080/api/chat/capabilities
```

### Tester via l'interface web (IHM)

Une application Angular est disponible pour tester les capacités MCP via une interface utilisateur moderne.

```bash
cd mcp-frontend
npm install
npm start
```

L'application sera accessible sur `http://localhost:4200/`.

**Fonctionnalités disponibles :**
- **Dashboard** - Vue d'ensemble avec capacités MCP disponibles selon le rôle
- **Commandes** - Recherche de commandes via MCP
- **Factures** - Analyse de factures via MCP
- **Clients** - Résumé d'activité client via MCP
- **Produits** - Gestion du catalogue produits
- **Assistant IA** - Chat avec l'IA pour interagir en langage naturel

👉 **[Documentation complète du frontend](mcp-frontend/README.md)**

### Tester via LLM (OpenAI / Spring AI)

L'endpoint `POST /api/chat/llm/message` envoie le message au LLM (via Spring AI) et autorise l'appel des tools MCP selon le rôle courant.

Important : pour conserver le contexte (ex: workflow de confirmation), utilisez un `conversationId` et réutilisez-le à chaque tour.

Sous PowerShell, utilisez `curl.exe` (car `curl` est un alias de `Invoke-WebRequest`).

```bash
# 1) Premier message (le serveur renvoie un conversationId)
curl.exe -u manager:manager123 -H "Content-Type: application/json" -d "{\"message\":\"Crée une commande pour CLI-001 avec 2 PROD-001\"}" http://localhost:8080/api/chat/llm/message

# 2) Confirmer (réutiliser le conversationId reçu à l'étape 1)
curl.exe -u manager:manager123 -H "Content-Type: application/json" -d "{\"message\":\"Oui je confirme\",\"conversationId\":\"<COLLER_ICI>\"}" http://localhost:8080/api/chat/llm/message
```

Si l'utilisateur n'a pas les droits (ex: rôle SUPPORT), le LLM est informé des capacités autorisées et doit refuser l'action au lieu d'appeler un tool non autorisé.

### Console H2

Accédez à `http://localhost:8080/h2-console` pour explorer la base de données.
- JDBC URL: `jdbc:h2:mem:enterprisedb`
- User: `sa`
- Password: (vide)

## 📁 Structure du projet

```
src/main/java/com/enterprise/mcp/
├── McpEnterpriseApplication.java    # Point d'entrée
├── domain/
│   ├── entity/                       # Entités JPA
│   │   ├── Customer.java
│   │   ├── Order.java
│   │   ├── OrderLine.java
│   │   ├── Invoice.java
│   │   └── InvoiceLine.java
│   └── repository/                   # Repositories Spring Data
├── service/
│   ├── CustomerService.java          # Service métier clients
│   ├── OrderService.java             # Service métier commandes
│   ├── InvoiceService.java           # Service métier factures
│   └── dto/                          # Objets de transfert
├── mcp/
│   ├── McpCapabilityHandler.java     # Gestionnaire des capacités
│   ├── McpResponse.java              # Réponse standardisée
│   └── McpToolsConfiguration.java    # Configuration Spring AI
├── security/
│   ├── McpRole.java                  # Définition des rôles
│   ├── McpCapability.java            # Enum des capacités
│   ├── McpSecurityContext.java       # Contexte de sécurité
│   └── McpAccessDeniedException.java # Exception accès refusé
├── audit/
│   └── McpAuditService.java          # Service d'audit
├── config/
│   ├── SecurityConfig.java           # Configuration Spring Security
│   └── DataInitializer.java          # Données de démonstration
└── controller/
    └── ChatController.java           # API REST
```

## 🎓 Formation associée

Ce projet est accompagné d'une formation complète en Markdown :
→ Voir le dossier `docs/formation/`

## ⚠️ Points clés MCP

### Ce que MCP fait

- ✅ Expose des **capacités métier contrôlées** à l'IA
- ✅ Applique la **sécurité par rôle** (RBAC)
- ✅ **Audite** toutes les interactions IA
- ✅ **Valide** les données côté serveur
- ✅ Demande **confirmation** pour les actions sensibles

### Ce que MCP ne fait pas

- ❌ L'IA n'accède **jamais directement** à la base de données
- ❌ Pas d'exécution de code arbitraire
- ❌ Pas de contournement des règles métier

## 📜 Licence

Projet de démonstration à usage pédagogique.

## 👤 Auteur

Projet créé pour la formation "MCP pour ingénieurs Java".
