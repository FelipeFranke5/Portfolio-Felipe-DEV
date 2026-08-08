// jsdom não implementa Element.scrollTo (usado pelo ChatComponent para
// rolar o histórico de mensagens); sem esse shim, qualquer detectChanges()
// que dispare o efeito de auto-scroll lança "container.scrollTo is not a
// function" durante os testes.
if (typeof Element !== 'undefined' && !Element.prototype.scrollTo) {
  Element.prototype.scrollTo = function (): void {};
}

// sockjs-client (usado pelo ChatbotService) carrega, no nível do módulo, uma
// transport de fallback "eventsource" pensada só para clientes Node.js —
// essa versão do pacote `eventsource` importa `http`/`https`/`util` sem
// checar o ambiente, o que não faz sentido (e quebra o bundling) rodando
// num browser real, que já tem EventSource/WebSocket nativos. Como os specs
// nunca abrem uma conexão SockJS de verdade, mockamos o módulo inteiro para
// não pagar esse custo de import.
vi.mock('sockjs-client', () => ({ default: class FakeSockJS {} }));
