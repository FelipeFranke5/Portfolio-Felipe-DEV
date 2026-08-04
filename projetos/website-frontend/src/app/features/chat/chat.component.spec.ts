import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WritableSignal, signal } from '@angular/core';

import { ChatComponent } from './chat.component';
import { ChatMessage, ChatbotService, ConnectionStatus } from './chatbot.service';

describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let chatbotServiceMock: {
    connectionStatus: WritableSignal<ConnectionStatus>;
    messages: WritableSignal<ChatMessage[]>;
    lastError: WritableSignal<string | null>;
    connect: jasmine.Spy;
    disconnect: jasmine.Spy;
    send: jasmine.Spy;
  };

  beforeEach(async () => {
    chatbotServiceMock = {
      connectionStatus: signal<ConnectionStatus>('disconnected'),
      messages: signal<ChatMessage[]>([]),
      lastError: signal<string | null>(null),
      connect: jasmine.createSpy('connect'),
      disconnect: jasmine.createSpy('disconnect'),
      send: jasmine.createSpy('send'),
    };

    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [provideRouter([]), { provide: ChatbotService, useValue: chatbotServiceMock }],
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
    expect(messages[0].classList.contains('chat-message--user')).toBeTrue();
    expect(messages[1].classList.contains('chat-message--ai')).toBeTrue();
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

    expect(component.canSend()).toBeFalse();
    const sendButton: HTMLButtonElement =
      fixture.nativeElement.querySelector('.chat-composer__send');
    expect(sendButton.disabled).toBeTrue();
  });

  it('should disable the send button when the draft is empty, even if connected', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    fixture.detectChanges();

    expect(component.canSend()).toBeFalse();
  });

  it('should disable the send button when the draft exceeds 500 characters', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.draftMessage.set('a'.repeat(501));
    fixture.detectChanges();

    expect(component.canSend()).toBeFalse();
  });

  it('should enable the send button and call chatbotService.send() with the draft when connected and valid', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.onDraftChange('Quais projetos você tem?');
    fixture.detectChanges();

    expect(component.canSend()).toBeTrue();

    component.send();

    expect(chatbotServiceMock.send).toHaveBeenCalledWith('Quais projetos você tem?');
    expect(component.draftMessage()).toBe('');
  });

  it('should send on Enter without Shift, and not send on Shift+Enter', () => {
    chatbotServiceMock.connectionStatus.set('connected');
    component.onDraftChange('Pergunta');

    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    spyOn(enterEvent, 'preventDefault');
    component.onTextareaKeydown(enterEvent);

    expect(enterEvent.preventDefault).toHaveBeenCalled();
    expect(chatbotServiceMock.send).toHaveBeenCalledWith('Pergunta');

    chatbotServiceMock.send.calls.reset();
    component.onDraftChange('Outra pergunta');
    const shiftEnterEvent = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
    spyOn(shiftEnterEvent, 'preventDefault');
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

  it('should disable Conectar only when already connecting/connected, and Desconectar only when disconnected', () => {
    chatbotServiceMock.connectionStatus.set('disconnected');
    fixture.detectChanges();
    expect(component.canConnect()).toBeTrue();
    expect(component.canDisconnect()).toBeFalse();

    chatbotServiceMock.connectionStatus.set('connected');
    fixture.detectChanges();
    expect(component.canConnect()).toBeFalse();
    expect(component.canDisconnect()).toBeTrue();
  });

  it('should call chatbotService.disconnect() on destroy', () => {
    fixture.destroy();
    expect(chatbotServiceMock.disconnect).toHaveBeenCalled();
  });
});
