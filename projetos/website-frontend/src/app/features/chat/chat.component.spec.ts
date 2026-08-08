import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WritableSignal, signal } from '@angular/core';

import { ChatComponent } from './chat.component';
import { AuthService } from '../../core/services/auth.service';
import { ChatMessage, ChatbotService, ConnectionStatus } from './chatbot.service';

describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let chatbotServiceMock: {
    connectionStatus: WritableSignal<ConnectionStatus>;
    messages: WritableSignal<ChatMessage[]>;
    lastError: WritableSignal<string | null>;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    send: ReturnType<typeof vi.fn>;
  };
  let authServiceMock: { logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    chatbotServiceMock = {
      connectionStatus: signal<ConnectionStatus>('disconnected'),
      messages: signal<ChatMessage[]>([]),
      lastError: signal<string | null>(null),
      connect: vi.fn(),
      disconnect: vi.fn(),
      send: vi.fn(),
    };

    authServiceMock = { logout: vi.fn().mockReturnValue(Promise.resolve()) };

    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        provideRouter([]),
        { provide: ChatbotService, useValue: chatbotServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and connect automatically', () => {
    expect(component).toBeTruthy();
    expect(chatbotServiceMock.connect).toHaveBeenCalled();
  });

  it('should render the header and footer components', () => {
    const headerElement = fixture.nativeElement.querySelector('app-header');
    const footerElement = fixture.nativeElement.querySelector('app-footer');

    expect(headerElement).not.toBeNull();
    expect(footerElement).not.toBeNull();
  });

  it('should render one chat-message per item in messages, with the correct kind modifier class', () => {
    chatbotServiceMock.messages.set([
      { id: '1', kind: 'user', sender: 'felipe', text: 'Oi', timestamp: new Date() },
      { id: '2', kind: 'ai', sender: 'bot', text: 'Olá!', timestamp: new Date() },
    ]);
    fixture.detectChanges();

    const messages: NodeListOf<HTMLElement> =
      fixture.nativeElement.querySelectorAll('.chat-message');

    expect(messages.length).toBe(2);
    expect(messages[0].classList.contains('chat-message--user')).toBe(true);
    expect(messages[1].classList.contains('chat-message--ai')).toBe(true);
  });

  it('should reflect the char counter as the draft message changes', () => {
    component.onDraftChange('Olá, tudo bem?');
    fixture.detectChanges();

    expect(component.charCount()).toBe('Olá, tudo bem?'.length);
    const counter: HTMLElement = fixture.nativeElement.querySelector('.chat-composer__counter');
    expect(counter.textContent).toContain(`${'Olá, tudo bem?'.length}/500`);
  });

  it('should disable the send button when disconnected, even with a valid draft', () => {
    chatbotServiceMock.connectionStatus.set('disconnected');
    component.onDraftChange('Uma pergunta válida');
    fixture.detectChanges();

    expect(component.canSend()).toBe(false);
    const sendButton: HTMLButtonElement =
      fixture.nativeElement.querySelector('.chat-composer__send');
    expect(sendButton.disabled).toBe(true);
  });

  it('should disable the send button when the draft is empty, even if connected', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    fixture.detectChanges();

    expect(component.canSend()).toBe(false);
  });

  it('should disable the send button when the draft exceeds 500 characters', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.draftMessage.set('a'.repeat(501));
    fixture.detectChanges();

    expect(component.canSend()).toBe(false);
  });

  it('should enable the send button and call chatbotService.send() with the draft when connected and valid', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.onDraftChange('Quais projetos você tem?');
    fixture.detectChanges();

    expect(component.canSend()).toBe(true);

    component.send();

    expect(chatbotServiceMock.send).toHaveBeenCalledWith('Quais projetos você tem?');
    expect(component.draftMessage()).toBe('');
  });

  it('should send on Enter without Shift, and not send on Shift+Enter', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.onDraftChange('Pergunta');

    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    vi.spyOn(enterEvent, 'preventDefault');
    component.onTextareaKeydown(enterEvent);

    expect(enterEvent.preventDefault).toHaveBeenCalled();
    expect(chatbotServiceMock.send).toHaveBeenCalledWith('Pergunta');

    chatbotServiceMock.send.mockClear();
    component.onDraftChange('Outra pergunta');
    const shiftEnterEvent = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
    vi.spyOn(shiftEnterEvent, 'preventDefault');
    component.onTextareaKeydown(shiftEnterEvent);

    expect(shiftEnterEvent.preventDefault).not.toHaveBeenCalled();
    expect(chatbotServiceMock.send).not.toHaveBeenCalled();
  });

  it('should show the correct status label for each connection status', () => {
    const labels: Record<ConnectionStatus, string> = {
      connected: 'Conectado',
      connecting: 'Conectando...',
      disconnected: 'Desconectado',
      reconnecting: 'Reconectando...',
    };

    (Object.keys(labels) as ConnectionStatus[]).forEach((status) => {
      chatbotServiceMock.connectionStatus.set(status);
      fixture.detectChanges();
      expect(component.statusLabel()).toBe(labels[status]);
    });
  });

  it('should disable Conectar only when already connecting/connected', () => {
    chatbotServiceMock.connectionStatus.set('disconnected');
    fixture.detectChanges();
    expect(component.canConnect()).toBe(true);

    chatbotServiceMock.connectionStatus.set('connected');
    fixture.detectChanges();
    expect(component.canConnect()).toBe(false);
  });

  it('should never disable the disconnect button, whatever the connection status', () => {
    const statuses: ConnectionStatus[] = [
      'connected',
      'connecting',
      'disconnected',
      'reconnecting',
    ];

    statuses.forEach((status) => {
      chatbotServiceMock.connectionStatus.set(status);
      fixture.detectChanges();

      const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll(
        '.chat-page__control-button',
      );
      const disconnectButton = Array.from(buttons).find((button) =>
        button.textContent?.includes('Desconectar e sair'),
      );

      expect(disconnectButton).toBeTruthy();
      expect(disconnectButton!.disabled).toBe(false);
    });
  });

  it('should disconnect and log the user out with a redirect to the home page', () => {
    component.disconnect();

    expect(chatbotServiceMock.disconnect).toHaveBeenCalled();
    expect(authServiceMock.logout).toHaveBeenCalledWith(window.location.origin + '/');
  });

  it('should show a feedback message and allow a retry when logout fails', async () => {
    authServiceMock.logout.mockReturnValue(Promise.reject(new Error('keycloak offline')));

    component.disconnect();
    await fixture.whenStable();
    fixture.detectChanges();

    const feedback: HTMLElement = fixture.nativeElement.querySelector('.chat-page__feedback');
    expect(feedback.textContent).toContain('Não foi possível encerrar a sessão');

    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll(
      '.chat-page__control-button',
    );
    const disconnectButton = Array.from(buttons).find((button) =>
      button.textContent?.includes('Desconectar e sair'),
    );
    expect(disconnectButton!.disabled).toBe(false);

    authServiceMock.logout.mockReturnValue(Promise.resolve());
    disconnectButton!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(authServiceMock.logout).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('.chat-page__feedback')).toBeNull();
  });

  it('should keep showing the chatbot error when there is no logout error', () => {
    chatbotServiceMock.lastError.set('Falha ao conectar.');
    fixture.detectChanges();

    expect(component.lastError()).toBe('Falha ao conectar.');
  });

  it('should call chatbotService.disconnect() on destroy without logging out', () => {
    fixture.destroy();
    expect(chatbotServiceMock.disconnect).toHaveBeenCalled();
    expect(authServiceMock.logout).not.toHaveBeenCalled();
  });
});
