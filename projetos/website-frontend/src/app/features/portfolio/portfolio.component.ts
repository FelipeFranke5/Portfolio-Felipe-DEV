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
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { Project, ProjectDetail, ProjectsService } from './projects.service';

const MODAL_CLOSE_ANIMATION_MS = 180;

const CERTIFICATE_PLACEHOLDER_DESCRIPTION =
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.';

interface Certificate {
  issuer: string;
  name: string;
  description: string;
  issuedAt: string;
  certificateUrl: string;
  imageSrc: string;
}

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [HeaderComponent, FooterComponent, DatePipe],
  templateUrl: './portfolio.component.html',
  styleUrl: './portfolio.component.scss',
})
export class PortfolioComponent {
  private readonly projectsService = inject(ProjectsService);

  readonly certificates: Certificate[] = [
    {
      issuer: 'KipperDev Marketing e Treinamentos',
      name: 'Curso de Angular v18',
      description: CERTIFICATE_PLACEHOLDER_DESCRIPTION,
      issuedAt: 'Julho de 2026',
      certificateUrl: 'https://fernandakipper.com/certificado/d47841ed-7ca4-4162-b099-d87125b99cf0',
      imageSrc: '/images/portfolio/certificates/certificate-angular-v18.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'SailPoint Certified IdentityIQ Engineer',
      description: CERTIFICATE_PLACEHOLDER_DESCRIPTION,
      issuedAt: 'Julho de 2026',
      certificateUrl: 'https://www.credly.com/badges/c2174a1f-0c8e-4fa7-be1f-c5301710a54d/public_url',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-identityiq-engineer.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'Set Up and Administer Identity Security Cloud',
      description: CERTIFICATE_PLACEHOLDER_DESCRIPTION,
      issuedAt: 'Agosto de 2025',
      certificateUrl: 'https://verify.skilljar.com/c/7eugin4petmm',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-isc-setup-administer.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'SailPoint Identity Security Leader Credential',
      description: CERTIFICATE_PLACEHOLDER_DESCRIPTION,
      issuedAt: 'Setembro de 2025',
      certificateUrl: 'https://verify.skilljar.com/c/hvpwt534z3ck',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-identity-security-leader.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'Introduction to Identity Security Cloud',
      description: CERTIFICATE_PLACEHOLDER_DESCRIPTION,
      issuedAt: 'Agosto de 2025',
      certificateUrl: 'https://verify.skilljar.com/c/tqgjrowfdt38',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-isc-introduction.jpg',
    },
  ];

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
