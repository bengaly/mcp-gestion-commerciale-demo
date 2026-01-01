# Exemples de conversations IA avec MCP

Ce document présente des exemples concrets de conversations entre utilisateurs et l'assistant IA utilisant les capacités MCP.

---

## Exemple 1 : Recherche de commande simple

### Contexte
Agent support qui répond à un appel client.

### Conversation

```
👤 Utilisateur (Support - Marie):
Le client TechCorp demande des infos sur sa commande CMD-20240115-TC001

🤖 Assistant IA:
Je recherche cette commande pour vous.

[Appel MCP: findOrder("CMD-20240115-TC001")]

Voici les informations sur la commande CMD-20240115-TC001 :

=== Commande CMD-20240115-TC001 ===

**Client:** TechCorp Solutions
**Statut:** DELIVERED ✅
**Date de commande:** 15/01/2024

**Lignes de commande:**
- Licence Logiciel Enterprise (PROD-001) x 5 @ 2 500,00 € = 12 500,00 €
- Support Premium 1 an (PROD-002) x 1 @ 5 000,00 € = 5 000,00 €

**Total HT:** 17 500,00 €
**TVA (20%):** 3 500,00 €
**Total TTC:** 21 000,00 €

**Adresse de livraison:** 123 Rue de l'Entreprise, 75001 Paris

La commande a été livrée avec succès. Vous pouvez informer le client que tout est en ordre.
```

### Trace d'audit générée

```
[2024-01-20 14:32:42] [AUDIT-START] MCP-1705757562-A1B2C3D4 
  | User: marie.support | Role: SUPPORT | Capability: findOrder 
  | Params: {orderNumber=CMD-20240115-TC001}

[2024-01-20 14:32:42] [AUDIT-COMPLETE] MCP-1705757562-A1B2C3D4 
  | Capability: findOrder | Result: Commande trouvée: CMD-20240115-TC001
```

---

## Exemple 2 : Analyse de facture à risque

### Contexte
Manager qui évalue un risque client.

### Conversation

```
👤 Utilisateur (Manager - Pierre):
J'ai reçu une alerte sur la facture FAC-2024-000100, peux-tu l'analyser ?

🤖 Assistant IA:
Je lance une analyse détaillée de cette facture.

[Appel MCP: analyzeInvoice("FAC-2024-000100")]

=== Analyse de la Facture FAC-2024-000100 ===

**Client:** Startup Digital (CLI-003)
**Statut:** OVERDUE
**Date d'émission:** 01/11/2023
**Date d'échéance:** 01/12/2023

**Situation financière:**
- Montant total: 1 800,00 €
- Montant payé: 0,00 € (0.0%)
- Reste à payer: 1 800,00 €

**Évaluation du risque:** 🔴 ÉLEVÉ
⚠️ FACTURE EN RETARD DE 45 JOURS

**Recommandations:**
→ Envoyer une deuxième relance - contacter le client par téléphone
→ Escalader au service recouvrement
→ Envisager la suspension du compte client

**Historique client:**
- Total facturé (payé): 0,00 €
- Encours actuel: 1 800,00 €
- Nombre de factures: 1

⚠️ Ce client n'a jamais réglé de facture. C'est un nouveau client qui présente 
un risque élevé. Je recommande de contacter le client en priorité et d'envisager 
des mesures de recouvrement si aucune réponse n'est obtenue sous 48h.
```

### Trace d'audit générée

```
[2024-01-20 15:45:12] [AUDIT-START] MCP-1705761912-E5F6G7H8 
  | User: pierre.manager | Role: MANAGER | Capability: analyzeInvoice 
  | Params: {invoiceNumber=FAC-2024-000100}

[2024-01-20 15:45:12] [AUDIT-COMPLETE] MCP-1705761912-E5F6G7H8 
  | Capability: analyzeInvoice | Result: Facture analysée - Risque: ÉLEVÉ
```

---

## Exemple 3 : Résumé client avant rendez-vous

### Contexte
Commercial qui prépare un rendez-vous client.

### Conversation

```
👤 Utilisateur (Manager - Thomas):
J'ai un RDV avec Grand Groupe SA demain matin. Fais-moi un résumé complet.

🤖 Assistant IA:
Je prépare un résumé de l'activité de Grand Groupe SA pour votre rendez-vous.

[Appel MCP: summarizeCustomerActivity("CLI-004")]

=== Résumé Client: Grand Groupe SA ===

**Informations générales:**
- Code client: CLI-004
- Segment: VIP 🌟
- Statut: ACTIVE
- Contact: Sophie Bernard
- Email: sophie.bernard@grandgroupe.fr

**Activité commerciale:**
- Nombre total de commandes: 1
- Chiffre d'affaires total: 165 000,00 €

**Situation financière:**
- Nombre total de factures: 1
- Montant total payé: 100 000,00 €
- Montant en attente: 98 000,00 €
- Factures impayées: 1

**Dernières commandes:**
- CMD-20240105-GG001 | DELIVERED | 165 000,00 € | 05/01/2024

**Dernières factures:**
- FAC-2024-000125 | PARTIALLY_PAID | 198 000,00 € | Échéance: 19/02/2024

---
Résumé généré le: 2024-01-20T16:30:00

---

**📋 Points clés pour votre RDV:**

✅ **Points positifs:**
- Client VIP avec CA significatif (165k€)
- Relation commerciale établie
- 50% de la facture déjà réglée

⚠️ **Points d'attention:**
- Encours important (98k€) restant à régler
- Échéance de paiement dans 1 mois

💡 **Suggestions pour le RDV:**
1. Aborder le sujet du solde de facture de manière diplomatique
2. Proposer un échéancier si nécessaire (client VIP)
3. Explorer les opportunités de nouvelles commandes
4. Renforcer la relation (le client représente un CA important)
```

---

## Exemple 4 : Création de commande avec confirmation

### Contexte
Commercial qui crée une commande via l'assistant.

### Conversation

```
👤 Utilisateur (Manager - Thomas):
Crée une commande pour PME Innovation avec 2 modules Analytics à 3500€

🤖 Assistant IA:
Je prépare la commande. Laissez-moi d'abord vérifier que tout est en ordre.

[Appel MCP: createOrder(customerCode="CLI-002", lines=[...], confirmed=false)]

🔔 **Confirmation requise**

=== Confirmation de commande ===

Client: PME Innovation (CLI-002)
Adresse de livraison: 123 Rue de l'Entreprise, 75001 Paris

Articles:
- Module Analytics (PROD-004) x 2 @ 3 500,00 € = 7 000,00 €

Total HT: 7 000,00 €
TVA (20%): 1 400,00 €
Total TTC: 8 400,00 €

✅ La commande est valide et peut être créée.
Montant estimé: 7 000,00 €

⚠️ **Confirmez-vous la création de cette commande?**

[ID de corrélation: MCP-1705763500-I9J0K1L2]

---

👤 Utilisateur (Manager - Thomas):
Oui, je confirme

🤖 Assistant IA:
Parfait, je procède à la création de la commande.

[Appel MCP: createOrder(customerCode="CLI-002", lines=[...], confirmed=true)]

✅ **Commande créée avec succès!**

**Numéro de commande:** CMD-20240120-M3N4O5P6
**Client:** PME Innovation
**Montant total:** 7 000,00 € HT
**Statut:** En attente de validation

La commande a été créée et est en attente de validation par le service concerné. 
Le client sera notifié une fois la commande validée.

Souhaitez-vous que je fasse autre chose?
```

### Trace d'audit générée

```
[2024-01-20 17:05:00] [AUDIT-START] MCP-1705763500-I9J0K1L2 
  | User: thomas.commercial | Role: MANAGER | Capability: createOrder 
  | Params: {customerCode=CLI-002, confirmed=false}

[2024-01-20 17:05:00] [AUDIT-CONFIRM-REQUIRED] MCP-1705763500-I9J0K1L2 
  | Capability: createOrder | Action: Commande 7000€ pour PME Innovation

[2024-01-20 17:05:15] [AUDIT-CONFIRMATION] MCP-1705763500-I9J0K1L2 
  | Confirmed: true | By: thomas.commercial

[2024-01-20 17:05:15] [AUDIT-COMPLETE] MCP-1705763500-I9J0K1L2 
  | Capability: createOrder | Result: Commande créée: CMD-20240120-M3N4O5P6
```

---

## Exemple 5 : Accès refusé

### Contexte
Agent support qui essaie de créer une commande (non autorisé).

### Conversation

```
👤 Utilisateur (Support - Marie):
Crée une commande pour TechCorp avec 3 licences

🤖 Assistant IA:
[Appel MCP: createOrder(...)]

🚫 **Accès refusé**

Désolé, vous n'avez pas les droits nécessaires pour créer une commande.

Votre rôle (SUPPORT) vous permet de:
- ✅ Rechercher des commandes
- ✅ Analyser des factures
- ✅ Consulter l'activité des clients

Pour créer une commande, veuillez contacter:
- Un commercial (rôle MANAGER)
- Un administrateur (rôle ADMIN)

Puis-je vous aider avec autre chose dans le cadre de vos permissions?
```

### Trace d'audit générée

```
[2024-01-20 17:30:00] [AUDIT-ACCESS-DENIED] MCP-1705765800-Q7R8S9T0 
  | User: marie.support | Role: SUPPORT | Capability: createOrder
```

---

## Exemple 6 : Questions multiples orchestrées

### Contexte
Manager qui pose une question complexe nécessitant plusieurs appels MCP.

### Conversation

```
👤 Utilisateur (Manager - Pierre):
Donne-moi un aperçu complet de TechCorp : leurs commandes récentes et 
l'état de leurs factures.

🤖 Assistant IA:
Je vais rassembler ces informations pour vous.

[Appel MCP: summarizeCustomerActivity("CLI-001")]

Voici l'aperçu complet de TechCorp Solutions :

=== Résumé Client: TechCorp Solutions ===

**Informations générales:**
- Code client: CLI-001
- Segment: ENTERPRISE
- Contact: Jean Dupont
- Email: jean.dupont@techcorp.fr

**Activité commerciale:**
- Commandes totales: 2
- CA cumulé: 22 000,00 €

**Situation financière:**
- Factures totales: 1
- Payé: 21 000,00 €
- Encours: 0,00 €
✅ Aucune facture en retard

**Dernières commandes:**
- CMD-20240210-TC002 | IN_PREPARATION | 4 500,00 € | 10/02/2024
- CMD-20240115-TC001 | DELIVERED | 17 500,00 € | 15/01/2024

**Dernière facture:**
- FAC-2024-000123 | PAID | 21 000,00 €

---

**📊 Synthèse:**

TechCorp Solutions est un client ENTERPRISE en excellente santé financière :
- ✅ Paiement à jour (facture de 21k€ réglée)
- ✅ Relation commerciale active (nouvelle commande en préparation)
- ✅ Aucun impayé

Ce client représente un potentiel de développement intéressant. 
La commande en préparation (4 500€) devrait être livrée prochainement.
```

---

## Résumé des capacités utilisées

| Exemple | Capacité | Rôle | Confirmation |
|---------|----------|------|--------------|
| 1 | findOrder | SUPPORT | Non |
| 2 | analyzeInvoice | MANAGER | Non |
| 3 | summarizeCustomerActivity | MANAGER | Non |
| 4 | createOrder | MANAGER | **Oui** |
| 5 | createOrder | SUPPORT | Refusé |
| 6 | summarizeCustomerActivity | MANAGER | Non |

---

## Bonnes pratiques observées

1. **Réponses contextualisées** : L'IA adapte ses conclusions au contexte (RDV, support client, etc.)
2. **Audit systématique** : Chaque appel est tracé avec corrélation
3. **Confirmation explicite** : Les actions sensibles nécessitent validation
4. **Refus clair** : L'utilisateur comprend pourquoi et quoi faire
5. **Formatage lisible** : Les réponses sont structurées et actionnables
