# Audit de sécurité — module Assistant RH IA

Périmètre : le chatbot IA (backend Spring `ia/*`, `ia-service` FastAPI, widget
Angular) et ses interactions avec l'authentification/RBAC existants.

## Synthèse

| Domaine              | État | Points clés |
|----------------------|------|-------------|
| Authentification      | OK  | JWT Bearer, filtre dédié, contexte sans session |
| Autorisation (RBAC)   | OK  | Contexte RH scopé à l'identité du JWT, jamais au corps |
| Validation d'entrée   | OK  | Bornes de taille + rôle d'historique restreint |
| Limite de débit       | OK  | Fenêtre glissante par utilisateur sur les endpoints LLM |
| CORS                  | Corrigé | Origines explicites, credentials désactivés |
| Persistance           | Renforcée | Troncature + écriture non bloquante pour la réponse |
| Secrets               | Durci | Avertissement au démarrage si secret par défaut |
| Injection de prompt   | Traité | Prompt système durci + contexte en lecture seule |

## Constats et correctifs (étape 10a)

| # | Constat | Risque | Correctif |
|---|---------|--------|-----------|
| 1 | CORS `allowedOriginPatterns("*")` + `allowCredentials(true)` | Un site tiers pouvait émettre des requêtes credentialed | Origines explicites via `app.cors.allowed-origins` (défaut `http://localhost:4200`), `allowCredentials(false)` (auth par en-tête Bearer, sans cookie) |
| 2 | Historique persisté sans borne sur le texte assemblé | `DataIntegrityViolation` (VARCHAR(4000)) pouvant transformer une réussite en repli | Troncature à 4000 caractères dans `ChatHistoryService` |
| 5. Persistance couplée à la réponse | Une erreur d'écriture déclenchait le repli « IA indisponible » | `ChatbotService.persist(...)` isole l'écriture (journalisée, non bloquante) |
| 5b. Rate-limit sur tout `/chatbot/**` | Les lectures d'historique consommaient le quota | Interceptor limité à `/chatbot/chat` et `/chatbot/chat/stream` |
| 6. Secret JWT par défaut silencieux | Risque de déploiement avec le secret de dev | Avertissement au démarrage si `JWT_SECRET` absent |

## Mesures en place (audit)

- **Authentification / RBAC** : toutes les routes `/chatbot/**` exigent un JWT
  (`@PreAuthorize("isAuthenticated()")`). L'identité provient exclusivement de
  `UserPrincipal` (JWT), jamais du corps de requête. Le contexte RH est construit
  côté serveur (`ChatContextService`) et strictement limité aux données de
  l'utilisateur (`findByUserIdAndDeletedAtIsNull`, `findByEmployeeIdAndDeletedAtIsNull`).
- **Anti-injection de prompt** : prompt système durci (`ia-service/app/chat/prompts.py`)
  interdisant d'inventer des données, de suivre des instructions du message, ou de
  révéler des champs techniques. Le contexte est présenté comme source de vérité en lecture seule.
- **Validation d'entrée** : message ≤ 2000 caractères ; historique ≤ 12 messages,
  rôle restreint à `user|assistant`, contenu ≤ 4000 ; rejet 400 sinon. Côté
  ia-service, mêmes bornes via Pydantic.
- **Limite de débit** : fenêtre glissante par utilisateur (`ChatbotRateLimiter`),
  30 req/60 s par défaut, HTTP 429 `CHATBOT_RATE_LIMITED` au-delà.
- **CORS** : origines explicitement autorisées (config `CORS_ALLOWED_ORIGINS`),
  `allowCredentials=false` (jeton Bearer en en-tête, pas de cookie).
- **Confidentialité des logs** : jamais le contenu utilisateur ; seuls id, rôle et
  longueur sont journalisés.
- **Secrets** : `.env` exclu du git, `.env.example` fourni ; avertissement si le
  secret JWT de développement est utilisé.
- **Métriques** : exposées via Actuator, accessibles uniquement aux utilisateurs
  authentifiés.

## Risques résiduels et recommandations (production)

| Élément | Statut | Recommandation |
|---------|--------|----------------|
| ia-service sans authentification | Accepté (réseau Docker interne, non exposé) | Restreindre le réseau / ajouter un jeton interne si exposé |
| Jeton JWT en `localStorage` | Accepté (SPA) | Mitigé par l'échappement Markdown + sanitisation Angular ; envisager des cookies `httpOnly` sinon |
| Actuator `metrics` lisible par tout utilisateur connecté | Accepté | Le limiter au rôle ADMIN si nécessaire |
| Cold start Ollama (~20 s) | Connu | `OLLAMA_KEEP_ALIVE=24h`, warm-up au démarrage |
| Secret JWT par défaut | Avertissement | Imposer un secret fort (32+ car.) avant mise en production |
