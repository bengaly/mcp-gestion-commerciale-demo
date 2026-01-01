# Chapitre 1 : Introduction - Pourquoi MCP en entreprise

## 🎯 Objectifs du chapitre

- Comprendre le contexte d'émergence de MCP
- Différencier MCP des approches existantes
- Identifier les cas d'usage légitimes en entreprise

---

## 1.1 Le problème : l'IA et le SI d'entreprise

### L'explosion de l'IA générative

Depuis 2022, les LLM (Large Language Models) ont révolutionné l'interaction homme-machine. Mais leur intégration en entreprise pose des défis majeurs :

```
┌─────────────────────────────────────────────────────────────────┐
│                    DÉFIS DE L'INTÉGRATION IA                     │
├─────────────────────────────────────────────────────────────────┤
│ 🔐 Sécurité    │ Comment contrôler ce que l'IA peut faire ?     │
│ 📊 Données     │ Comment lui donner accès aux bonnes données ?  │
│ 🔍 Traçabilité │ Comment savoir ce que l'IA a fait ?            │
│ ✅ Validation  │ Comment valider ses actions ?                   │
│ 🎯 Gouvernance │ Comment respecter les règles métier ?          │
└─────────────────────────────────────────────────────────────────┘
```

### Les approches naïves (à éviter)

**Approche 1 : Accès direct à la base de données**
```
❌ LLM → SQL brut → Base de données
```
Problèmes :
- Injection SQL possible
- Pas de validation métier
- Pas de contrôle d'accès fin
- Données sensibles exposées

**Approche 2 : Appels API REST directs**
```
❌ LLM → URLs API → Services
```
Problèmes :
- L'IA doit "deviner" les URLs
- Pas de contexte sémantique
- Difficile à sécuriser
- Pas d'orchestration

---

## 1.2 MCP : La solution architecturale

### Qu'est-ce que MCP ?

**MCP (Model Context Protocol)** est un protocole standardisé qui définit comment un LLM peut interagir de manière **contrôlée** avec des systèmes externes.

```
┌─────────────────────────────────────────────────────────────────┐
│                      AVEC MCP                                    │
│                                                                  │
│   LLM  ──────►  MCP Server  ──────►  Services Métiers           │
│         protocole     │        │                                 │
│         standardisé   │        │                                 │
│                       ▼        ▼                                 │
│                   Sécurité   Audit                               │
└─────────────────────────────────────────────────────────────────┘
```

### Les concepts clés

| Concept | Description | Analogie |
|---------|-------------|----------|
| **Capability** | Action que l'IA peut demander | Un bouton sur une télécommande |
| **MCP Server** | Serveur exposant les capacités | La box TV qui reçoit les commandes |
| **MCP Client** | Le LLM qui consomme les capacités | La télécommande |
| **Tool** | Implémentation technique d'une capacité | Le circuit derrière le bouton |

---

## 1.3 MCP ≠ API REST

### Différences fondamentales

| Aspect | API REST | MCP |
|--------|----------|-----|
| **Consommateur** | Application/Humain | LLM/IA |
| **Découverte** | Documentation Swagger | Description sémantique |
| **Paramètres** | Types stricts | Types + descriptions naturelles |
| **Réponse** | JSON technique | Texte compréhensible par l'IA |
| **Orchestration** | Côté client | Côté serveur MCP |
| **Sécurité** | Par token/API key | Par capacité + rôle |

### Exemple concret

**API REST classique :**
```json
GET /api/orders/CMD-123
Authorization: Bearer xyz

{
  "id": 123,
  "status": "DELIVERED",
  "total": 1500.00,
  "customer_id": 456
}
```

**Capacité MCP :**
```
Capability: findOrder
Description: "Recherche une commande par son numéro et retourne 
              les détails complets incluant client, lignes et statut"
Parameters:
  - orderNumber: "Le numéro de commande (ex: CMD-20240115-ABC)"
Response: "Texte formaté décrivant la commande de manière complète"
```

---

## 1.4 MCP ≠ Frontend classique

### MCP complète, ne remplace pas

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE CIBLE                            │
│                                                                  │
│  ┌──────────────┐              ┌──────────────┐                 │
│  │  Frontend    │              │  Assistant   │                 │
│  │  Classique   │              │     IA       │                 │
│  │  (React...)  │              │   (Chat)     │                 │
│  └──────┬───────┘              └──────┬───────┘                 │
│         │                             │                          │
│         │  API REST                   │  MCP                     │
│         │                             │                          │
│         ▼                             ▼                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    BACKEND JAVA                          │    │
│  │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │    │
│  │  │  REST API   │    │ MCP Server  │    │  Services   │  │    │
│  │  │ Controllers │    │ Capabilities│───►│   Métiers   │  │    │
│  │  └──────┬──────┘    └─────────────┘    └─────────────┘  │    │
│  │         │                                     ▲          │    │
│  │         └─────────────────────────────────────┘          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Cas d'usage respectifs

| Besoin | Solution |
|--------|----------|
| Formulaire de création de commande | Frontend classique |
| "Montre-moi les commandes en retard de ce client" | Assistant IA + MCP |
| Dashboard avec graphiques | Frontend classique |
| "Analyse cette facture et dis-moi les risques" | Assistant IA + MCP |
| Export Excel | Frontend classique |
| "Résume l'activité de ce client sur les 6 derniers mois" | Assistant IA + MCP |

---

## 1.5 Positionnement architectural de MCP

### MCP est une couche d'architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    COUCHES ARCHITECTURALES                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    PRÉSENTATION                          │    │
│  │            (Frontend, API REST, MCP Server)              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    APPLICATION                           │    │
│  │              (Services métiers, Use Cases)               │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                      DOMAINE                             │    │
│  │               (Entités, Règles métier)                   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                   INFRASTRUCTURE                         │    │
│  │            (Base de données, APIs externes)              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Point clé** : MCP se positionne dans la couche **Présentation**, au même niveau qu'une API REST. Il ne remplace pas les services métiers, il les **expose différemment**.

---

## 1.6 Pourquoi MCP maintenant ?

### Facteurs de convergence

1. **Maturité des LLM** : GPT-4, Claude 3, Gemini sont capables de raisonnement complexe
2. **Demande métier** : Les utilisateurs veulent interagir en langage naturel
3. **Standardisation** : MCP apporte un protocole commun (initié par Anthropic)
4. **Écosystème Spring** : Spring AI simplifie l'intégration Java

### Bénéfices pour l'entreprise

| Bénéfice | Description |
|----------|-------------|
| **Productivité** | Questions complexes en langage naturel |
| **Accessibilité** | Pas besoin de connaître les outils métier |
| **Contrôle** | Gouvernance centralisée des accès IA |
| **Évolutivité** | Ajout de capacités sans modifier le LLM |
| **Sécurité** | Audit complet des interactions |

---

## 📝 Points clés à retenir

1. **MCP est une couche d'architecture**, pas un gadget IA
2. **MCP complète** le frontend classique, ne le remplace pas
3. L'IA agit **uniquement via des capacités explicitement exposées**
4. Chaque décision doit être **explicable et traçable**
5. La **sécurité et la gouvernance** sont obligatoires

---

## 🎯 Quiz de validation

1. Pourquoi ne pas donner un accès SQL direct à un LLM ?
2. Quelle est la différence principale entre une API REST et une capacité MCP ?
3. Dans quelle couche architecturale se positionne MCP ?
4. MCP remplace-t-il le frontend classique ?

---

[→ Chapitre suivant : Concepts fondamentaux MCP](./02-concepts-fondamentaux.md)
