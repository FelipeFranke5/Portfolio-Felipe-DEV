import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData } from 'keycloak-angular';

import { isChatAccessAllowed } from './auth.guard';

describe('isChatAccessAllowed', () => {
  const route = {} as ActivatedRouteSnapshot;

  function buildAuthData(authenticated: boolean, loginSpy: ReturnType<typeof vi.fn>): AuthGuardData {
    return {
      authenticated,
      grantedRoles: { realmRoles: [], resourceRoles: {} },
      keycloak: { login: loginSpy } as unknown as AuthGuardData['keycloak'],
    };
  }

  it('should allow access when the user is authenticated', async () => {
    const loginSpy = vi.fn();
    const authData = buildAuthData(true, loginSpy);
    const state = { url: '/chat' } as RouterStateSnapshot;

    const result = await isChatAccessAllowed(route, state, authData);

    expect(result).toBe(true);
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('should redirect to Keycloak login and deny access when the user is not authenticated', async () => {
    const loginSpy = vi.fn().mockResolvedValue(undefined);
    const authData = buildAuthData(false, loginSpy);
    const state = { url: '/chat' } as RouterStateSnapshot;

    const result = await isChatAccessAllowed(route, state, authData);

    expect(result).toBe(false);
    expect(loginSpy).toHaveBeenCalledWith({
      redirectUri: window.location.origin + '/chat',
    });
  });
});
