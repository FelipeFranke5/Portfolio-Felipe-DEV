import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { keycloakReadyGuard } from './keycloak-ready.guard';
import { KEYCLOAK_INIT_PROMISE } from '../tokens/keycloak-init.token';

describe('keycloakReadyGuard', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  function runGuard(initPromise: Promise<boolean>): Promise<boolean> {
    TestBed.configureTestingModule({
      providers: [{ provide: KEYCLOAK_INIT_PROMISE, useValue: initPromise }],
    });

    return TestBed.runInInjectionContext(() => keycloakReadyGuard(route, state)) as Promise<boolean>;
  }

  it('should resolve true once the Keycloak init promise resolves', async () => {
    await expect(runGuard(Promise.resolve(true))).resolves.toBe(true);
  });

  it('should resolve true even if the Keycloak init promise resolves to false', async () => {
    await expect(runGuard(Promise.resolve(false))).resolves.toBe(true);
  });

  it('should wait for the Keycloak init promise before resolving', async () => {
    let initResolved = false;
    const initPromise = new Promise<boolean>((resolve) => {
      setTimeout(() => {
        initResolved = true;
        resolve(true);
      }, 0);
    });

    const guardPromise = runGuard(initPromise);

    expect(initResolved).toBe(false);
    await guardPromise;
    expect(initResolved).toBe(true);
  });
});
