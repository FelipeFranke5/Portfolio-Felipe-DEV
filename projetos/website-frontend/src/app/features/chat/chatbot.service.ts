import { Injectable, inject, signal } from '@angular/core';
import { Client, IMessage, StompConfig } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'reconnecting';
export type ChatMessageKind = 'user' | 'system' | 'ai' | 'error';

export interface ChatMessage {
  id: string;
  kind: ChatMessageKind;
  sender: string;
  text: string;
  timestamp: Date;
}

interface ChatbotOutputPayload {
  name: string;
  message: string;
  timestamp: string;
}

interface ChatbotErrorPayload {
  currentTime: string;
  message: string;
}

const NEW_MESSAGE_DESTINATION = '/chatbot/new-message';
const MESSAGES_QUEUE = '/user/queue/chatbot/mensagens';
const RESPONSES_QUEUE = '/user/queue/chatbot/respostas';
const ERRORS_QUEUE = '/user/queue/chatbot/erros';
const SYSTEM_SENDER = 'sistema';
const AI_SENDER = 'bot';
const MAX_MESSAGE_LENGTH = 500;

/**
 * Fábrica injetável em volta de `new Client(...)` — costura de testabilidade
 * para os testes unitários substituírem por um fake sem abrir socket real.
 */
@Injectable({ providedIn: 'root' })
export class StompClientFactory {
  create(config: StompConfig): Client {
    return new Client(config);
  }
}

/**
 * Cliente STOMP/SockJS do chatbot, conforme o contrato do back-end descrito
 * em README.md > "Chatbot com IA": handshake SockJS em /api/websocket,
 * autenticação no frame CONNECT (não no handshake HTTP), e três filas de
 * recebimento sob /user/queue/chatbot/*.
 */
@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly authService = inject(AuthService);
  private readonly stompClientFactory = inject(StompClientFactory);

  private client: Client | null = null;
  // Distingue "nunca conectou" (provável falha de auth no CONNECT — o
  // back-end só fecha o socket, sem frame ERROR com corpo) de "conectou e
  // depois caiu" (provável rede/reconexão). É o melhor sinal disponível no
  // transporte, não uma certeza — ver handleDrop().
  private hasConnectedOnce = false;

  readonly connectionStatus = signal<ConnectionStatus>('disconnected');
  readonly messages = signal<ChatMessage[]>([]);
  readonly lastError = signal<string | null>(null);

  connect(): void {
    const status = this.connectionStatus();
    if (status === 'connected' || status === 'connecting') {
      return;
    }

    this.connectionStatus.set('connecting');
    this.hasConnectedOnce = false;

    const client = this.stompClientFactory.create({
      webSocketFactory: () => new SockJS(`${environment.apiUrl}/websocket`),
      reconnectDelay: 5000,
      beforeConnect: async () => {
        // Roda a cada tentativa de conexão, inclusive reconexões automáticas
        // — garante um token sempre fresco numa sessão de chat longa.
        const token = await this.authService.getToken();
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      },
      onConnect: () => {
        this.hasConnectedOnce = true;
        this.subscribeToQueues(client);
        this.connectionStatus.set('connected');
        this.lastError.set(null);
      },
      onWebSocketClose: () => this.handleDrop(client),
      onWebSocketError: () => this.handleDrop(client),
      onStompError: (frame) => {
        this.lastError.set(frame.headers['message'] ?? 'Erro STOMP.');
        this.handleDrop(client);
      },
    });

    this.client = client;
    client.activate();
  }

  disconnect(): void {
    this.client?.deactivate();
    this.client = null;
    this.hasConnectedOnce = false;
    this.connectionStatus.set('disconnected');
  }

  send(message: string): void {
    const trimmed = message.trim();
    if (!trimmed || trimmed.length > MAX_MESSAGE_LENGTH) {
      return;
    }
    if (this.connectionStatus() !== 'connected' || !this.client) {
      return;
    }

    this.client.publish({
      destination: NEW_MESSAGE_DESTINATION,
      body: JSON.stringify({ message: trimmed }),
    });
  }

  private handleDrop(client: Client): void {
    if (!this.hasConnectedOnce) {
      this.connectionStatus.set('disconnected');
      this.lastError.set(
        'Não foi possível conectar. Verifique se você está logado ou tente novamente em instantes.'
      );
      return;
    }

    this.connectionStatus.set(client.active ? 'reconnecting' : 'disconnected');
  }

  // Assina as três filas de forma síncrona, antes de retornar — como isso
  // acontece antes de connectionStatus virar 'connected' (e o botão Enviar
  // só libera nesse estado), o requisito do back-end de assinar antes de
  // enviar é satisfeito por construção. Reassinado a cada reconexão, já que
  // cada onConnect representa uma nova sessão STOMP.
  private subscribeToQueues(client: Client): void {
    client.subscribe(MESSAGES_QUEUE, (frame) => this.handleOutput(frame));
    client.subscribe(RESPONSES_QUEUE, (frame) => this.handleOutput(frame));
    client.subscribe(ERRORS_QUEUE, (frame) => this.handleError(frame));
  }

  private deriveKind(name: string): ChatMessageKind {
    if (name === SYSTEM_SENDER) {
      return 'system';
    }
    if (name === AI_SENDER) {
      return 'ai';
    }
    return 'user';
  }

  private handleOutput(frame: IMessage): void {
    const payload: ChatbotOutputPayload = JSON.parse(frame.body);
    this.appendMessage({
      kind: this.deriveKind(payload.name),
      sender: payload.name,
      text: payload.message,
      // Jackson serializa LocalDateTime sem timezone; o Date do navegador
      // interpreta como horário local, aproximação aceitável para uma
      // exibição discreta de horário (imprecisão conhecida e aceita).
      timestamp: new Date(payload.timestamp),
    });
  }

  private handleError(frame: IMessage): void {
    const payload: ChatbotErrorPayload = JSON.parse(frame.body);
    // Erros (ex.: rate limit) não derrubam a sessão STOMP — nada aqui
    // desconecta, é uma atualização de estado pura.
    this.lastError.set(payload.message);
    this.appendMessage({
      kind: 'error',
      sender: 'Erro',
      text: payload.message,
      timestamp: new Date(payload.currentTime),
    });
  }

  private appendMessage(message: Omit<ChatMessage, 'id'>): void {
    this.messages.update((current) => [...current, { id: crypto.randomUUID(), ...message }]);
  }
}
