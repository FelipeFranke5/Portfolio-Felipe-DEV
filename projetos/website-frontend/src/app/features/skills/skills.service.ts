import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../core/services/api.service';

export interface Skill {
  id: string;
  name: string;
  category: string;
  skillLevel: string;
}

/** Espelha o `SkillRequest` do back-end (POST e PUT usam o mesmo payload). */
export interface SkillRequest {
  name: string;
  category: string;
  level: number;
}

export const SKILL_NAME_MAX_LENGTH = 50;
export const SKILL_CATEGORY_MAX_LENGTH = 50;

export interface SkillLevelMeta {
  /** Valor numérico aceito por `SkillRequest.level` (1 a 5). */
  level: number;
  /** Nome da constante do enum `SkillLevel` no back-end. */
  enumName: string;
  /** Resultado de `SkillLevel.toString()`, ou seja, a descrição em inglês. */
  description: string;
  /** Rótulo exibido na interface. */
  label: string;
  /** Quantas das 4 barrinhas do indicador de nível ficam preenchidas. */
  tier: number;
}

/**
 * Fonte única de verdade sobre os níveis de skill, compartilhada entre a
 * página pública e o painel /admin.
 *
 * Ela existe porque as duas pontas da API falam de nível de formas diferentes:
 * `SkillRequest` recebe um `int` de 1 a 5, enquanto `SkillDTO` devolve o enum
 * `SkillLevel`. Sem esta tabela, o formulário de edição não conseguiria
 * pré-selecionar o nível do registro que acabou de carregar.
 */
export const SKILL_LEVELS: readonly SkillLevelMeta[] = [
  {
    level: 1,
    enumName: 'ZERO_EXPERIENCE_STILL_LEARNING',
    description: 'Zero Experience - Still Learning',
    label: 'Sem experiência — ainda aprendendo',
    tier: 0,
  },
  {
    level: 2,
    enumName: 'SOME_EXPERIENCE_STILL_LEARNING',
    description: 'Some Experience - Still Learning',
    label: 'Alguma experiência — ainda aprendendo',
    tier: 1,
  },
  {
    level: 3,
    enumName: 'INTERMEDIATE_KNOWLEDGE',
    description: 'Has intermediate knowledge about the topic',
    label: 'Conhecimento intermediário',
    tier: 2,
  },
  {
    level: 4,
    enumName: 'ADVANCED_KNOWLEDGE',
    description: 'Has advanced knowledge about the topic, but no work experience',
    label: 'Avançado — sem experiência prática',
    tier: 3,
  },
  {
    level: 5,
    enumName: 'WORK_EXPERIENCE',
    description: 'Has advanced knowledge about the topic and work experience',
    label: 'Avançado — com experiência prática',
    tier: 4,
  },
];

export const UNKNOWN_SKILL_LEVEL: SkillLevelMeta = {
  level: 0,
  enumName: 'UNKNOWN',
  description: 'Unknown',
  label: 'Desconhecido',
  tier: -1,
};

/**
 * Resolve o nível a partir de qualquer uma das três representações possíveis.
 *
 * O Jackson do back-end serializa `SkillLevel` pelo NOME da constante
 * (`"WORK_EXPERIENCE"`) — não há `write-enums-using-to-string` configurado nem
 * `@JsonValue` no enum. A descrição também é aceita porque `SkillLevel`
 * sobrescreve `toString()`, e bastaria alguém ligar aquela flag no
 * `application.yml` para o formato mudar sem aviso; casar pelas duas formas
 * deixa o front imune a essa troca. Devolve `null` para valor desconhecido, e
 * a decisão de exibir "Desconhecido" fica com quem chama.
 */
export function resolveSkillLevel(value: string | number | null | undefined): SkillLevelMeta | null {
  if (value === null || value === undefined) {
    return null;
  }

  if (typeof value === 'number') {
    return SKILL_LEVELS.find((meta) => meta.level === value) ?? null;
  }

  return (
    SKILL_LEVELS.find((meta) => meta.enumName === value || meta.description === value) ?? null
  );
}

@Injectable({ providedIn: 'root' })
export class SkillsService {
  private readonly apiService = inject(ApiService);

  getSkills() {
    return this.apiService.get<Skill[]>('/skills');
  }

  getSkillById(id: string) {
    return this.apiService.get<Skill>(`/skills/${id}`);
  }

  /**
   * Versões usadas pelo painel /admin — mesma observação de
   * `ProjectsService`: sem overlay global e sem corpo de resposta nos verbos
   * de escrita (`204 No Content`).
   */
  getSkillsForAdmin() {
    return this.apiService.get<Skill[]>('/skills', { skipGlobalLoading: true });
  }

  createSkill(request: SkillRequest) {
    return this.apiService.post<void>('/skills', request, { skipGlobalLoading: true });
  }

  updateSkill(id: string, request: SkillRequest) {
    return this.apiService.put<void>(`/skills/${id}`, request, { skipGlobalLoading: true });
  }

  deleteSkill(id: string) {
    return this.apiService.delete<void>(`/skills/${id}`, { skipGlobalLoading: true });
  }
}
