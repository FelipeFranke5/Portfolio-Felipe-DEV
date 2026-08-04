import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';

import { AuthService, SessionExpiredError } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let keycloakMock: {
    authenticated: boolean;
    token: string | undefined;
    login: jasmine.Spy;
    logout: jasmine.Spy;
    updateToken: jasmine.Spy;
  };

  beforeEach(() => {
    keycloakMock = {
      authenticated: false,
      token: undefined,
      login: jasmine.createSpy('login').and.resolveTo(),
      logout: jasmine.createSpy('logout').and.resolveTo(),
      updateToken: jasmine.createSpy('updateToken').and.resolveTo(true),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: keycloakMock },
        {
          provide: KEYCLOAK_EVENT_SIGNAL,
          useValue: signal({ type: KeycloakEventType.Ready, args: true }),
        },
      ],
    });

    service = TestBed.inject(AuthService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should reflect keycloak.authenticated', () => {
    keycloakMock.authenticated = true;
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should report not authenticated when keycloak.authenticated is undefined', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should delegate login to keycloak.login with the redirect uri', () => {
    service.login('/chat');
    expect(keycloakMock.login).toHaveBeenCalledWith({ redirectUri: '/chat' });
  });

  it('should delegate logout to keycloak.logout with the redirect uri', () => {
    service.logout('/');
    expect(keycloakMock.logout).toHaveBeenCalledWith({ redirectUri: '/' });
  });

  it('should refresh the token (min validity 30s) before returning it', async () => {
    keycloakMock.token = 'fresh-token';

    const token = await service.getToken();

    expect(keycloakMock.updateToken).toHaveBeenCalledWith(30);
    expect(token).toBe('fresh-token');
  });

  it('should reject when there is no token after refreshing', async () => {
    keycloakMock.token = undefined;

    await expectAsync(service.getToken()).toBeRejectedWithError(
      'Nenhum token disponível — usuário não autenticado.'
    );
  });

  it('should wrap a failed updateToken() as a SessionExpiredError, preserving the original cause', async () => {
    const refreshFailure = new Error('refresh token expired');
    keycloakMock.updateToken.and.rejectWith(refreshFailure);

    const error = await service.getToken().then(
      () => null,
      (rejection: unknown) => rejection
    );

    expect(error).toBeInstanceOf(SessionExpiredError);
    expect((error as SessionExpiredError).cause).toBe(refreshFailure);
  });
});
