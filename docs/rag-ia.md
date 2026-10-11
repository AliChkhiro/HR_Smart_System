# Assistant RH — base de connaissances (RAG)

L'assistant RH s'appuie sur une **base de connaissances documentaire locale**
(RAG, *Retrieval-Augmented Generation*). Les questions de politique ou de
procédure (télétravail, congés, frais, intégration, sécurité) sont enrichies par
les extraits de documents RH les plus pertinents avant l'appel au modèle.

## Principe

1. Le corpus RH est constitué de fichiers Markdown (`ia-service/data/hr_docs/*.md`).
2. Chaque document est découpé en **sections** (titres `##`).
3. Un index **BM25** (implémentation Python interne, sans dépendance externe) note
   chaque section selon la requête et retourne les 3 extraits les plus pertinents.
4. Les extraits sont ajoutés au prompt sous la forme d'une section
   « Documentation RH », et le modèle est invité à **citer les titres** utilisés.
5. Les **sources** (titres des documents) sont remontées jusqu'au widget, qui les
   affiche sous la réponse.

Le RAG est **100 % local** : aucune donnée n'est envoyée à un service tiers, et
aucun modèle d'embeddings supplémentaire n'est nécessaire (recherche lexicale
BM25).

## Corpus fourni

| Document                              | Contenu                          |
|---------------------------------------|----------------------------------|
| `conges.md`                           | Types de congés, validation, solde |
| `teletravail.md`                      | Éligibilité, rythme, équipement  |
| `frais.md`                            | Dépenses remboursables, plafonds |
| `onboarding.md`                       | Parcours d'intégration, accès    |
| `securite.md`                         | RGPD, contrôle d'accès, IA       |

## Ajouter un document

Déposer un fichier Markdown dans `ia-service/data/hr_docs/`, avec un titre `# `
et des sections `## `. L'index est reconstruit au démarrage du service (aucune
migration nécessaire).

La racine du corpus est configurable via la variable d'environnement
`HR_DOCS_DIR` (défaut : `data/hr_docs`).

## Endpoints concernés

- `POST /api/v1/chat` : réponse complète, avec le champ `sources` (liste de titres).
- `POST /api/v1/chat/stream` : flux NDJSON ; un événement
  `{"type":"sources","sources":[...]}` est émis avant les jetons.

Côté backend (`AppRH`), `GET /api/chatbot/chat` renvoie également `sources`, et le
flux `/api/chatbot/chat/stream` relaie l'événement `sources` tel quel.

## Tests

`ia-service/tests/test_rag.py` valide le chargement du corpus, la tokenisation et
la pertinence de la recherche :

```bash
docker run --rm -v "%CD%/ia-service:/app" -w /app python:3.12-slim `
  sh -c "pip install -q pytest && python -m pytest -q"
```
