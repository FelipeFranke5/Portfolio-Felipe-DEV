import { Route } from '@angular/router';

import { routes } from './app.routes';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { keycloakReadyGuard } from './core/guards/keycloak-ready.guard';

describe('routes', () => {
  function findRoute(path: string): Route {
    const route = routes.find((r) => r.path === path);
    if (!route) {
      throw new Error(`Route "${path}" not found`);
    }
    return route;
  }

  it('should run keycloakReadyGuard before authGuard on /chat', () => {
    expect(findRoute('chat').canActivate).toEqual([keycloakReadyGuard, authGuard]);
  });

  it('should run keycloakReadyGuard before adminGuard on /admin', () => {
    expect(findRoute('admin').canActivate).toEqual([keycloakReadyGuard, adminGuard]);
  });

  it('should not guard public routes behind Keycloak readiness', () => {
    for (const path of ['', 'portfolio', 'skills', 'contact']) {
      expect(findRoute(path).canActivate).toBeUndefined();
    }
  });
});
