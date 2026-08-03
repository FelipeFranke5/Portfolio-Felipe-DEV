import { TestBed } from '@angular/core/testing';
import { Client, IFrame, IMessage, StompConfig } from '@stomp/stompjs';

import { AuthService } from '../../core/services/auth.service';
import { ChatbotService, StompClientFactory } from './chatbot.service';

class FakeClient {
  connectHeaders: Record<string, string> = {};
  active = false;
  deactivateCalls = 0;
  subscriptions: { destination: string; callback: (frame: IMessage) => void }[] = [];
  publish = jasmine.createSpy('publish');

  constructor(private readonly config: StompConfig) {}

  activate(): void {
    this.active = true;
  }

  deactivate(): Promise<void> {
    this.active = false;
    this.deactivateCalls++;
    return Promise.resolve();
  }

  subscribe(destination: string, callback: (frame: IMessage) => void) {
    this.subscriptions.push({ destination, callback });
    return { id: `sub-${this.subscriptions.length}`, unsubscribe: () => {} };
  }

  async triggerBeforeConnect(): Promise<void> {
    await this.config.beforeConnect?.(this as unknown as Client);
  }

  triggerConnect(): void {
    this.config.onConnect?.({} as IFrame);
  }

  triggerClose(): void {
    this.config.onWebSocketClose?.({} as CloseEvent);
  }

  triggerStompError(message: string): void {
    this.config.onStompError?.({ headers: { message } } as unknown as IFrame);
  }

  emit(destination: string, body: unknown): void {
    const subscription = this.subscriptions.find((s) => s.destination === destination);
    subscription?.callback({ body: JSON.stringify(body) } as IMessage);
  }
}

class FakeStompClientFactory {
  lastClient: FakeClient | null = null;
  createCalls = 0;

  create(config: StompConfig): Client {
    this.createCalls++;
    this.lastClient = new FakeClient(config);
    return this.lastClient as unknown as Client;
  }
}

describe('ChatbotService', () => {
  let service: ChatbotService;
  let factory: FakeStompClientFactory;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    factory = new FakeStompClientFactory();
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['getToken']);
    authServiceSpy.getToken.and.resolveTo('token-123');

    TestBed.configureTestingModule({
      providers: [
        { provide: StompClientFactory, useValue: factory },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    service = TestBed.inject(ChatbotService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should transition to connecting, set the Bearer header, then connect and subscribe to the three queues in order', async () => {
    service.connect();
    expect(service.connectionStatus()).toBe('connecting');

    const client = factory.lastClient!;
    await client.triggerBeforeConnect();
    expect(client.connectHeaders['Authorization']).toBe('Bearer token-123');

    client.triggerConnect();

    expect(service.connectionStatus()).toBe('connected');
    expect(client.subscriptions.map((s) => s.destination)).toEqual([
      '/user/queue/chatbot/mensagens',
      '/user/queue/chatbot/respostas',
      '/user/queue/chatbot/erros',
    ]);
  });

  it('should not open a second connection while connecting or connected', () => {
    service.connect();
    service.connect();
    expect(factory.createCalls).toBe(1);

    factory.lastClient!.triggerConnect();
    service.connect();
    expect(factory.createCalls).toBe(1);
  });

  it('should route messages from the mensagens queue as user messages', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    client.emit('/user/queue/chatbot/mensagens', {
      name: 'felipe',
      message: 'Olá!',
      timestamp: '2026-08-03T10:00:00',
    });

    expect(service.messages().length).toBe(1);
    expect(service.messages()[0].kind).toBe('user');
    expect(service.messages()[0].sender).toBe('felipe');
    expect(service.messages()[0].text).toBe('Olá!');
  });

  it('should route "sistema" messages from the respostas queue as system messages', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    client.emit('/user/queue/chatbot/respostas', {
      name: 'sistema',
      message: 'Mensagem em processamento..',
      timestamp: '2026-08-03T10:00:00',
    });

    expect(service.messages()[0].kind).toBe('system');
  });

  it('should route "bot" messages from the respostas queue as ai messages', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    client.emit('/user/queue/chatbot/respostas', {
      name: 'bot',
      message: 'Posso ajudar com isso.',
      timestamp: '2026-08-03T10:00:00',
    });

    expect(service.messages()[0].kind).toBe('ai');
  });

  it('should route messages from the erros queue as error messages and set lastError, without disconnecting', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    client.emit('/user/queue/chatbot/erros', {
      currentTime: '2026-08-03T10:00:00',
      message: 'Muitas perguntas seguidas. Aguarde um instante antes de tentar novamente.',
    });

    expect(service.messages()[0].kind).toBe('error');
    expect(service.lastError()).toBe(
      'Muitas perguntas seguidas. Aguarde um instante antes de tentar novamente.'
    );
    expect(service.connectionStatus()).toBe('connected');
    expect(client.deactivateCalls).toBe(0);
  });

  it('should end at disconnected with lastError set when the initial connection attempt fails', () => {
    service.connect();
    const client = factory.lastClient!;

    client.triggerClose();

    expect(service.connectionStatus()).toBe('disconnected');
    expect(service.lastError()).toContain('Não foi possível conectar');
  });

  it('should transition to reconnecting when a connection drops after having connected once', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();
    client.active = true;

    client.triggerClose();

    expect(service.connectionStatus()).toBe('reconnecting');
  });

  it('should re-subscribe to the three queues on every reconnection', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();
    expect(client.subscriptions.length).toBe(3);

    client.triggerConnect();
    expect(client.subscriptions.length).toBe(6);
  });

  it('should set lastError from the STOMP error frame message', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    client.triggerStompError('Erro no broker.');

    expect(service.lastError()).toBe('Erro no broker.');
  });

  it('should not publish when send() is called while disconnected', () => {
    service.send('Olá?');
    expect(service.messages().length).toBe(0);
  });

  it('should not publish a blank message', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    service.send('   ');

    expect(client.publish).not.toHaveBeenCalled();
  });

  it('should not publish a message longer than 500 characters', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    service.send('a'.repeat(501));

    expect(client.publish).not.toHaveBeenCalled();
  });

  it('should publish to /chatbot/new-message with the trimmed message when connected', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    service.send('  Quais projetos você tem?  ');

    expect(client.publish).toHaveBeenCalledWith({
      destination: '/chatbot/new-message',
      body: JSON.stringify({ message: 'Quais projetos você tem?' }),
    });
  });

  it('should deactivate the client and reset to disconnected on disconnect()', () => {
    service.connect();
    const client = factory.lastClient!;
    client.triggerConnect();

    service.disconnect();

    expect(client.deactivateCalls).toBe(1);
    expect(service.connectionStatus()).toBe('disconnected');
  });
});
