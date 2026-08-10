import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { KEYCLOAK_INIT_PROMISE } from '../tokens/keycloak-init.token';

export const keycloakReadyGuard: CanActivateFn = async () => {
  await inject(KEYCLOAK_INIT_PROMISE);
  return true;
};
