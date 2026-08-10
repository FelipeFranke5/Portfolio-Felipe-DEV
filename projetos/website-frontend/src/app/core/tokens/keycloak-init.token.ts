import { inject, InjectionToken } from '@angular/core';
import Keycloak from 'keycloak-js';

export const KEYCLOAK_INIT_PROMISE = new InjectionToken<Promise<boolean>>('KEYCLOAK_INIT_PROMISE', {
  providedIn: 'root',
  factory: () =>
    inject(Keycloak)
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
      })
      .catch((error) => {
        console.error('Keycloak initialization failed', error);
        return false;
      }),
});
