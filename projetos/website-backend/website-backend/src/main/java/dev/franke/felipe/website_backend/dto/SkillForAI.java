package dev.franke.felipe.website_backend.dto;

/**
 * Habilidade no formato que vai para o modelo de IA — sem o UUID, que a IA não usa.
 */
public record SkillForAI(String nome, String categoria, String nivel) {
}
