# AppRH — Plateforme de gestion de projets, tâches, congés et opérations RH

Plateforme web unifiée (gestion de projets + RH) avec recommandation IA d'attribution de tâches.
Projet de stage de fin d'études (3 mois, solo). Stack : Java 21 / Spring Boot 4 · Angular 22 · PostgreSQL · Docker · FastAPI.

## Architecture

```
┌────────────┐   /api    ┌─────────────┐    ┌──────────────┐
│  Frontend  │ ────────► │   Backend   │ ──►│  PostgreSQL  │
│  Angular   │   (nginx) │ Spring Boot │    └──────────────┘
│ + Nginx    │           │ (monolithe  │
└────────────┘           │  modulaire) │ ──► ┌────────────┐
                         └─────────────┘     │  ia-service│
                                             │  FastAPI   │
                                             └────────────┘
```

- **backend/** — monolithe modulaire Spring Boot, modules métier par domaine (auth, users, employees, departments, skills, projects, tasks, leaves, notifications, dashboard, ai, audit, settings), chaque module organisé en couches api / application / domain / infrastructure.
- **frontend/** — SPA Angular 22 (standalone, signaux), Angular Material, garde d'authentification, layout sidenav.
- **ia-service/** — microservice FastAPI, moteur de scoring pondéré pour la recommandation d'attribution de tâches (Sprint 5).
- **docs/** — documentation du projet (cahier des charges, UML, MCD/MLD).

## Démarrage rapide (Docker)

```bash
docker compose up --build
```

| Service     | URL                          |
|-------------|------------------------------|
| Frontend    | http://localhost:4200        |
| API Swagger | http://localhost:8081/api/swagger-ui.html |
| Actuator    | http://localhost:8081/api/actuator/health |
| IA Service  | http://localhost:8000/docs   |

Arrêt : `docker compose down` (ajouter `-v` pour supprimer le volume PostgreSQL).

## Développement local (sans Docker)

- **Backend** : `.\mvnw.cmd spring-boot:run` (Java 21 ; PostgreSQL requis, variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- **Frontend** : `npm start` puis http://localhost:4200 (l'API est appelée sur `http://localhost:8080/api`).
- **IA** : `uvicorn app.main:app --reload --port 8000` (Python 3.12, `pip install -r requirements.txt`).

## Configuration

Toutes les valeurs sensibles passent par variables d'environnement (voir `application.yml` et `docker-compose.yml`) :
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `IA_SERVICE_URL`.

## Planning des sprints

| Sprint | Semaines | Contenu |
|--------|----------|---------|
| 0 | S1–S2 | Cadrage, maquettes, UML, MCD/MLD, squelettes (ce dépôt) |
| 1 | S3–S4 | Authentification JWT, utilisateurs, rôles, permissions |
| 2 | S5–S6 | Employés, départements, compétences, projets |
| 3 | S7–S8 | Tâches, Kanban, calendrier |
| 4 | S9–S10 | Congés, notifications, dashboard |
| 5 | S11 | Module IA (recommandation d'attribution) |
| 6 | S12 | Tests, sécurisation, documentation, packaging, soutenance |

## Conventions

- Commits : [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `refactor:`, ...).
- Branches : `main` (stable) / `develop` / `feature/*`.
