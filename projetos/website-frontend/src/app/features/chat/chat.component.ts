import {
  Component,
  DestroyRef,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { ChatbotService, ConnectionStatus } from './chatbot.service';

const MAX_MESSAGE_LENGTH = 500;

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
  styleUrl: './chat.component.scss',
})
export class ChatComponent {
  private readonly chatbotService = inject(ChatbotService);
  private readonly destroyRef = inject(DestroyRef);

  readonly maxMessageLength = MAX_MESSAGE_LENGTH;

  readonly connectionStatus = this.chatbotService.connectionStatus;
  readonly messages = this.chatbotService.messages;
  readonly lastError = this.chatbotService.lastError;

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
  readonly canDisconnect = computed(() => this.connectionStatus() !== 'disconnected');

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
