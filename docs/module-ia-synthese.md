# Module IA — synthèse (soutenance)

Rapport de synthèse du module d'intelligence artificielle d'AppRH : assistant
conversationnel RH 100 % local (aucun service payant, aucune donnée envoyée à
l'extérieur) et moteur de recommandation d'attribution.

## 1. Objectif et périmètre

- **Assistant RH IA** : widget flottant Angular permettant à un utilisateur
  connecté d'interroger ses congés, tâches et projets en langage naturel.
- **Base de connaissances (RAG)** : recherche documentaire **100 % locale** (BM25)
  sur les politiques RH, avec citation des sources affichée dans le widget.
- **Garde-fous** : le LLM ne décide jamais des droits ; il n'accède qu'au
  contexte RH de l'utilisateur courant, construit côté serveur à partir du JWT.
- **Hors périmètre** : exécution d'actions (tools), mémoire globale inter-utilisateurs.

## 2. Architecture

```
Widget Angular ──► Backend Spring ──► ia-service (FastAPI) ──► Ollama (local, llama3.2:3b)
   (NDJSON)          /api/chatbot/*      /api/v1/chat*              ▲
                        │                    │                     │
                        │                    └─ RAG BM25 ───────────┘
                        │                       (data/hr_docs/*.md)
                        └─ PostgreSQL (chat_message) + Actuator (métriques)
```

Composants :
- `ia/api` : `ChatbotController` (REST + flux), `ChatRequest`/`ChatResponse`,
  `ChatMessageDto`, `ChatbotRateLimitInterceptor`.
- `ia/application` : `ChatbotService` (orchestration), `ChatbotQuickAnswerService`
  (réponses déterministes), `ChatContextService` (contexte scopé), `ChatHistoryService`
  (persistance).
- `ia/infrastructure` : `ChatbotClient` (HTTP + streaming NDJSON), `ChatbotRateLimiter`,
  `ChatbotMetrics`, `ChatMessageRepository`, `ChatbotProperties`.
- `ia-service` (Python/FastAPI) : `app/api/chat.py`, `app/chat/{prompts,ollama}.py`,
  `app/rag/*` (corpus + index BM25). Corpus : `data/hr_docs/*.md`.
- Frontend : `features/chatbot/*` (widget, pipe Markdown), `core/services/chatbot.service.ts`.

## 3. Fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| Réponses déterministes | Questions de comptage (congés, tâches, projets) calculées en base, LLM court-circuité → chiffres exacts. |
| Base de connaissances (RAG) | Questions de politique enrichies par les extraits RH les plus pertinents (BM25 local), sources citées dans le widget. |
| Conversation multi-tours | Historique (8 derniers messages) réinjecté dans le prompt. |
| Streaming temps réel | Réponse diffusée en NDJSON et affichée progressivement. |
| Persistance serveur | Chaque échange stocké dans `chat_message` (Flyway V6), scopé au JWT ; `GET`/`DELETE /chatbot/messages`. |
| UX | Historique de session, bouton « Effacer », suggestions, arrêt de génération, copie, rendu Markdown. |
| Accessibilité | Focus auto, `role="log" aria-live`, libellés ARIA, retour du focus. |
| Repli gracieux | ia-service indisponible → message de repli, `aiAvailable=false`, bandeau UI. |
| Observabilité | Métriques Micrometer sur `/api/actuator/metrics`. |

## 4. Sécurité et RBAC

Détail complet dans [`securite-ia.md`](securite-ia.md) : identité issue du JWT,
contexte scopé, anti-injection, validation, limite de débit (30/60 s),
CORS restreint, secrets externalisés.

## 5. API (backend)

| Méthode | Chemin | Réponse |
|---------|--------|---------|
| POST | `/api/chatbot/chat` | `{ reply, aiAvailable, sources }` |
| POST | `/api/chatbot/chat/stream` | Flux NDJSON `application/x-ndjson` (+ événement `sources`) |
| GET | `/api/chatbot/messages` | 40 derniers messages de l'utilisateur |
| DELETE | `/api/chatbot/messages` | `204` |

## 6. Tests et couverture

| Périmètre | Résultat |
|-----------|----------|
| Backend (`.\mvnw.cmd test`) | **56 tests**, 11 classes, 0 échec |
| Frontend (`npx ng test --watch=false`) | **25 tests**, 5 fichiers, 0 échec |
| ia-service (`pytest`) | **7 tests** (RAG), 0 échec |
| Couverture backend (JaCoCo) | **78,2 %** lignes · 47,3 % branches |

Rapport de couverture : `backend/target/site/jacoco/index.html`.
Tests notables : réponses déterministes conformes à la base, repli, flux NDJSON,
persistance/effacement, RBAC (401), limite de débit (429), métriques.

## 7. Déploiement et démonstration

1. `Copy-Item .env.example .env` puis renseigner les valeurs.
2. `docker compose up -d --build` (healthchecks sur db, ollama, ia-service,
   backend, frontend).
3. Démo automatisée de bout en bout :

```powershell
.\scripts\demo-chatbot.ps1
# variante avec test de la limite de débit :
.\scripts\demo-chatbot.ps1 -IncludeRateLimit
```

4. Interface : http://localhost:4200 — API : http://localhost:8081/api.

## 8. Limites et pistes d'évolution

- Cold start Ollama ~20 s (atténué par `OLLAMA_KEEP_ALIVE=24h` et le
  préchargement automatique `OLLAMA_WARMUP=true`).
- Qualité limitée de `llama3.2:3b` sur CPU → mitigée par le mode déterministe.
- Évolutions : exécution d'actions (tools) avec confirmation utilisateur,
  embeddings pour affiner la recherche documentaire, évaluation automatique de la
  qualité des réponses.
