import {
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
  ChangeDetectionStrategy
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import {
  SKILL_LEVELS,
  Skill,
  SkillLevelMeta,
  SkillsService,
  UNKNOWN_SKILL_LEVEL,
  resolveSkillLevel,
} from './skills.service';

interface SkillGroup {
  category: string;
  skills: Skill[];
}

const MODAL_CLOSE_ANIMATION_MS = 180;

@Component({
  selector: 'app-skills',
  standalone: true,
  imports: [HeaderComponent, FooterComponent],
  templateUrl: './skills.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './skills.component.scss',
})
export class SkillsComponent {
  private readonly skillsService = inject(SkillsService);

  private readonly defaultErrorMessage =
    'Houve uma falha ao consultar /api/skills. Tente novamente.';

  // Vem do service para ser a mesma tabela usada pelo painel /admin. A versão
  // anterior era uma lista local que casava o nível pela DESCRIÇÃO do enum,
  // mas a API serializa o `skillLevel` pelo NOME da constante
  // ("WORK_EXPERIENCE") — nenhuma skill batia, e todas apareciam como
  // "Desconhecido" com o filtro de nível sem efeito.
  readonly levels = SKILL_LEVELS;

  readonly skills = signal<Skill[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly search = signal('');
  readonly selectedCategory = signal('all');
  readonly selectedLevel = signal('all');

  readonly activeSkill = signal<Skill | null>(null);
  readonly isModalOpen = signal(false);
  readonly isModalLoading = signal(false);
  readonly modalErrorMessage = signal<string | null>(null);

  private readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  private modalCloseTimeout?: ReturnType<typeof setTimeout>;
  private lastFocusedElement: HTMLElement | null = null;

  readonly categoryOptions = computed(() => {
    const categories = new Set(this.skills().map((skill) => skill.category));
    return Array.from(categories);
  });

  readonly filteredSkills = computed(() => {
    const search = this.search().trim().toLowerCase();
    const category = this.selectedCategory();
    const level = this.selectedLevel();

    return this.skills().filter((skill) => {
      if (category !== 'all' && skill.category !== category) {
        return false;
      }
      // Compara o nível já resolvido, e não o valor cru da API, para o filtro
      // continuar funcionando se a serialização do enum mudar.
      if (level !== 'all' && this.levelMeta(skill.skillLevel).enumName !== level) {
        return false;
      }
      if (search && !skill.name.toLowerCase().includes(search)) {
        return false;
      }
      return true;
    });
  });

  readonly groupedSkills = computed<SkillGroup[]>(() => {
    const groups: SkillGroup[] = [];
    for (const skill of this.filteredSkills()) {
      const group = groups.find((g) => g.category === skill.category);
      if (group) {
        group.skills.push(skill);
      } else {
        groups.push({ category: skill.category, skills: [skill] });
      }
    }
    return groups;
  });

  readonly hasResults = computed(() => this.filteredSkills().length > 0);
  readonly totalResults = computed(() => this.filteredSkills().length);

  constructor() {
    this.fetchSkills();

    effect(() => {
      if (this.isModalOpen()) {
        this.closeButton()?.nativeElement.focus();
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.isModalOpen()) {
      this.closeModal();
    }
  }

  fetchSkills(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.skillsService.getSkills().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(this.resolveErrorMessage(error));
      },
    });
  }

  levelMeta(skillLevel: string): SkillLevelMeta {
    return resolveSkillLevel(skillLevel) ?? UNKNOWN_SKILL_LEVEL;
  }

  levelBars(tier: number): boolean[] {
    return Array.from({ length: 4 }, (_, index) => index < tier);
  }

  clearFilters(): void {
    this.search.set('');
    this.selectedCategory.set('all');
    this.selectedLevel.set('all');
  }

  openSkill(skill: Skill, trigger: HTMLElement): void {
    clearTimeout(this.modalCloseTimeout);
    this.lastFocusedElement = trigger;

    this.activeSkill.set(null);
    this.isModalOpen.set(true);
    this.isModalLoading.set(true);
    this.modalErrorMessage.set(null);

    this.skillsService.getSkillById(skill.id).subscribe({
      next: (fullSkill) => {
        this.activeSkill.set(fullSkill);
        this.isModalLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isModalLoading.set(false);
        this.modalErrorMessage.set(this.resolveErrorMessage(error));
      },
    });
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.lastFocusedElement?.focus();
    this.lastFocusedElement = null;

    this.modalCloseTimeout = setTimeout(() => {
      this.activeSkill.set(null);
      this.modalErrorMessage.set(null);
    }, MODAL_CLOSE_ANIMATION_MS);
  }

  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 500 && error.error?.reason) {
      return error.error.reason;
    }
    return this.defaultErrorMessage;
  }
}
