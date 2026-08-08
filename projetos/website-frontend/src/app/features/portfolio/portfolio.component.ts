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
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { CertificatesComponent } from './certificates/certificates.component';
import { Project, ProjectDetail, ProjectsService } from './projects.service';

const MODAL_CLOSE_ANIMATION_MS = 180;

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [HeaderComponent, FooterComponent, CertificatesComponent, DatePipe],
  templateUrl: './portfolio.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './portfolio.component.scss',
})
export class PortfolioComponent {
  private readonly projectsService = inject(ProjectsService);

  private readonly defaultErrorMessage =
    'Houve uma falha ao consultar /api/projects. Tente novamente.';

  readonly projects = signal<Project[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly search = signal('');

  readonly activeProject = signal<ProjectDetail | null>(null);
  readonly isModalOpen = signal(false);
  readonly isModalLoading = signal(false);
  readonly modalErrorMessage = signal<string | null>(null);

  private readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  private modalCloseTimeout?: ReturnType<typeof setTimeout>;
  private lastFocusedElement: HTMLElement | null = null;

  readonly filteredProjects = computed(() => {
    const search = this.search().trim().toLowerCase();
    if (!search) {
      return this.projects();
    }
    return this.projects().filter((project) => project.name.toLowerCase().includes(search));
  });

  readonly hasResults = computed(() => this.filteredProjects().length > 0);
  readonly totalResults = computed(() => this.filteredProjects().length);

  constructor() {
    this.fetchProjects();

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

  fetchProjects(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectsService.getProjects().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMessage.set(this.resolveErrorMessage(error));
      },
    });
  }

  openProject(project: Project, trigger: HTMLElement): void {
    clearTimeout(this.modalCloseTimeout);
    this.lastFocusedElement = trigger;

    this.activeProject.set(null);
    this.isModalOpen.set(true);
    this.isModalLoading.set(true);
    this.modalErrorMessage.set(null);

    this.projectsService.getProjectById(project.id).subscribe({
      next: (fullProject) => {
        this.activeProject.set(fullProject);
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
      this.activeProject.set(null);
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
