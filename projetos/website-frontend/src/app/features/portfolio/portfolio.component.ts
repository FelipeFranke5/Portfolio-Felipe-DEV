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
      issuer: 'SailPoint',
      name: 'Build an Access Model in Identity Security Cloud',
      description: 'Curso básico de Angular na versão v18, no qual apresenta os conceitos básicos e funcionalidades disponíveis. Após a conclusão deste breve curso, aprendi como iniciar uma aplicação, realizar o build e criar features novas com base em uma arquitetura simples. Aprendi também que este framework possui similaridades com o Spring Boot (como por exemplo o uso de DI).',
      issuedAt: 'Agosto de 2026',
      certificateUrl: 'https://fernandakipper.com/certificado/d47841ed-7ca4-4162-b099-d87125b99cf0',
      imageSrc: '/images/portfolio/certificates/certificate-ndidvezi5m2f-1785786526.jpg',
    },
    {
      issuer: 'KipperDev Marketing e Treinamentos',
      name: 'Curso de Angular v18',
      description: 'Curso básico de Angular na versão v18, no qual apresenta os conceitos básicos e funcionalidades disponíveis. Após a conclusão deste breve curso, aprendi como iniciar uma aplicação, realizar o build e criar features novas com base em uma arquitetura simples. Aprendi também que este framework possui similaridades com o Spring Boot (como por exemplo o uso de DI).',
      issuedAt: 'Julho de 2026',
      certificateUrl: 'https://fernandakipper.com/certificado/d47841ed-7ca4-4162-b099-d87125b99cf0',
      imageSrc: '/images/portfolio/certificates/certificate-angular-v18.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'SailPoint Certified IdentityIQ Engineer',
      description: 'Curso completo de IdentityIQ, abordando os principais temas desta solução completa de IGA - Desde a configuração inicial, até a construção de Workflows, geração de relatórios personalizados, instalação de aplicações utilizando conectores, controles rigorosos de acesso aplicando o principio do menor privilégio, entre outras. Este curso me capacitou para atuar na sustentação de um ambiente on-premise que utiliza diversos servidores (linux e windows) e estes servidores são integrados ao IdentityIQ - Incluindo a instalação de patches de segurança e upgrade de versões da ferramenta.',
      issuedAt: 'Julho de 2026',
      certificateUrl: 'https://www.credly.com/badges/c2174a1f-0c8e-4fa7-be1f-c5301710a54d/public_url',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-identityiq-engineer.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'Set Up and Administer Identity Security Cloud',
      description: 'Curso completo de Identity Security para implementadores técnicos. Após realizar este curso, aprendi sobre como realizar uma configuração completa de um Tenant, quais configurações temos disponíveis, como podemos conectar sources (sejam autoritativas ou não) permitindo a integração de aplicações conectadas dos clientes, como os acessos são carregados no sistema e como ocorre o processo de provisionamento na ferramenta. Após aprender estes conceitos, é possível auxiliar outros clientes que podem ter dúvidas sobre a ferramenta, explicando os detalhes dos fluxos e das possibilidades de integração. Aqui aprendemos também sobre conceitos fundamentais, como a conexão com as VAs, arquitetura da solução, quais liberações de rede são necessárias e quais decisões devem ser tomadas durante a instalação de um Tenant.',
      issuedAt: 'Agosto de 2025',
      certificateUrl: 'https://verify.skilljar.com/c/7eugin4petmm',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-isc-setup-administer.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'SailPoint Identity Security Leader Credential',
      description: 'Após finalizar este curso, aprendi sobre quais são as melhores práticas durante o planejamento de novos projetos que utilizam ou pretendem utilizar o SailPoint Identity Security Cloud como ferramenta principal para federação das Identidades e Acessos, tendo o mindset de um Gerente de Projetos ou Líder Técnico - Considerando quais pontos levar em consideração para uma modelagem de acessos bem-sucedida, uma coleta efetiva de requisitos e um planejamento adequado. Além disso, o curso ensina sobre quais técnicas podemos adotar para casos onde os projetos já possuem uma implementação e estão em uma fase madura.',
      issuedAt: 'Setembro de 2025',
      certificateUrl: 'https://verify.skilljar.com/c/hvpwt534z3ck',
      imageSrc: '/images/portfolio/certificates/certificate-sailpoint-identity-security-leader.jpg',
    },
    {
      issuer: 'SailPoint',
      name: 'Introduction to Identity Security Cloud',
      description: 'Curso básico de Identity Security Cloud (conceitos iniciais), a nova solução cloud da SailPoint capaz de providenciar controle completo de acessos, estados de ciclo de vida (funcionários, terceiros ou mesmo contas de serviço), certificações, relatórios e a centralização das aplicações conectadas na infraestrutura dos clientes. Após finalizar este curso, pude entender quais são as vantagens de utilizar esta solução e os principais componentes que compõem esta ferramenta.',
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
