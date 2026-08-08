import {
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import {
  SKILL_CATEGORY_MAX_LENGTH,
  SKILL_LEVELS,
  SKILL_NAME_MAX_LENGTH,
  Skill,
  SkillLevelMeta,
  SkillRequest,
  SkillsService,
  UNKNOWN_SKILL_LEVEL,
  resolveSkillLevel,
} from '../../skills/skills.service';
import { SessionIdleService } from '../../../core/services/session-idle.service';
import {
  flattenValidationErrors,
  isAuthError,
  resolveApiErrorMessage,
} from '../../../core/services/api-error';

type FormMode = 'create' | 'edit';
type SkillFieldName = 'name' | 'category' | 'level';

interface SkillGroup {
  category: string;
  skills: Skill[];
}

@Component({
  selector: 'app-admin-skills',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-skills.component.html',
  styleUrl: './admin-skills.component.scss',
})
export class AdminSkillsComponent {
  private readonly skillsService = inject(SkillsService);
  private readonly sessionIdle = inject(SessionIdleService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  private readonly listErrorMessage = 'Não foi possível carregar as skills. Tente novamente.';
  private readonly saveErrorMessage =
    'Não foi possível salvar a skill. Revise os dados e tente novamente.';
  private readonly deleteErrorMessage = 'Não foi possível excluir a skill. Tente novamente.';

  readonly levels = SKILL_LEVELS;
  readonly nameMaxLength = SKILL_NAME_MAX_LENGTH;
  readonly categoryMaxLength = SKILL_CATEGORY_MAX_LENGTH;

  readonly skills = signal<Skill[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');
  readonly successMessage = signal<string | null>(null);

  readonly formMode = signal<FormMode | null>(null);
  readonly editingSkillId = signal<string | null>(null);
  readonly editingSkillName = signal<string>('');
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);
  readonly serverFieldErrors = signal<Record<string, string[]>>({});

  readonly deleteTarget = signal<Skill | null>(null);
  readonly isDeleting = signal(false);
  readonly deleteErrorMessageSignal = signal<string | null>(null);

  private readonly nameInput = viewChild<ElementRef<HTMLInputElement>>('nameInput');
  private readonly deleteConfirmButton =
    viewChild<ElementRef<HTMLButtonElement>>('deleteConfirmButton');

  private lastFocusedElement: HTMLElement | null = null;

  readonly skillForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(SKILL_NAME_MAX_LENGTH)]],
    category: ['', [Validators.required, Validators.maxLength(SKILL_CATEGORY_MAX_LENGTH)]],
    level: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
  });

  readonly filteredSkills = computed(() => {
    const search = this.search().trim().toLowerCase();

    if (!search) {
      return this.skills();
    }

    return this.skills().filter(
      (skill) =>
        skill.name.toLowerCase().includes(search) ||
        skill.category.toLowerCase().includes(search)
    );
  });

  /** Categorias já cadastradas, oferecidas como sugestão no formulário. */
  readonly categoryOptions = computed(() =>
    Array.from(new Set(this.skills().map((skill) => skill.category))).sort()
  );

  readonly groupedSkills = computed<SkillGroup[]>(() => {
    const groups: SkillGroup[] = [];

    for (const skill of this.filteredSkills()) {
      const group = groups.find((candidate) => candidate.category === skill.category);

      if (group) {
        group.skills.push(skill);
      } else {
        groups.push({ category: skill.category, skills: [skill] });
      }
    }

    return groups;
  });

  readonly totalResults = computed(() => this.filteredSkills().length);
  readonly hasResults = computed(() => this.totalResults() > 0);
  readonly isModalOpen = computed(() => this.formMode() !== null);

  constructor() {
    this.fetchSkills();

    if (this.route.snapshot.queryParamMap.get('action') === 'create') {
      this.openCreate(null);
    }

    effect(() => {
      if (this.formMode()) {
        this.nameInput()?.nativeElement.focus({ preventScroll: true });
      }
    });

    effect(() => {
      if (this.deleteTarget()) {
        this.deleteConfirmButton()?.nativeElement.focus();
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.deleteTarget()) {
      this.closeDeleteConfirm();
      return;
    }

    if (this.isModalOpen()) {
      this.closeForm();
    }
  }

  fetchSkills(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.skillsService.getSkillsForAdmin().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.handleAuthError(error);
        this.errorMessage.set(resolveApiErrorMessage(error, this.listErrorMessage));
      },
    });
  }

  levelMeta(skillLevel: string): SkillLevelMeta {
    return resolveSkillLevel(skillLevel) ?? UNKNOWN_SKILL_LEVEL;
  }

  levelBars(tier: number): boolean[] {
    return Array.from({ length: 4 }, (_, index) => index < tier);
  }

  openCreate(trigger: HTMLElement | null): void {
    this.lastFocusedElement = trigger;
    this.resetForm();
    this.editingSkillId.set(null);
    this.editingSkillName.set('');
    this.formMode.set('create');
  }

  /**
   * Diferente de projetos, a listagem de skills já traz o registro completo —
   * não há truncamento no back-end, então não é preciso buscar por ID antes de
   * editar. O único ajuste é traduzir o enum devolvido pela API de volta para o
   * número de 1 a 5 que o `SkillRequest` espera.
   */
  openEdit(skill: Skill, trigger: HTMLElement): void {
    this.lastFocusedElement = trigger;
    this.resetForm();
    this.editingSkillId.set(skill.id);
    this.editingSkillName.set(skill.name);
    this.formMode.set('edit');

    this.skillForm.setValue({
      name: skill.name,
      category: skill.category,
      level: resolveSkillLevel(skill.skillLevel)?.level ?? 0,
    });

    if (!resolveSkillLevel(skill.skillLevel)) {
      this.formErrorMessage.set(
        "O nível atual desta habilidade (Skill) não foi reconhecida. Por favor, escolha um nível antes de salvar."
      );
    }
  }

  closeForm(): void {
    this.formMode.set(null);
    this.restoreFocus();
  }

  submit(): void {
    if (this.skillForm.invalid) {
      this.skillForm.markAllAsTouched();
      return;
    }

    const raw = this.skillForm.getRawValue();
    const request: SkillRequest = {
      name: raw.name.trim(),
      category: raw.category.trim(),
      level: Number(raw.level),
    };

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);
    this.serverFieldErrors.set({});

    const editingId = this.editingSkillId();
    const save$ = editingId
      ? this.skillsService.updateSkill(editingId, request)
      : this.skillsService.createSkill(request);

    save$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeForm();
        this.successMessage.set(
          editingId
            ? `Skill "${request.name}" atualizada com sucesso.`
            : `Skill "${request.name}" criada com sucesso.`
        );
        // 204 sem corpo: a lista só reflete a mudança após um novo GET.
        this.fetchSkills();
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.handleAuthError(error);
        this.serverFieldErrors.set(flattenValidationErrors(error));
        this.formErrorMessage.set(resolveApiErrorMessage(error, this.saveErrorMessage));
      },
    });
  }

  askDeleteConfirmation(skill: Skill, trigger: HTMLElement): void {
    this.lastFocusedElement = trigger;
    this.deleteErrorMessageSignal.set(null);
    this.deleteTarget.set(skill);
  }

  closeDeleteConfirm(): void {
    this.deleteTarget.set(null);
    this.restoreFocus();
  }

  confirmDelete(): void {
    const target = this.deleteTarget();

    if (!target) {
      return;
    }

    this.isDeleting.set(true);
    this.deleteErrorMessageSignal.set(null);

    this.skillsService.deleteSkill(target.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeDeleteConfirm();
        this.successMessage.set(`Skill "${target.name}" excluída.`);
        this.fetchSkills();
      },
      error: (error: HttpErrorResponse) => {
        this.isDeleting.set(false);
        this.handleAuthError(error);
        this.deleteErrorMessageSignal.set(
          resolveApiErrorMessage(error, this.deleteErrorMessage)
        );
      },
    });
  }

  isFieldInvalid(fieldName: SkillFieldName): boolean {
    const control = this.skillForm.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }

  fieldErrors(fieldName: SkillFieldName): string[] {
    return this.serverFieldErrors()[fieldName] ?? [];
  }

  onFormBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeForm();
    }
  }

  onDeleteBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeDeleteConfirm();
    }
  }

  dismissSuccess(): void {
    this.successMessage.set(null);
  }

  private resetForm(): void {
    this.skillForm.reset({ name: '', category: '', level: 3 });
    this.formErrorMessage.set(null);
    this.serverFieldErrors.set({});
    this.successMessage.set(null);
  }

  private restoreFocus(): void {
    this.lastFocusedElement?.focus();
    this.lastFocusedElement = null;
  }

  private handleAuthError(error: HttpErrorResponse): void {
    if (isAuthError(error)) {
      this.sessionIdle.markExpired();
    }
  }
}
