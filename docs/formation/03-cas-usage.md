# Chapitre 3 : Présentation du cas d'usage

## 🎯 Objectifs du chapitre

- Comprendre le contexte métier du projet
- Identifier les besoins justifiant l'intégration IA
- Voir comment MCP répond à ces besoins

---

## 3.1 Le contexte métier

### L'entreprise fictive

**CommercePro SA** est une entreprise de distribution B2B qui gère :
- ~500 clients actifs
- ~2000 commandes/mois
- ~1500 factures/mois

### Les acteurs

| Rôle | Responsabilités | Besoins |
|------|-----------------|---------|
| **Support client** | Répondre aux questions clients | Accès rapide aux infos |
| **Commercial** | Gérer les comptes clients | Vue synthétique activité |
| **Manager** | Valider commandes, suivre CA | Analyses et alertes |
| **Direction** | Piloter l'activité | Tableaux de bord |

### Le SI existant

```
┌─────────────────────────────────────────────────────────────────┐
│                    SI EXISTANT                                   │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   ERP        │  │   CRM        │  │   Facturation│          │
│  │              │  │              │  │              │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                    │
│         └─────────────────┼─────────────────┘                    │
│                           │                                      │
│                    ┌──────▼───────┐                              │
│                    │   Base de    │                              │
│                    │   données    │                              │
│                    └──────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3.2 Les problèmes actuels

### Problème 1 : Information dispersée

```
Support: "Le client TechCorp appelle pour sa facture"

Actions actuelles:
1. Ouvrir le CRM → trouver le client
2. Ouvrir l'ERP → voir les commandes
3. Ouvrir la facturation → chercher la facture
4. Croiser les informations manuellement
5. Répondre au client

⏱️ Temps moyen: 5-10 minutes
```

### Problème 2 : Questions complexes

```
Manager: "Quels clients ont des factures en retard > 30 jours
          ET un encours > 10 000€?"

Actions actuelles:
1. Export Excel des factures
2. Filtrage manuel
3. Croisement avec les encours
4. Analyse des résultats

⏱️ Temps moyen: 30-60 minutes
```

### Problème 3 : Manque de proactivité

```
❌ Pas d'alerte automatique sur les risques
❌ Pas de synthèse client disponible rapidement
❌ Pas d'analyse de tendance en temps réel
```

---

## 3.3 La solution : un assistant IA

### Vision cible

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                  │
│  Support: "Donne-moi un résumé complet du client TechCorp"      │
│                                                                  │
│  Assistant IA:                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ === Résumé Client: TechCorp Solutions ===               │    │
│  │                                                          │    │
│  │ **Informations générales:**                              │    │
│  │ - Code client: CLI-001                                   │    │
│  │ - Segment: ENTERPRISE                                    │    │
│  │ - Contact: Jean Dupont                                   │    │
│  │                                                          │    │
│  │ **Activité commerciale:**                                │    │
│  │ - Commandes totales: 15                                  │    │
│  │ - CA cumulé: 125 000 €                                   │    │
│  │                                                          │    │
│  │ **Situation financière:**                                │    │
│  │ - Factures payées: 12                                    │    │
│  │ - Encours: 15 000 €                                      │    │
│  │ ⚠️ 1 facture en retard de 15 jours                       │    │
│  │                                                          │    │
│  │ **Dernières commandes:**                                 │    │
│  │ - CMD-20240210 | En préparation | 4 500 €               │    │
│  │ - CMD-20240115 | Livrée | 17 500 €                      │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ⏱️ Temps: < 5 secondes                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Bénéfices attendus

| Métrique | Avant | Après | Gain |
|----------|-------|-------|------|
| Temps recherche info | 5-10 min | < 10 sec | **95%** |
| Temps analyse complexe | 30-60 min | < 1 min | **98%** |
| Satisfaction support | 65% | 90% | **+25 pts** |
| Erreurs humaines | ~5% | < 1% | **-80%** |

---

## 3.4 Pourquoi MCP pour ce cas ?

### Alternative 1 : Chatbot classique (RAG)

```
❌ Problèmes:
- Données en temps réel difficiles
- Pas d'actions possibles
- Hallucinations fréquentes
- Pas de contrôle fin
```

### Alternative 2 : API REST directes

```
❌ Problèmes:
- L'IA doit "deviner" les endpoints
- Pas de sécurité par capacité
- Pas d'orchestration côté serveur
- Difficile à auditer
```

### Solution MCP

```
✅ Avantages:
- Capacités métier explicites
- Sécurité par rôle intégrée
- Audit de chaque appel
- Réponses formatées pour l'IA
- Orchestration contrôlée
```

---

## 3.5 Les capacités MCP retenues

### Capacité 1 : findOrder

**Objectif** : Rechercher une commande par son numéro

```
┌─────────────────────────────────────────────────────────────────┐
│ Capacité: findOrder                                              │
├─────────────────────────────────────────────────────────────────┤
│ Description: Recherche une commande par son numéro et retourne  │
│              les détails complets (client, lignes, montants,    │
│              statut, dates)                                      │
│                                                                  │
│ Paramètres:                                                      │
│   - orderNumber (String): "CMD-20240115-TC001"                  │
│                                                                  │
│ Retour: Description textuelle formatée de la commande           │
│                                                                  │
│ Rôles autorisés: SUPPORT, MANAGER, ADMIN                        │
│ Confirmation requise: Non                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Capacité 2 : analyzeInvoice

**Objectif** : Analyse détaillée d'une facture avec indicateurs de risque

```
┌─────────────────────────────────────────────────────────────────┐
│ Capacité: analyzeInvoice                                         │
├─────────────────────────────────────────────────────────────────┤
│ Description: Analyse une facture en profondeur avec:            │
│              - Statut de paiement                                │
│              - Indicateurs de risque                             │
│              - Recommandations d'action                          │
│              - Historique client                                 │
│                                                                  │
│ Paramètres:                                                      │
│   - invoiceNumber (String): "FAC-2024-000123"                   │
│                                                                  │
│ Retour: Rapport d'analyse avec niveau de risque                 │
│                                                                  │
│ Rôles autorisés: SUPPORT, MANAGER, ADMIN                        │
│ Confirmation requise: Non                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Capacité 3 : summarizeCustomerActivity

**Objectif** : Générer un résumé complet de l'activité d'un client

```
┌─────────────────────────────────────────────────────────────────┐
│ Capacité: summarizeCustomerActivity                              │
├─────────────────────────────────────────────────────────────────┤
│ Description: Génère un résumé complet incluant:                 │
│              - Informations client                               │
│              - Commandes récentes                                │
│              - Factures et paiements                             │
│              - Indicateurs de fidélité                           │
│              - Alertes éventuelles                               │
│                                                                  │
│ Paramètres:                                                      │
│   - customerCode (String): "CLI-001"                            │
│                                                                  │
│ Retour: Résumé structuré de l'activité client                   │
│                                                                  │
│ Rôles autorisés: SUPPORT, MANAGER, ADMIN                        │
│ Confirmation requise: Non                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Capacité 4 : createOrder

**Objectif** : Créer une nouvelle commande (avec validation et confirmation)

```
┌─────────────────────────────────────────────────────────────────┐
│ Capacité: createOrder                                            │
├─────────────────────────────────────────────────────────────────┤
│ Description: Crée une nouvelle commande pour un client avec:    │
│              - Validation des données                            │
│              - Vérification limite de crédit                     │
│              - Demande de confirmation                           │
│              - Création effective après confirmation             │
│                                                                  │
│ Paramètres:                                                      │
│   - customerCode (String): Code du client                       │
│   - lines (List): Lignes de commande avec produits/quantités    │
│   - shippingAddress (String, optionnel): Adresse de livraison   │
│   - confirmed (Boolean): True si confirmation reçue             │
│                                                                  │
│ Retour: Résultat de validation ou confirmation de création      │
│                                                                  │
│ Rôles autorisés: MANAGER, ADMIN                                 │
│ Confirmation requise: ⚠️ OUI                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3.6 Matrice des rôles et capacités

```
┌─────────────────────────────────────────────────────────────────┐
│                    MATRICE RBAC                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Capacité                  │ SUPPORT │ MANAGER │ ADMIN          │
│  ─────────────────────────────────────────────────────────      │
│  findOrder                 │   ✅    │   ✅    │   ✅           │
│  analyzeInvoice            │   ✅    │   ✅    │   ✅           │
│  summarizeCustomerActivity │   ✅    │   ✅    │   ✅           │
│  createOrder               │   ❌    │   ✅    │   ✅           │
│  validateOrder             │   ❌    │   ✅    │   ✅           │
│  cancelOrder               │   ❌    │   ❌    │   ✅           │
│                                                                  │
│  Légende:                                                        │
│  ✅ = Autorisé                                                   │
│  ❌ = Refusé (AccessDeniedException)                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3.7 Scénarios d'utilisation

### Scénario 1 : Support client

```
Contexte: Un client appelle pour connaître l'état de sa commande

Support: "Où en est la commande CMD-20240210-TC002?"

IA → MCP: findOrder("CMD-20240210-TC002")

Réponse: "La commande CMD-20240210-TC002 est actuellement en préparation.
          Elle concerne 3 formations Expert pour un total de 4 500€.
          La livraison est prévue pour le 17 février."
```

### Scénario 2 : Analyse risque

```
Contexte: Le manager veut évaluer un risque client

Manager: "Analyse la facture FAC-2024-000100 en détail"

IA → MCP: analyzeInvoice("FAC-2024-000100")

Réponse: "⚠️ ATTENTION - Facture à risque ÉLEVÉ
          
          La facture FAC-2024-000100 de Startup Digital est en retard 
          de 45 jours pour un montant de 1 800€.
          
          Recommandations:
          → Envoyer une deuxième relance
          → Contacter le client par téléphone
          → Envisager la suspension du compte"
```

### Scénario 3 : Création commande

```
Contexte: Un commercial veut créer une commande rapidement

Commercial: "Crée une commande pour TechCorp avec 5 licences Enterprise
             à 2500€ pièce"

IA → MCP: createOrder(customerCode="CLI-001", 
                      lines=[{productCode="PROD-001", quantity=5, ...}],
                      confirmed=false)

MCP → IA: "CONFIRMATION REQUISE:
           Commande de 12 500€ HT pour TechCorp Solutions
           - 5x Licence Logiciel Enterprise @ 2 500€
           
           Confirmez-vous cette création?"

Commercial: "Oui, confirme"

IA → MCP: createOrder(..., confirmed=true)

Réponse: "✅ Commande CMD-20240315-xxx créée avec succès!
          Montant: 12 500€ HT
          Statut: En attente de validation"
```

---

## 📝 Points clés à retenir

1. **Problème réel** : Information dispersée, recherches chronophages
2. **Solution MCP** : Capacités métier explicites et contrôlées
3. **4 capacités** : findOrder, analyzeInvoice, summarizeCustomer, createOrder
4. **Sécurité** : RBAC avec 3 niveaux (SUPPORT, MANAGER, ADMIN)
5. **Confirmation** : Obligatoire pour les actions qui modifient des données

---

## 🎯 Quiz de validation

1. Pourquoi un chatbot RAG classique ne suffit-il pas pour ce cas ?
2. Quelle capacité nécessite une confirmation et pourquoi ?
3. Quel rôle peut créer une commande ?
4. Combien de temps gagne-t-on sur une recherche d'information ?

---

[← Chapitre précédent](./02-concepts-fondamentaux.md) | [Chapitre suivant →](./04-architecture.md)
