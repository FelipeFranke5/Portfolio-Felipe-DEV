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
  PROJECT_DESCRIPTION_MAX_LENGTH,
  PROJECT_DESCRIPTION_MIN_LENGTH,
  PROJECT_GITHUB_URL_PATTERN,
  PROJECT_NAME_MAX_LENGTH,
  PROJECT_NAME_MIN_LENGTH,
  Project,
  ProjectRequest,
  ProjectsService,
  emptyToNull,
  parseStack,
} from '../../portfolio/projects.service';
import { SessionIdleService } from '../../../core/services/session-idle.service';
import {
  flattenValidationErrors,
  isAuthError,
  resolveApiErrorMessage,
} from '../../../core/services/api-error';

type FormMode = 'create' | 'edit';
type ProjectFieldName = 'name' | 'description' | 'stack' | 'githubURL' | 'demoURL';

const DEMO_URL_PATTERN = /^https?:\/\/.+/;

@Component({
  selector: 'app-admin-projects',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './admin-projects.component.html',
  styleUrl: './admin-projects.component.scss',
})
export class AdminProjectsComponent {
  private readonly projectsService = inject(ProjectsService);
  private readonly sessionIdle = inject(SessionIdleService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  private readonly listErrorMessage =
    'Não foi possível carregar os projetos. Tente novamente.';
  private readonly saveErrorMessage =
    'Não foi possível salvar o projeto. Revise os dados e tente novamente.';
  private readonly deleteErrorMessage = 'Não foi possível excluir o projeto. Tente novamente.';

  readonly nameMinLength = PROJECT_NAME_MIN_LENGTH;
  readonly nameMaxLength = PROJECT_NAME_MAX_LENGTH;
  readonly descriptionMinLength = PROJECT_DESCRIPTION_MIN_LENGTH;
  readonly descriptionMaxLength = PROJECT_DESCRIPTION_MAX_LENGTH;

  readonly projects = signal<Project[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly search = signal('');
  readonly successMessage = signal<string | null>(null);

  readonly formMode = signal<FormMode | null>(null);
  readonly editingProjectId = signal<string | null>(null);
  readonly editingProjectName = signal<string>('');
  readonly isFormLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly formErrorMessage = signal<string | null>(null);
  /** Erros por campo devolvidos pelo back-end no 422. */
  readonly serverFieldErrors = signal<Record<string, string[]>>({});

  readonly deleteTarget = signal<Project | null>(null);
  readonly isDeleting = signal(false);
  readonly deleteErrorMessageSignal = signal<string | null>(null);

  private readonly nameInput = viewChild<ElementRef<HTMLInputElement>>('nameInput');
  private readonly deleteConfirmButton =
    viewChild<ElementRef<HTMLButtonElement>>('deleteConfirmButton');

  private lastFocusedElement: HTMLElement | null = null;

  readonly projectForm = this.formBuilder.nonNullable.group({
    name: [
      '',
      [
        Validators.required,
        Validators.minLength(PROJECT_NAME_MIN_LENGTH),
        Validators.maxLength(PROJECT_NAME_MAX_LENGTH),
      ],
    ],
    description: [
      '',
      [
        Validators.required,
        Validators.minLength(PROJECT_DESCRIPTION_MIN_LENGTH),
        Validators.maxLength(PROJECT_DESCRIPTION_MAX_LENGTH),
      ],
    ],
    stack: ['', [Validators.required]],
    githubURL: ['', [Validators.pattern(PROJECT_GITHUB_URL_PATTERN)]],
    demoURL: ['', [Validators.pattern(DEMO_URL_PATTERN)]],
    featured: [false],
  });

  readonly filteredProjects = computed(() => {
    const search = this.search().trim().toLowerCase();

    if (!search) {
      return this.projects();
    }

    return this.projects().filter(
      (project) =>
        project.name.toLowerCase().includes(search) ||
        project.stack.some((tech) => tech.toLowerCase().includes(search))
    );
  });

  readonly totalResults = computed(() => this.filteredProjects().length);
  readonly hasResults = computed(() => this.totalResults() > 0);
  readonly isModalOpen = computed(() => this.formMode() !== null);

  /** Pré-visualização da stack enquanto o usuário digita a lista separada por vírgula. */
  readonly stackPreview = signal<string[]>([]);

  constructor() {
    this.fetchProjects();

    this.projectForm.controls.stack.valueChanges.subscribe((value) => {
      this.stackPreview.set(parseStack(value));
    });

    // O dashboard linka para cá com ?action=create para abrir o formulário
    // direto, sem um clique extra.
    if (this.route.snapshot.queryParamMap.get('action') === 'create') {
      this.openCreate(null);
    }

    effect(() => {
      if (this.formMode() && !this.isFormLoading()) {
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

  fetchProjects(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectsService.getProjectsForAdmin().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.handleAuthError(error);
        this.errorMessage.set(resolveApiErrorMessage(error, this.listErrorMessage));
      },
    });
  }

  openCreate(trigger: HTMLElement | null): void {
    this.lastFocusedElement = trigger;
    this.resetForm();
    this.editingProjectId.set(null);
    this.editingProjectName.set('');
    this.formMode.set('create');
  }

  /**
   * A listagem devolve a descrição truncada em 50 caracteres pelo back-end, sem
   * qualquer marcação de que foi cortada. Preencher o formulário com o valor da
   * lista faria o PUT gravar a descrição truncada por cima da original, então a
   * edição SEMPRE recarrega o registro completo por ID antes de abrir.
   */
  openEdit(project: Project, trigger: HTMLElement): void {
    this.lastFocusedElement = trigger;
    this.resetForm();
    this.editingProjectId.set(project.id);
    this.editingProjectName.set(project.name);
    this.formMode.set('edit');
    this.isFormLoading.set(true);

    this.projectsService.getProjectByIdForAdmin(project.id).subscribe({
      next: (detail) => {
        this.projectForm.setValue({
          name: detail.name,
          description: detail.description,
          stack: detail.stack.join(', '),
          githubURL: detail.githubURL ?? '',
          demoURL: detail.demoURL ?? '',
          featured: detail.featured,
        });
        this.isFormLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isFormLoading.set(false);
        this.handleAuthError(error);
        this.formErrorMessage.set(resolveApiErrorMessage(error, this.listErrorMessage));
      },
    });
  }

  closeForm(): void {
    this.formMode.set(null);
    this.isFormLoading.set(false);
    this.restoreFocus();
  }

  submit(): void {
    if (this.projectForm.invalid) {
      this.projectForm.markAllAsTouched();
      return;
    }

    const raw = this.projectForm.getRawValue();
    const request: ProjectRequest = {
      name: raw.name.trim(),
      description: raw.description.trim(),
      stack: parseStack(raw.stack),
      githubURL: emptyToNull(raw.githubURL),
      demoURL: emptyToNull(raw.demoURL),
      featured: raw.featured,
    };

    if (request.stack.length === 0) {
      this.projectForm.controls.stack.setErrors({ required: true });
      this.projectForm.controls.stack.markAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.formErrorMessage.set(null);
    this.serverFieldErrors.set({});

    const editingId = this.editingProjectId();
    const save$ = editingId
      ? this.projectsService.updateProject(editingId, request)
      : this.projectsService.createProject(request);

    save$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.closeForm();
        this.successMessage.set(
          editingId
            ? `Projeto "${request.name}" atualizado com sucesso.`
            : `Projeto "${request.name}" criado com sucesso.`
        );
        // POST e PUT respondem 204 sem corpo: a lista só se atualiza refazendo o GET.
        this.fetchProjects();
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.handleAuthError(error);
        this.serverFieldErrors.set(flattenValidationErrors(error));
        this.formErrorMessage.set(resolveApiErrorMessage(error, this.saveErrorMessage));
      },
    });
  }

  askDeleteConfirmation(project: Project, trigger: HTMLElement): void {
    this.lastFocusedElement = trigger;
    this.deleteErrorMessageSignal.set(null);
    this.deleteTarget.set(project);
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

    this.projectsService.deleteProject(target.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.closeDeleteConfirm();
        this.successMessage.set(`Projeto "${target.name}" excluído.`);
        this.fetchProjects();
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

  isFieldInvalid(fieldName: ProjectFieldName): boolean {
    const control = this.projectForm.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }

  fieldErrors(fieldName: ProjectFieldName): string[] {
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
    this.projectForm.reset({
      name: '',
      description: '',
      stack: '',
      githubURL: '',
      demoURL: '',
      featured: false,
    });
    this.stackPreview.set([]);
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
