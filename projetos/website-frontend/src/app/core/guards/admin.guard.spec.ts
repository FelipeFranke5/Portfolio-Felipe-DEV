import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthGuardData } from 'keycloak-angular';

import { isAdminAccessAllowed } from './admin.guard';

describe('isAdminAccessAllowed', () => {
  const route = {} as ActivatedRouteSnapshot;

  function buildAuthData(
    authenticated: boolean,
    realmRoles: string[],
    loginSpy: jasmine.Spy
  ): AuthGuardData {
    return {
      authenticated,
      grantedRoles: { realmRoles, resourceRoles: {} },
      keycloak: { login: loginSpy } as unknown as AuthGuardData['keycloak'],
    };
  }

  it('should allow access when the user is authenticated and has the ADMIN realm role', async () => {
    const loginSpy = jasmine.createSpy('login');
    const authData = buildAuthData(true, ['ADMIN'], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData);

    expect(result).toBeTrue();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('should redirect to Keycloak login and deny access when the user is not authenticated', async () => {
    const loginSpy = jasmine.createSpy('login').and.resolveTo();
    const authData = buildAuthData(false, [], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData);

    expect(result).toBeFalse();
    expect(loginSpy).toHaveBeenCalledWith({
      redirectUri: window.location.origin + '/admin',
    });
  });

  it('should deny access WITHOUT redirecting to login when authenticated but missing the ADMIN role', async () => {
    const loginSpy = jasmine.createSpy('login');
    const authData = buildAuthData(true, ['USER'], loginSpy);
    const state = { url: '/admin' } as RouterStateSnapshot;

    const result = await isAdminAccessAllowed(route, state, authData);

    expect(result).toBeFalse();
    expect(loginSpy).not.toHaveBeenCalled();
  });
});
