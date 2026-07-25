import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../core/services/api.service';

export interface Skill {
  id: string;
  name: string;
  category: string;
  skillLevel: string;
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
}
