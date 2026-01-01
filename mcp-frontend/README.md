# MCP Enterprise Frontend

Application Angular pour tester les fonctionnalités MCP (Model Context Protocol) via une interface utilisateur moderne.

## Fonctionnalités

- **Authentification** - Login avec gestion des rôles (SUPPORT, MANAGER, ADMIN)
- **Dashboard** - Vue d'ensemble avec capacités MCP disponibles
- **Commandes** - Recherche de commandes via MCP (`findOrder`)
- **Factures** - Analyse de factures via MCP (`analyzeInvoice`)
- **Clients** - Résumé d'activité client via MCP (`summarizeCustomerActivity`)
- **Assistant IA** - Chat avec l'IA pour interagir en langage naturel

## Prérequis

- Node.js 18+
- Angular CLI 21+
- Backend Spring Boot MCP démarré sur le port 8080

## Installation

```bash
npm install
```

## Démarrage

```bash
npm start
```

L'application sera accessible sur `http://localhost:4200/`.

Le proxy est configuré pour rediriger les appels `/api/*` vers le backend Spring Boot sur `http://localhost:8080`.

## Utilisateurs de démo

| Utilisateur | Mot de passe | Rôle    | Capacités                                      |
|-------------|--------------|---------|------------------------------------------------|
| support     | support123   | SUPPORT | findOrder, analyzeInvoice                      |
| manager     | manager123   | MANAGER | + summarizeCustomerActivity, createOrder       |
| admin       | admin123     | ADMIN   | + validateOrder (toutes les capacités)         |

## Structure du projet

```
src/app/
├── core/
│   ├── guards/         # Guards d'authentification et rôles
│   ├── interceptors/   # Intercepteur HTTP pour auth Basic
│   ├── models/         # Interfaces TypeScript
│   └── services/       # Services Auth et MCP
├── features/
│   ├── chat/           # Module assistant IA
│   ├── customers/      # Module clients
│   ├── dashboard/      # Dashboard principal
│   ├── invoices/       # Module factures
│   ├── layout/         # Navbar et layout principal
│   ├── login/          # Page de connexion
│   └── orders/         # Module commandes
└── app.routes.ts       # Configuration des routes
```

## Technologies

- Angular 21 (standalone components)
- Angular Material
- Angular Signals pour la réactivité
- RxJS pour les appels HTTP
- SCSS pour les styles

## Build production

```bash
npm run build
```

Les fichiers seront générés dans `dist/mcp-frontend/`.
