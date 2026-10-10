# Assistant RH IA (chatbot)

Assistant conversationnel intégré à AppRH : un widget flottant côté Angular qui
interroge le backend Spring, lequel relaie vers un microservice FastAPI
(`ia-service`) adossé à un modèle **Ollama local** (`llama3.2:3b`). Aucun service
payant, aucune donnée envoyée à l'extérieur.

## Flux

```
Widget Angular ──► Backend Spring ──► ia-service (FastAPI) ──► Ollama (local)
   (NDJSON)          /api/chatbot/*      /api/v1/chat*
```

1. Le frontend envoie `POST /api/chatbot/chat/stream` avec le message et
   l'historique récent (jeton JWT en en-tête).
2. Le backend construit un **contexte RH scopé à l'utilisateur** à partir du JWT
   (identité, congés, tâches, projets) — le LLM ne décide jamais des droits.
3. L'ia-service assemble le prompt (contexte + historique) et interroge Ollama.
4. La réponse est renvoyée en flux **NDJSON** (un objet JSON par ligne) et
   affichée progressivement dans le widget.

## Endpoints (backend)

| Méthode | Chemin                     | Réponse                                  |
|---------|----------------------------|------------------------------------------|
| POST    | `/api/chatbot/chat`        | `{ "reply": string, "aiAvailable": bool }` |
| POST    | `/api/chatbot/chat/stream` | Flux NDJSON `application/x-ndjson`        |
| GET     | `/api/chatbot/messages`    | Historique serveur `[{ "role", "content" }]` |
| DELETE  | `/api/chatbot/messages`    | `204 No Content` (efface l'historique)     |

Corps de requête (identique pour les deux) :

```json
{
  "message": "Combien de congés approuvés ai-je ?",
  "history": [
    { "role": "user", "content": "Bonjour" },
    { "role": "assistant", "content": "Bonjour, comment puis-je aider ?" }
  ]
}
```

Événements du flux : `{"type":"token","text":"…"}`, `{"type":"done"}`,
`{"type":"fallback","text":"…"}`.

## Endpoints (ia-service)

| Méthode | Chemin                | Rôle                       |
|---------|-----------------------|----------------------------|
| POST    | `/api/v1/chat`        | Réponse complète           |
| POST    | `/api/v1/chat/stream` | Réponse en flux NDJSON     |

## RBAC et confidentialité

- L'identité provient **toujours** du JWT (`UserPrincipal`), jamais du corps de
  requête. Un utilisateur ne peut obtenir que ses propres données.
- Le contexte est filtré par rôle et par appartenance avant d'être envoyé au LLM.
- Le contenu utilisateur n'est **jamais journalisé** (seuls l'id, le rôle et la
  longueur le sont).
- Le prompt système interdit au modèle d'inventer des données, d'exposer des
  champs techniques ou de suivre des instructions d'injection.

## Réponses déterministes

Les questions de comptage (congés, tâches, projets) sont interceptées par
`ChatbotQuickAnswerService` : la réponse est calculée directement en base et le
LLM est court-circuité. Cela garantit des chiffres exacts. Si la question
demande aussi un détail (« lesquels », « liste »), elle est laissée au LLM.

## Persistance et observabilité

- **Persistance serveur** : chaque échange (question + réponse) est enregistré
  dans la table `chat_message` (migration Flyway `V6`), rattaché à l'utilisateur
  du JWT. `GET /api/chatbot/messages` renvoie les 40 derniers messages ;
  `DELETE /api/chatbot/messages` efface l'historique de l'utilisateur.
- **Observabilité** : métriques Micrometer exposées sur `/api/actuator/metrics` :
  - `chatbot.requests` (tags `surface` = `chat` | `stream`, `mode` = `deterministic` | `llm` | `fallback`) ;
  - `chatbot.latency` (tag `surface`) ;
  - `chatbot.rate.limited` (compteur des requêtes rejetées en HTTP 429).

## Limites et validation

| Limite                         | Valeur                                   |
|--------------------------------|------------------------------------------|
| Taille du message              | 2 000 caractères                         |
| Historique transmis            | 12 messages max (4000 car. chacun)       |
| Historique utilisé par le LLM  | 8 derniers échanges                      |
| Débit par utilisateur          | 30 appels LLM / 60 s (HTTP 429 au-delà)  |

## Configuration (variables d'environnement)

| Variable                            | Défaut                   | Description                       |
|-------------------------------------|--------------------------|-----------------------------------|
| `CHATBOT_IA_SERVICE_URL`            | `http://localhost:8000`  | URL de l'ia-service               |
| `CHATBOT_OLLAMA_MODEL`              | `llama3.2:3b`            | Modèle Ollama                     |
| `CHATBOT_TIMEOUT_MS`                | `30000`                  | Timeout backend → ia-service      |
| `CHATBOT_RATE_LIMIT_MAX`            | `30`                     | Appels LLM max par fenêtre        |
| `CHATBOT_RATE_LIMIT_WINDOW_SECONDS` | `60`                     | Taille de la fenêtre (s)          |
| `CORS_ALLOWED_ORIGINS`              | `http://localhost:4200`  | Origines CORS autorisées (liste)  |
| `OLLAMA_URL`                        | `http://ollama:11434`    | URL Ollama (ia-service)           |
| `OLLAMA_MODEL`                      | `llama3.2:3b`            | Modèle (ia-service)               |
| `OLLAMA_KEEP_ALIVE`                 | `24h`                    | Durée de maintien du modèle chargé|
| `CHATBOT_TIMEOUT_SECONDS`           | `80`                     | Timeout ia-service → Ollama       |

## Comportement de repli

Si l'ia-service est indisponible, le backend renvoie un message de repli
(`aiAvailable: false`), et le widget affiche un bandeau « Service IA momentanément
indisponible ». Les réponses déterministes fonctionnent même dans ce cas.

## Tests

Backend (JUnit + Testcontainers) :

- `ChatbotIntegrationTest` : authentification, validation, historique,
  réponses déterministes, repli, flux NDJSON, persistance, métriques.
- `ChatbotRateLimitIntegrationTest` : renvoi HTTP 429 au-delà de la limite.

```bash
cd backend
.\mvnw.cmd test
```

Frontend (vitest via `@angular/build:unit-test`) :

```bash
cd frontend
npx ng test --watch=false
```

- `markdown.pipe.spec.ts` : rendu Markdown (échappement, listes, emphase, code).
- `chatbot.service.spec.ts` : `ask`, `loadHistory`, `clearHistory`, flux NDJSON.
- `chatbot.spec.ts` : restauration de session, envoi, réponse diffusée, effacement.

## Déploiement

1. Copier la configuration : `Copy-Item .env.example .env` puis renseigner les
   valeurs (voir `README.md` → *Configuration*).
2. Construire et démarrer la pile : `docker compose up -d --build`.
3. La pile est prête lorsque tous les services sont `healthy` :
   `docker compose ps`. Des **healthchecks** couvrent `db`, `ollama`,
   `ia-service` (`/health`), `backend` (`/api/actuator/health`) et `frontend`
   (`/`), et `depends_on` conditionne le démarrage à leur bon état.
4. Interface : http://localhost:4200 — API : http://localhost:8081/api.
5. Démonstration automatisée : `.\scripts\demo-chatbot.ps1`
   (ajouter `-IncludeRateLimit` pour tester le HTTP 429).

Voir aussi : [`module-ia-synthese.md`](module-ia-synthese.md) (synthèse) et
[`securite-ia.md`](securite-ia.md) (audit sécurité).

## Dépannage

| Symptôme                              | Cause / remède                                                |
|---------------------------------------|---------------------------------------------------------------|
| Première réponse très lente (~20 s)   | Chargement du modèle (cold start CPU), normal une seule fois. |
| HTTP 429                              | Limite de débit atteinte ; patienter ou ajuster les variables.|
| `aiAvailable: false`                  | ia-service ou Ollama arrêté ; vérifier les conteneurs.        |
| Réponses imprécises                   | Privilégier les questions de comptage (déterministes).        |
| Modèle absent                         | `docker exec apprh-ollama ollama pull llama3.2:3b`.           |
