import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  effect,
  signal,
  viewChild,
} from '@angular/core';

const MODAL_CLOSE_ANIMATION_MS = 180;

export interface Certificate {
  issuer: string;
  name: string;
  description: string;
  issuedAt: string;
  certificateUrl: string;
  imageSrc: string;
}

@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [],
  templateUrl: './certificates.component.html',
  styleUrl: './certificates.component.scss',
})
export class CertificatesComponent implements OnDestroy {
  readonly certificates: Certificate[] = [
    {
      issuer: 'SailPoint',
      name: 'Build an Access Model in Identity Security Cloud',
      description: 'Após concluir este curso, aprendi sobre todas as etapas necessárias para a construção da modelagem de acessos no Identity Security Cloud, bem como as melhores práticas que devemos considerar, pensando na manutenção futura. Diferentes modelos foram apresentados (two-tier, flat) para Roles, Access Profiles e Entitlements (o nível mais individual); Como configurar esses acessos na ferramenta, utilizar roles dinâmicas para evitar o uso de muitas roles na organização e a importância das informações advindas das bases autoritativas (ou das aplicações que possuem os grupos / acessos efetivamente) estarem limpas (higienização, validação e integridade dos dados) para uma modelagem completa. Aprendi também que é possível determinar quais usuários devem participar da elaboração e da manutenção de um modelo de roles (role owner, times de sustentação, assurance, etc) e também como criar / alterar roles no painel da SailPoint, utilizando recursos de IA para obter ajuda na construção e manutenção (sugestões de roles para criar, quantas Identidades possuem pelo menos uma role, etc).',
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

  readonly activeCertificate = signal<Certificate | null>(null);
  readonly isModalOpen = signal(false);

  private readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  private modalCloseTimeout?: ReturnType<typeof setTimeout>;
  private lastFocusedElement: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.isModalOpen()) {
        this.closeButton()?.nativeElement.focus();
      }
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.modalCloseTimeout);
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.isModalOpen()) {
      this.closeModal();
    }
  }

  openCertificate(certificate: Certificate, trigger: HTMLElement): void {
    clearTimeout(this.modalCloseTimeout);
    this.lastFocusedElement = trigger;

    this.activeCertificate.set(certificate);
    this.isModalOpen.set(true);
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
      this.activeCertificate.set(null);
    }, MODAL_CLOSE_ANIMATION_MS);
  }
}
