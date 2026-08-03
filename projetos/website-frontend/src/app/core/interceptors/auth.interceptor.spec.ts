import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import Keycloak from 'keycloak-js';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  includeBearerTokenInterceptor,
} from 'keycloak-angular';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  it('should be the includeBearerTokenInterceptor from keycloak-angular', () => {
    expect(authInterceptor).toBe(includeBearerTokenInterceptor);
  });

  describe('when wired with a matching URL condition', () => {
    let httpClient: HttpClient;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(withInterceptors([authInterceptor])),
          provideHttpClientTesting(),
          {
            provide: Keycloak,
            useValue: {
              authenticated: true,
              token: 'abc123',
              updateToken: () => Promise.resolve(false),
            },
          },
          {
            provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
            useValue: [{ urlPattern: /^http:\/\/localhost\/api(?!\/websocket)/ }],
          },
        ],
      });

      httpClient = TestBed.inject(HttpClient);
      httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpTestingController.verify());

    it('should attach the Bearer token to requests matching the configured URL pattern', fakeAsync(() => {
      httpClient.get('http://localhost/api/projects').subscribe();
      // includeBearerTokenInterceptor awaits keycloak.updateToken() before
      // calling next() for matching URLs — flush that microtask.
      tick();

      const request = httpTestingController.expectOne('http://localhost/api/projects');
      expect(request.request.headers.get('Authorization')).toBe('Bearer abc123');
    }));

    it('should not attach the Bearer token to requests outside the configured URL pattern', () => {
      httpClient.get('https://external.example.com/data').subscribe();

      const request = httpTestingController.expectOne('https://external.example.com/data');
      expect(request.request.headers.has('Authorization')).toBeFalse();
    });
  });
});
