# Sécurité et confidentialité des données

## Protection des données
Les données RH sont confidentielles et couvertes par le RGPD. Chaque collaborateur
n'accède qu'aux données qui le concernent ou qui relèvent de ses responsabilités.

## Contrôle d'accès
AppRH applique un contrôle d'accès basé sur les rôles (RBAC). Les rôles principaux
sont administrateur, responsable RH, chef de projet et employé. Un employé ne peut
consulter que ses propres congés, tâches et notifications.

## Mots de passe et authentification
Les mots de passe doivent contenir au moins 8 caractères. L'authentification
repose sur un jeton JWT à durée limitée. Il est interdit de partager ses
identifiants ou ses jetons d'accès.

## Assistant IA et données
L'assistant RH s'exécute localement : aucune donnée n'est envoyée à un service
d'IA tiers. Le modèle ne décide jamais des autorisations ; les permissions sont
toujours vérifiées par l'application selon l'identité de l'utilisateur connecté.
