export const environment = {
  production: true,
  apiUrl: '/api',
  // Vide en production : les identifiants de démonstration vivent uniquement
  // dans environment.development.ts, substitué au build « development »
  // (voir fileReplacements dans angular.json). Ils n'existent donc pas
  // dans le bundle de production.
  demoAccountsHint: ''
};
