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

@Injectable({ providedIn: 'root' })
export class ProjectsService {
  private readonly apiService = inject(ApiService);

  getProjects() {
    return this.apiService.get<Project[]>('/projects');
  }

  getProjectById(id: string) {
    return this.apiService.get<ProjectDetail>(`/projects/${id}`);
  }
}
