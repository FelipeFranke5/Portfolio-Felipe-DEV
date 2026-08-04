// sockjs-client (usado pelo ChatbotService) referencia o global `global` do
// Node.js sem checar a existência antes (node_modules/sockjs-client/lib/utils/
// browser-crypto.js), o que quebra com ReferenceError assim que o módulo é
// avaliado num navegador real — o bundler esbuild do Angular CLI, ao
// contrário do antigo builder webpack, não faz esse shim automaticamente.
(globalThis as unknown as { global: typeof globalThis }).global = globalThis;
