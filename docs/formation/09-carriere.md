# Chapitre 9 : Positionnement carrière

## 🎯 Objectifs du chapitre

- Savoir valoriser MCP sur un CV
- Préparer les réponses d'entretien
- Se différencier des autres candidats Java

---

## 9.1 Pourquoi MCP est un atout carrière

### Le contexte marché 2024-2025

```
┌─────────────────────────────────────────────────────────────────┐
│                    TENDANCES DU MARCHÉ                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 📈 Explosion de l'IA générative en entreprise                   │
│    → Besoin de développeurs sachant intégrer l'IA au SI         │
│                                                                  │
│ 🔐 Exigences de sécurité croissantes                            │
│    → L'IA doit être contrôlée, auditée, gouvernée               │
│                                                                  │
│ 🏢 Transformation digitale des DSI                               │
│    → Recherche de profils "pont" entre dev et architecture      │
│                                                                  │
│ 💰 Valorisation salariale                                        │
│    → +15-25% pour les profils IA/ML vs Java classique           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Ce que MCP démontre

| Compétence | Ce que ça prouve |
|------------|------------------|
| **Architecture** | Vous savez concevoir des systèmes complexes |
| **Sécurité** | Vous pensez "enterprise-ready" |
| **Innovation** | Vous êtes à jour sur les technologies IA |
| **Pragmatisme** | Vous savez intégrer l'IA sans la subir |
| **Vision** | Vous comprenez les enjeux DSI |

---

## 9.2 Comment en parler sur un CV

### Section "Compétences techniques"

```
┌─────────────────────────────────────────────────────────────────┐
│ COMPÉTENCES TECHNIQUES                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ Backend                                                          │
│ • Java 17+, Spring Boot 3.x, Spring Security                    │
│ • Spring AI, MCP (Model Context Protocol)                       │
│ • Architecture hexagonale, DDD                                   │
│                                                                  │
│ IA & Intégration                                                 │
│ • Intégration LLM en SI d'entreprise (MCP Server)               │
│ • Sécurisation des interactions IA (RBAC, audit)                │
│ • OpenAI API, Claude API via Spring AI                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Section "Expériences" - Exemple de formulation

```
┌─────────────────────────────────────────────────────────────────┐
│ DÉVELOPPEUR JAVA / INTÉGRATION IA                                │
│ Entreprise XYZ | 2024 - Présent                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ Conception et développement d'un MCP Server pour l'intégration  │
│ d'un assistant IA dans le SI de gestion commerciale :           │
│                                                                  │
│ • Architecture MCP avec Spring Boot et Spring AI                │
│ • Exposition de 4 capacités métier contrôlées                   │
│ • Implémentation RBAC (3 rôles, 7 capacités)                    │
│ • Système d'audit complet avec traçabilité                      │
│ • Workflow de confirmation pour les actions sensibles           │
│                                                                  │
│ Résultats :                                                      │
│ • Réduction de 95% du temps de recherche d'information          │
│ • 0 incident de sécurité sur les interactions IA                │
│ • Adoption par 50 utilisateurs (support, commerciaux)           │
│                                                                  │
│ Stack : Java 17, Spring Boot 3.2, Spring AI, H2/PostgreSQL      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Mots-clés à inclure

- MCP (Model Context Protocol)
- Spring AI
- LLM Integration
- IA en entreprise
- Gouvernance IA
- RBAC / Sécurité IA
- Audit des interactions IA

---

## 9.3 Questions d'entretien et réponses

### Question 1 : "Qu'est-ce que MCP ?"

**❌ Mauvaise réponse :**
> "C'est un protocole pour connecter l'IA à des APIs"

**✅ Bonne réponse :**
> "MCP, Model Context Protocol, est un protocole standardisé qui définit comment un LLM peut interagir de manière **contrôlée** avec un système d'information. Contrairement à une API REST classique, MCP expose des **capacités métier** avec des descriptions sémantiques que l'IA peut comprendre. L'avantage clé est la **gouvernance** : on contrôle précisément ce que l'IA peut faire, on audite chaque appel, et on peut exiger des confirmations pour les actions sensibles."

---

### Question 2 : "Pourquoi ne pas simplement donner accès à l'API REST à l'IA ?"

**✅ Bonne réponse :**
> "Plusieurs raisons :
> 1. **Découverte** : L'IA ne peut pas deviner les endpoints REST. MCP fournit des descriptions en langage naturel.
> 2. **Sécurité** : Une API REST a souvent trop de endpoints. MCP expose le strict nécessaire.
> 3. **Format** : Les réponses REST sont techniques (JSON). MCP retourne du texte exploitable par l'IA.
> 4. **Gouvernance** : MCP intègre nativement les notions de rôles et d'audit, pas une API REST classique.
> 
> En résumé, MCP est conçu pour l'IA, REST est conçu pour les applications."

---

### Question 3 : "Comment sécurisez-vous les interactions IA ?"

**✅ Bonne réponse :**
> "J'applique plusieurs niveaux de sécurité :
> 1. **RBAC** : Chaque utilisateur a un rôle (SUPPORT, MANAGER, ADMIN) avec des capacités définies
> 2. **Validation** : Toutes les entrées sont validées côté serveur, jamais confiance aux données de l'IA
> 3. **Confirmation** : Les actions qui modifient des données nécessitent une confirmation explicite
> 4. **Audit** : Chaque appel est tracé avec un ID de corrélation, l'utilisateur, le rôle, les paramètres et le résultat
> 5. **Isolation** : L'IA n'a jamais accès direct à la base de données, tout passe par les services métiers"

---

### Question 4 : "Donnez un exemple concret d'utilisation"

**✅ Bonne réponse :**
> "Dans le projet que j'ai développé, un agent support peut demander à l'assistant IA : 'Montre-moi la commande CMD-123'. L'IA analyse la demande, appelle la capacité MCP `findOrder`, qui :
> 1. Vérifie que l'utilisateur a le rôle SUPPORT
> 2. Trace l'appel dans l'audit
> 3. Appelle le service métier
> 4. Formate la réponse de manière lisible
> 5. Trace le succès
> 
> L'utilisateur obtient une réponse complète en quelques secondes au lieu de naviguer dans 3 applications différentes."

---

### Question 5 : "Comment MCP se positionne par rapport à LangChain ou autres frameworks ?"

**✅ Bonne réponse :**
> "MCP et LangChain ne sont pas au même niveau :
> - **LangChain** est un framework d'orchestration côté client/LLM pour chaîner des appels
> - **MCP** est un protocole côté serveur pour exposer des capacités métier
> 
> Ils sont complémentaires : LangChain peut utiliser MCP comme source de tools. Dans un contexte entreprise, je préfère MCP car il permet une gouvernance centralisée côté serveur, indépendante du framework d'orchestration choisi."

---

### Question 6 : "Quels sont les risques de l'IA en entreprise et comment MCP les adresse ?"

**✅ Bonne réponse :**
> "Les principaux risques :
> 
> | Risque | Solution MCP |
> |--------|--------------|
> | Accès non contrôlé | RBAC par capacité |
> | Actions non souhaitées | Confirmation obligatoire |
> | Pas de traçabilité | Audit systématique |
> | Données exposées | Capacités minimales, pas d'accès DB |
> | Hallucinations | Validation des données côté serveur |
> 
> MCP permet de bénéficier de l'IA tout en gardant le contrôle."

---

## 9.4 Se différencier des autres candidats

### Le profil "développeur Java classique"

```
❌ Profil commun :
- Java, Spring Boot
- API REST
- Base de données
- Tests unitaires
```

### Le profil "développeur Java + IA enterprise"

```
✅ Profil différenciant :
- Java, Spring Boot, Spring AI
- API REST + MCP Server
- Base de données + Gouvernance IA
- Tests unitaires + Audit des interactions IA
- Sécurité applicative + RBAC pour l'IA
```

### Les questions à poser en entretien

Montrez votre expertise en posant des questions pertinentes :

1. "Avez-vous déjà intégré un LLM dans votre SI ? Quelles problématiques avez-vous rencontrées ?"
2. "Comment gérez-vous la gouvernance des interactions IA aujourd'hui ?"
3. "Quel est votre niveau d'audit sur les actions déclenchées par l'IA ?"
4. "Avez-vous une stratégie de sécurisation des accès IA ?"

---

## 9.5 Évolution vers architecte

### Compétences démontrées

```
┌─────────────────────────────────────────────────────────────────┐
│            COMPÉTENCES ARCHITECTE DÉMONTRÉES PAR MCP             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 🏗️ Conception de systèmes                                       │
│    • Architecture en couches                                     │
│    • Séparation des responsabilités                              │
│    • Design patterns (Adapter, Facade, Chain of Responsibility)  │
│                                                                  │
│ 🔐 Sécurité                                                      │
│    • RBAC, moindre privilège                                     │
│    • Defense in depth                                            │
│    • Audit et traçabilité                                        │
│                                                                  │
│ 📊 Gouvernance                                                   │
│    • Contrôle des accès                                          │
│    • Conformité (RGPD, SOX)                                      │
│    • Documentation et explicabilité                              │
│                                                                  │
│ 🔮 Vision technologique                                          │
│    • Intégration de nouvelles technologies (IA)                  │
│    • Évaluation des risques                                      │
│    • Pragmatisme (solution vs hype)                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Parcours recommandé

```
Développeur Java
      │
      ▼
Développeur Java Senior + Spring AI/MCP
      │
      ▼
Tech Lead avec expertise IA
      │
      ▼
Architecte Solutions (spécialité IA en entreprise)
      │
      ▼
Architecte Enterprise / CTO
```

---

## 9.6 Certifications et formations complémentaires

### Recommandations

| Certification | Pertinence | Difficulté |
|---------------|------------|------------|
| Spring Professional | ⭐⭐⭐⭐⭐ | Moyenne |
| AWS Solutions Architect | ⭐⭐⭐⭐ | Élevée |
| Google Cloud Professional ML | ⭐⭐⭐ | Élevée |
| Kubernetes (CKA) | ⭐⭐⭐ | Élevée |

### Veille technologique

- **Anthropic** : Évolutions de MCP
- **Spring AI** : Nouvelles fonctionnalités
- **OpenAI / Google** : Évolutions des modèles
- **Hugging Face** : Modèles open source

---

## 📝 Points clés à retenir

1. **MCP = différenciation** sur le marché Java
2. **Valorisez les résultats** : temps gagné, sécurité, adoption
3. **Préparez les réponses** aux questions types
4. **Montrez la vision** : pas juste le code, l'architecture
5. **Évolution naturelle** vers architecte

---

## 🎯 Quiz de validation

1. Comment présenteriez-vous MCP en une phrase sur un CV ?
2. Quelle est LA différence clé entre MCP et une API REST ?
3. Comment MCP démontre-t-il des compétences d'architecte ?
4. Quelle question poseriez-vous en entretien pour montrer votre expertise ?

---

[← Chapitre précédent](./08-erreurs-antipatterns.md) | [Chapitre suivant →](./10-exercices.md)
