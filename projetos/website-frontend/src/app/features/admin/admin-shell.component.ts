import { Component, ElementRef, OnDestroy, effect, inject, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { SessionIdleService } from '../../core/services/session-idle.service';

interface AdminNavLink {
  path: string;
  label: string;
  exact: boolean;
}

/**
 * Moldura das telas do painel: Header e Footer públicos do site (reaproveitados
 * de propósito, conforme a orientação de design) mais a navegação interna do
 * /admin e o aviso de sessão expirada.
 *
 * Existe como rota-pai para que a subnav e o relógio de inatividade não sejam
 * remontados a cada troca de tela filha.
 */
@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, HeaderComponent, FooterComponent],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent implements OnDestroy {
  private readonly authService = inject(AuthService);
  protected readonly sessionIdle = inject(SessionIdleService);

  private readonly reloginButton = viewChild<ElementRef<HTMLButtonElement>>('reloginButton');

  readonly navLinks: AdminNavLink[] = [
    { path: '/admin', label: 'Dashboard', exact: true },
    { path: '/admin/projects', label: 'Projetos', exact: false },
    { path: '/admin/skills', label: 'Skills', exact: false },
    { path: '/admin/logs', label: 'Logs', exact: false },
  ];

  constructor() {
    this.sessionIdle.start();

    effect(() => {
      if (this.sessionIdle.isExpired()) {
        this.reloginButton()?.nativeElement.focus();
      }
    });
  }

  ngOnDestroy(): void {
    this.sessionIdle.stop();
  }

  relogin(): void {
    // Volta para o painel depois do login, e não para a home: quem viu este
    // aviso estava no meio de uma tarefa administrativa.
    void this.authService.login(window.location.origin + '/admin');
  }
}
