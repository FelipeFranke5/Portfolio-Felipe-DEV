package dev.franke.felipe.website_backend.dto;

import java.util.List;

/**
 * Projeto no formato que vai para o modelo de IA.
 *
 * <p>Não reaproveita o {@link ProjectReadOnly} de propósito: aquele trunca a descrição em
 * 50 caracteres (contrato de {@code GET /api/projects}) e carrega o UUID, que a IA nunca
 * usa e só consome tokens.
 */
public record ProjectForAIChatBot(String nome, String descricao, List<String> stack) {
}
