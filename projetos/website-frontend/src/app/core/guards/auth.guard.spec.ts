import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData } from 'keycloak-angular';

import { isChatAccessAllowed } from './auth.guard';

describe('isChatAccessAllowed', () => {
  const route = {} as ActivatedRouteSnapshot;

  function buildAuthData(authenticated: boolean, loginSpy: jasmine.Spy): AuthGuardData {
    return {
      authenticated,
      grantedRoles: { realmRoles: [], resourceRoles: {} },
      keycloak: { login: loginSpy } as unknown as AuthGuardData['keycloak'],
    };
  }

  it('should allow access when the user is authenticated', async () => {
    const loginSpy = jasmine.createSpy('login');
    const authData = buildAuthData(true, loginSpy);
    const state = { url: '/chat' } as RouterStateSnapshot;

    const result = await isChatAccessAllowed(route, state, authData);

    expect(result).toBeTrue();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('should redirect to Keycloak login and deny access when the user is not authenticated', async () => {
    const loginSpy = jasmine.createSpy('login').and.resolveTo();
    const authData = buildAuthData(false, loginSpy);
    const state = { url: '/chat' } as RouterStateSnapshot;

    const result = await isChatAccessAllowed(route, state, authData);

    expect(result).toBeFalse();
    expect(loginSpy).toHaveBeenCalledWith({
      redirectUri: window.location.origin + '/chat',
    });
  });
});
