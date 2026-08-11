# eBank

[![Build](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/)
[![License](https://img.shields.io/badge/license-None-lightgrey)](LICENSE)
[![Tech Stack](https://img.shields.io/badge/stack-Java%2FReact%2FDocker-blueviolet)](https://github.com/)
[![Status](https://img.shields.io/badge/status-in_progress-yellow)](https://github.com/)

<p align="center">
  <strong>eBank</strong> est une application bancaire full-stack pour démonstration, avec authentification JWT, administration clients/comptes et un tableau de bord client moderne.
</p>

---

## 📌 Présentation & problème résolu

`eBank` permet de gérer un service bancaire léger en mode monorepo :

- un backend Java Spring Boot sécurisé,
- une interface React moderne,
- une base de données MySQL,
- un orchestrateur Docker Compose pour un démarrage rapide.

### Problématique

Aujourd'hui, de nombreuses applications bancaires de démonstration manquent d'une architecture claire et d'une séparation nette entre rôles métier. Ce projet offre :

- une gestion d'authentification centralisée,
- une interface cliente simple,
- une interface administrateur dédiée,
- une infrastructure prête pour un déploiement conteneurisé.

### Valeur ajoutée

- Simplifie l'approche monorepo (frontend + backend) pour un développement local rapide.
- Met en évidence la séparation des rôles `CLIENT` et `AGENT_GUICHET`.
- Offre des exemples concrets d'API REST, de JWT et de proxy front/backend via Nginx.

---

## ✨ Fonctionnalités clés

### Fonctionnalités globales

- Authentification utilisateur via JWT
- Sécurité des routes backend avec Spring Security
- Structure modulaire backend (DTO / entities / services / web)
- Application frontend responsive en React
- Déploiement local via Docker Compose

### Module Admin (`AGENT_GUICHET`)

- Création de clients
- Création de comptes bancaires
- Consultation des comptes d'un client par `clientId`
- Interface d'administration simple et rapide

### Module Client

- Connexion sécurisée
- Visualisation des comptes et soldes
- Formulaire de transfert d'argent (volonté front-end)
- Expérience utilisateur moderne et intuitive

### Infrastructures

- Base de données MySQL
- Conteneurs Docker pour backend, frontend et base de données
- Proxy Nginx pour la gestion du routage `/api`

---

## 🛠️ Stack technique

| Couche | Technologie | Description |
|---|---|---|
| Backend | ![Java](https://img.shields.io/badge/Java-17-blue) `Java 17` | Application Spring Boot REST sécurisée |
| Backend | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen) | Framework principal pour API et sécurité |
| Backend | ![Maven](https://img.shields.io/badge/Maven-3.8.0-red) | Gestion de dépendances et build |
| Database | ![MySQL](https://img.shields.io/badge/MySQL-8.1-orange) | Persistance des clients et comptes |
| Frontend | ![React](https://img.shields.io/badge/React-19.2.1-cyan) | Interface utilisateur SPA |
| Frontend | ![React Router](https://img.shields.io/badge/React%20Router-7.10.1-purple) | Navigation côté client |
| Frontend | ![Axios](https://img.shields.io/badge/Axios-1.13.2-blue) | Requêtes HTTP vers l'API |
| DevOps | ![Docker](https://img.shields.io/badge/Docker-24.0.5-blue) | Conteneurisation des services |
| DevOps | ![Docker Compose](https://img.shields.io/badge/Docker%20Compose-1.29.2-blue) | Orchestration multi-service |

---

## 🏗️ Architecture & structure du projet

```text
ebankk/
├── .env.example
├── docker-compose.yml
├── .gitignore
├── ebank-backend/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/ebank/
│   │   │   │   ├── config/         # Sécurité JWT, filtre et configuration Spring
│   │   │   │   ├── dto/            # Requêtes et réponses REST
│   │   │   │   ├── entities/       # Modèles JPA (Client, BankAccount, AppUser...)
│   │   │   │   ├── repositories/   # Accès aux données Spring Data JPA
│   │   │   │   ├── services/       # Logique métier
│   │   │   │   └── web/            # Contrôleurs REST
│   │   └── resources/
│   │       └── application.properties.example
│   └── target/
└── ebank-frontend/
    ├── Dockerfile
    ├── nginx.conf
    ├── package.json
    ├── public/
    └── src/
        ├── api/                    # Client Axios
        ├── components/             # Composants UI réutilisables
        ├── context/                # Gestion du contexte d'authentification
        ├── pages/                  # Pages React du site
        └── services/               # Services HTTP métier
```

### Architecture monorepo

- Le backend Spring Boot expose les API REST sous `/api/*`
- Le frontend React consomme ces API et gère les rôles
- Docker Compose assemble la base MySQL, le backend et le frontend
- Nginx sert le frontend et proxy ` /api/` vers le backend

---

## 🚀 Guide d'installation & démarrage rapide

### Prérequis

- Docker
- Docker Compose
- Java 17 (pour exécuter localement sans Docker)
- Node.js 20+ (pour le frontend local sans Docker)
- Maven (optionnel si vous utilisez le wrapper Maven)

### Configuration des variables d'environnement

1. Copier le modèle :

```bash
# Linux / macOS
cp .env.example .env

# Windows PowerShell
copy .env.example .env
```

2. Modifier `.env` avec vos valeurs réelles :

```env
MYSQL_ROOT_PASSWORD=ChangeMeRootPassword
MYSQL_DATABASE=ebank_db
MYSQL_USER=ebank
MYSQL_PASSWORD=ChangeMeDbPassword
APP_JWT_SECRET=ChangeMeJwtSecret
APP_JWT_EXPIRATION=3600000
BACKEND_PORT=8080
FRONTEND_PORT=3000
REACT_APP_API_BASE_URL=http://localhost:8080
```

### Démarrage via Docker Compose

```bash
docker compose up --build
```

Puis ouvrez `http://localhost:3000` dans votre navigateur.

### Démarrage local sans Docker

#### Backend

```bash
cd ebank-backend
./mvnw clean package
./mvnw spring-boot:run
```

#### Frontend

```bash
cd ebank-frontend
npm install
npm start
```

> Le frontend attend que `REACT_APP_API_BASE_URL` pointe vers le backend. Par défaut, la configuration dans `.env.example` est `http://localhost:8080`.

---

## 📊 Documentation de l'API

### Authentification

- `POST /api/auth/login`
  - Payload :

```json
{
  "username": "user@example.com",
  "password": "password"
}
```

  - Réponse :

```json
{
  "accessToken": "JWT_TOKEN",
  "username": "admin",
  "role": "AGENT_GUICHET",
  "expiresIn": 3600000
}
```

- `GET /api/auth/me`
  - Vérifie que le token JWT est valide.

### Client

- `GET /api/client/accounts`
  - Retourne la liste des comptes du client connecté.

### Admin

- `POST /api/admin/clients`
  - Exemple de payload :

```json
{
  "identityRef": "ID-12345",
  "firstname": "Jean",
  "lastname": "Dupont",
  "birthDate": "1990-05-12",
  "email": "jean.dupont@example.com",
  "address": "10 rue de Paris"
}
```

- `POST /api/admin/accounts`
  - Exemple de payload :

```json
{
  "clientId": 1,
  "rib": "FR7612345987650123456789014",
  "balance": 1200.50
}
```

- `GET /api/admin/clients/{clientId}/accounts`
  - Récupère les comptes associés à un client.

### Notes API

- Le backend utilise Spring Security pour protéger les routes :
  - `CLIENT` pour les routes `/api/client/**`
  - `AGENT_GUICHET` pour les routes `/api/admin/**`
- Le frontend React utilise Axios et stocke le JWT en `localStorage`.

---

## 🧪 Tests & qualité

### Tests backend

```bash
cd ebank-backend
./mvnw test
```

### Tests frontend

```bash
cd ebank-frontend
npm test
```

### Qualité

- Backend : tests Spring Boot + JUnit
- Frontend : tests React via `react-scripts`

> Attention : les tests de couverture ne sont pas explicitement définis dans le dépôt actuel, mais vous pouvez les ajouter via `mvn test` et `npm test`.

---

## 🗺️ Roadmap

- [ ] Ajouter la gestion des transferts côté backend (`/api/client/transfers`)
- [ ] Ajouter un dashboard administrateur avec statistiques globales
- [ ] Ajouter la pagination et l’historique des opérations de compte
- [ ] Ajouter des tests d’intégration API
- [ ] Ajouter une licence open-source formelle (`LICENSE`)
- [ ] Ajouter des instructions de déploiement cloud (AKS / Azure App Service)

---

## 🤝 Contribution

Vous voulez contribuer ? Voici les bonnes pratiques :

1. Forkez le projet.
2. Créez une branche dédiée : `feature/ma-fonctionnalite`.
3. Ouvrez une Pull Request détaillée.
4. Ajoutez des tests et vérifiez le build.

### Bonnes pratiques

- Respectez les conventions de nommage du frontend React.
- Évitez d’exposer des secrets dans le dépôt.
- Ajoutez un fichier `LICENSE` si vous souhaitez ouvrir le projet.

---

## 📄 Licence

Aucune licence explicite n’a été trouvée dans ce dépôt. Pour un usage open-source sécurisé, ajoutez un fichier `LICENSE` adapté, par exemple `MIT`.

---

## 🔎 Remarques finales

`eBank` est un bon point de départ pour apprendre la création d’une application bancaire full-stack, avec une architecture monorepo fonctionnelle et un pipeline de développement simple.

Pour aller plus loin, ajoutez la gestion complète des transferts, la journalisation des opérations, et une couche de tests d’intégration.
