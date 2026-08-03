import { Injectable, Signal, computed, inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

/**
 * Wrapper de autenticação sobre o keycloak-angular, conforme descrito
 * na seção "core" do README.md.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  // Depende de keycloakEvent() só para reavaliar a cada evento do Keycloak
  // (login, refresh, logout) — keycloak.authenticated/token em si não são signals.
  readonly isAuthenticated: Signal<boolean> = computed(() => {
    this.keycloakEvent();
    return this.keycloak.authenticated ?? false;
  });

  /** Leitura síncrona best-effort, só para exibição não-crítica — pode estar
   * perto de expirar. Para qualquer uso que vá para a rede, use getToken(). */
  readonly token: Signal<string | null> = computed(() => {
    this.keycloakEvent();
    return this.keycloak.token ?? null;
  });

  login(redirectUri?: string): Promise<void> {
    return this.keycloak.login({ redirectUri });
  }

  logout(redirectUri?: string): Promise<void> {
    return this.keycloak.logout({ redirectUri });
  }

  /**
   * Token de acesso atual, renovando antes se estiver a menos de 30s de
   * expirar. Assíncrono de propósito: usado pelo `beforeConnect` do STOMP,
   * que roda a cada tentativa de conexão (inclusive reconexões automáticas),
   * garantindo token sempre fresco numa sessão de chat longa.
   */
  async getToken(): Promise<string> {
    await this.keycloak.updateToken(30);
    if (!this.keycloak.token) {
      throw new Error('Nenhum token disponível — usuário não autenticado.');
    }
    return this.keycloak.token;
  }
}
