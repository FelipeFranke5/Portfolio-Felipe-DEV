import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { AuthService } from '../../core/services/auth.service';
import { ChatbotService, ConnectionStatus, MAX_MESSAGE_LENGTH } from './chatbot.service';

const STATUS_LABELS: Record<ConnectionStatus, string> = {
  connected: 'Conectado',
  connecting: 'Conectando...',
  disconnected: 'Desconectado',
  reconnecting: 'Reconectando...',
};

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [DatePipe, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './chat.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './chat.component.scss',
})
export class ChatComponent {
  private readonly chatbotService = inject(ChatbotService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly maxMessageLength = MAX_MESSAGE_LENGTH;

  readonly connectionStatus = this.chatbotService.connectionStatus;
  readonly messages = this.chatbotService.messages;

  private readonly logoutError = signal<string | null>(null);
  readonly lastError = computed(() => this.logoutError() ?? this.chatbotService.lastError());

  readonly draftMessage = signal('');
  readonly charCount = computed(() => this.draftMessage().length);
  readonly canSend = computed(() => {
    const draft = this.draftMessage().trim();
    return (
      draft.length > 0 &&
      draft.length <= MAX_MESSAGE_LENGTH &&
      this.connectionStatus() === 'connected'
    );
  });

  readonly canConnect = computed(() => this.connectionStatus() === 'disconnected');

  readonly statusLabel = computed(() => STATUS_LABELS[this.connectionStatus()]);

  private readonly messagesContainer =
    viewChild<ElementRef<HTMLDivElement>>('messagesContainer');

  constructor() {
    effect(() => {
      this.messages();
      const container = this.messagesContainer()?.nativeElement;
      if (container) {
        container.scrollTo({ top: container.scrollHeight, behavior: 'smooth' });
      }
    });

    this.chatbotService.connect();
    this.destroyRef.onDestroy(() => this.chatbotService.disconnect());
  }

  connect(): void {
    this.chatbotService.connect();
  }

  disconnect(): void {
    this.chatbotService.disconnect();
    this.logoutError.set(null);
    void this.authService.logout(window.location.origin + '/').catch(() => {
      this.logoutError.set(
        'Não foi possível encerrar a sessão. A conexão com o chat foi fechada; tente novamente.',
      );
    });
  }

  onDraftChange(value: string): void {
    this.draftMessage.set(value.slice(0, MAX_MESSAGE_LENGTH));
  }

  onTextareaKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  send(): void {
    if (!this.canSend()) {
      return;
    }
    this.chatbotService.send(this.draftMessage());
    this.draftMessage.set('');
  }
}
