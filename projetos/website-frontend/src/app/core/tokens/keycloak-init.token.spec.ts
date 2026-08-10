import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';

import { KEYCLOAK_INIT_PROMISE } from './keycloak-init.token';

describe('KEYCLOAK_INIT_PROMISE', () => {
  it('should initialize Keycloak with a silent SSO check', async () => {
    const initSpy = vi.fn().mockResolvedValue(true);

    TestBed.configureTestingModule({
      providers: [{ provide: Keycloak, useValue: { init: initSpy } }],
    });

    const result = await TestBed.inject(KEYCLOAK_INIT_PROMISE);

    expect(initSpy).toHaveBeenCalledWith({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
    });
    expect(result).toBe(true);
  });

  it('should resolve to false instead of rejecting when Keycloak init fails', async () => {
    const initSpy = vi.fn().mockRejectedValue(new Error('network error'));
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    TestBed.configureTestingModule({
      providers: [{ provide: Keycloak, useValue: { init: initSpy } }],
    });

    const result = await TestBed.inject(KEYCLOAK_INIT_PROMISE);

    expect(result).toBe(false);
    consoleErrorSpy.mockRestore();
  });
});
