import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../core/services/api.service';

export interface Project {
  id: string;
  name: string;
  description: string;
  stack: string[];
}

export interface ProjectDetail extends Project {
  githubURL: string | null;
  demoURL: string | null;
  featured: boolean;
  createdAt: string;
}

/** Espelha o `ProjectRequest` do back-end (POST e PUT usam o mesmo payload). */
export interface ProjectRequest {
  name: string;
  description: string;
  stack: string[];
  githubURL: string | null;
  demoURL: string | null;
  featured: boolean;
}

/** Limites do `ProjectRequest`, replicados aqui para validar antes de enviar. */
export const PROJECT_NAME_MIN_LENGTH = 5;
export const PROJECT_NAME_MAX_LENGTH = 100;
export const PROJECT_DESCRIPTION_MIN_LENGTH = 5;
export const PROJECT_DESCRIPTION_MAX_LENGTH = 500;
export const PROJECT_GITHUB_URL_PATTERN = /^https:\/\/github\.com\/.+/;

/**
 * Converte o texto do campo de stack (itens separados por vírgula) na lista
 * que o back-end espera, descartando espaços e entradas vazias.
 */
export function parseStack(rawStack: string): string[] {
  return rawStack
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

/**
 * Campo de URL opcional vazio precisa virar `null`, NÃO string vazia.
 *
 * `ProjectRequest.githubURL` tem `@Pattern(^https://github\.com/.+)` no
 * back-end, e `@Pattern` não ignora string vazia — mandar `""` reprovaria a
 * validação de um campo que é opcional. Com `null` a anotação é pulada.
 */
export function emptyToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim() ?? '';
  return trimmed.length > 0 ? trimmed : null;
}

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly apiService = inject(ApiService);

  getProjects() {
    return this.apiService.get<Project[]>('/projects');
  }

  getProjectById(id: string) {
    return this.apiService.get<ProjectDetail>(`/projects/${id}`);
  }

  /**
   * Versões usadas pelo painel /admin. Suprimem o overlay global de
   * carregamento porque as telas de lá têm estado próprio, e os três verbos de
   * escrita devolvem `204 No Content` — não há corpo de resposta, então quem
   * chama precisa recarregar a lista depois.
   */
  getProjectsForAdmin() {
    return this.apiService.get<Project[]>('/projects', { skipGlobalLoading: true });
  }

  getProjectByIdForAdmin(id: string) {
    return this.apiService.get<ProjectDetail>(`/projects/${id}`, { skipGlobalLoading: true });
  }

  createProject(request: ProjectRequest) {
    return this.apiService.post<void>('/projects', request, { skipGlobalLoading: true });
  }

  updateProject(id: string, request: ProjectRequest) {
    return this.apiService.put<void>(`/projects/${id}`, request, { skipGlobalLoading: true });
  }

  deleteProject(id: string) {
    return this.apiService.delete<void>(`/projects/${id}`, { skipGlobalLoading: true });
  }
}
