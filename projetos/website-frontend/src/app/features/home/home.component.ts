import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

interface TrailStep {
  icon: string;
  label: string;
  highlighted?: boolean;
}

interface AboutTimelineEntry {
  eyebrow: string;
  title: string;
  description: string;
}

interface JourneyStep {
  number: string;
  title: string;
  description: string;
}

interface SkillCard {
  icon: string;
  title: string;
  description: string;
}

interface RoadmapCard {
  code: string;
  title: string;
  description: string;
}

interface GalleryImage {
  src: string;
  alt: string;
}

interface CoffeeAccent {
  id: string;
  src: string;
  top: string;
  left: string;
  size: string;
  rotate: string;
  opacity: string;
  /** Xícara sobre fundo claro: vai de branco em vez do azul padrão. */
  light?: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [HeaderComponent, FooterComponent, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  fullName: string = 'Felipe';
  currentJobTitle: string = 'Analista de Ongoing Júnior';
  currentJobCompany: string = 'NetBR by SEK';
  currentAge: number = this.calculateAge();

  readonly trailSteps: TrailStep[] = [
    { icon: '/images/home/icon-curiosity.svg', label: 'Curiosidade' },
    { icon: '/images/home/icon-atendimento.svg', label: 'Atendimento' },
    { icon: '/images/home/icon-apis.svg', label: 'APIs' },
    { icon: '/images/home/icon-programacao.svg', label: 'Programação' },
    { icon: '/images/home/icon-backend.svg', label: 'Back-End' },
    {
      icon: '/images/home/icon-dev-profissional.svg',
      label: 'Desenvolvimento Profissional',
      highlighted: true,
    },
  ];

  readonly aboutTimeline: AboutTimelineEntry[] = [
    {
      eyebrow: 'Infância & Interesse',
      title: 'Curiosidade e Soluções',
      description:
        'Interesse natural em encontrar soluções para problemas e entender o funcionamento das coisas desde cedo.',
    },
    {
      eyebrow: 'Primeiros Passos',
      title: 'Projetos Web Iniciais',
      description:
        'Minhas primeiras experiências reais com páginas web e a construção de interfaces simples.',
    },
    {
      eyebrow: 'Mercado',
      title: 'Atendimento ao Cliente',
      description:
        'Primeira experiência profissional, onde desenvolvi habilidades de comunicação e análise de problemas críticos.',
    },
    {
      eyebrow: 'Evolução',
      title: 'Contato com APIs e Back-End',
      description:
        'O ponto de virada: contato inicial com chamadas HTTP, sistemas de E-commerce e a descoberta do poder do Back-End.',
    },
    {
      eyebrow: 'Atualidade',
      title: 'Identity Management (IGA/IAM)',
      description:
        `Atuação focada em sustentação, testes e entrega em sistemas de Identity Governance and Administration e Identity and Access Management. Atualmente sou ${this.currentJobTitle} na ${this.currentJobCompany}, buscando a certificação IdentityIQ Engineer.`,
    },
  ];

  readonly journeySteps: JourneyStep[] = [
    {
      number: '01',
      title: 'Suporte N1 & Logs',
      description:
        "Análise detalhada de logs e investigação de erros sistêmicos que me levaram a questionar o 'porquê' das coisas.",
    },
    {
      number: '02',
      title: 'Interação com Devs',
      description:
        'O contato direto com times de desenvolvimento me mostrou a arquitetura Back-End por trás das APIs.',
    },
    {
      number: '03',
      title: 'Formação Acadêmica',
      description:
        'Cursos de Análise e Desenvolvimento de Sistemas (ADS) e aprendizado contínuo através de documentações e vídeos técnicos.',
    },
    {
      number: '04',
      title: 'Foco no Back-End',
      description:
        'Decisão de focar a carreira em Back-End, com interesse crescente em entender a integração ponta a ponta (Front-End).',
    },
  ];

  readonly skillCards: SkillCard[] = [
    {
      icon: '/images/home/skill-web-setup.svg',
      title: 'Setup de Aplicações Web',
      description:
        'Instalação e deploy de aplicações Web com Back-End (Apache Tomcat para Java e Uvicorn para Python) e integração com sistemas externos.',
    },
    {
      icon: '/images/home/skill-aws-ec2.svg',
      title: 'AWS e EC2',
      description:
        'Deploy de aplicações em instâncias EC2, incluindo configuração inicial e automação via comandos no servidor.',
    },
    {
      icon: '/images/home/skill-migration.svg',
      title: 'Migração On-Premises → AWS',
      description:
        'Acompanhamento de migrações estruturadas, focando em planejamento, comunicação entre squads e infraestrutura colaborativa.',
    },
    {
      icon: '/images/home/skill-sustentacao.svg',
      title: 'Sustentação de Aplicações',
      description:
        'Troubleshooting, análise de logs, ServiceNow, Jira, GMUDs e monitoramento ativo de ambientes produtivos.',
    },
    {
      icon: '/images/home/skill-agile.svg',
      title: 'Desenvolvimento Ágil',
      description:
        'Participação em planejamento com squads, Scrum, processos de Release, GMUD e documentação técnica detalhada.',
    },
    {
      icon: '/images/home/skill-core-tech.svg',
      title: 'Tecnologias Core',
      description:
        'Java como linguagem principal e Python para automações. Foco sólido em algoritmos, estruturas de dados e leitura de documentação.',
    },
  ];

  readonly roadmapCards: RoadmapCard[] = [
    {
      code: '01. Compartilhar',
      title: 'Compartilhar Conhecimento',
      description:
        'Aprimorar a habilidade de ensinar e explicar processos técnicos. Ensinar fortalece o próprio aprendizado.',
    },
    {
      code: '02. Front-End',
      title: 'Estudos Front-End',
      description:
        'Compreender melhor a integração entre camadas da aplicação Web para ser um desenvolvedor mais completo.',
    },
    {
      code: '03. CI/CD',
      title: 'CI/CD Automatizado',
      description:
        'Dominar pipelines de automação de testes e entrega contínua para otimizar processos manuais atuais.',
    },
  ];

  readonly galleryImages: GalleryImage[] = [
    { src: '/images/home/gallery-study.png', alt: 'Ambiente de estudos com notebook e café' },
    { src: '/images/home/gallery-frontend.png', alt: 'Representação abstrata de componentes de Front-End' },
    { src: '/images/home/gallery-architecture.jpg', alt: 'Interior arquitetônico moderno com concreto e vidro' },
  ];

  // Xícaras decorativas espalhadas por uma camada única sobre a página inteira.
  // `top`/`left` são percentuais da altura/largura total da Home. Os valores de
  // `top` foram calibrados contra a altura real medida em 1440px de largura
  // (~5790px), em que as sections ficam assim: hero 2-12%, sobre mim 18-42%,
  // jornada 42-54%, habilidades 54-67%, roadmap 68-77%, galeria 78-87%,
  // contato 88-94%. A galeria fica de fora de propósito: silhueta sobre foto
  // vira borrão. Como a camada é desenhada por cima do conteúdo, as opacidades
  // ficam baixas para não criar véu sobre o texto.
  // `id` existe porque o @for precisa de track único e há arquivo repetido.
  readonly coffeeAccents: CoffeeAccent[] = [
    { id: 'hero', src: '/images/home/coffee-svgrepo-com.svg', top: '3%', left: '-3%', size: '200px', rotate: '-14deg', opacity: '0.11' },
    { id: 'about', src: '/images/home/coffee-svgrepo-com-2.svg', top: '21%', left: '85%', size: '230px', rotate: '10deg', opacity: '0.1' },
    { id: 'about-lower', src: '/images/home/coffee-svgrepo-com-3.svg', top: '34%', left: '1%', size: '150px', rotate: '-18deg', opacity: '0.09' },
    { id: 'journey', src: '/images/home/coffee-svgrepo-com-4.svg', top: '46%', left: '88%', size: '190px', rotate: '8deg', opacity: '0.11' },
    { id: 'skills', src: '/images/home/coffee-svgrepo-com-2.svg', top: '58%', left: '1%', size: '165px', rotate: '-8deg', opacity: '0.1' },
    { id: 'roadmap', src: '/images/home/coffee-svgrepo-com-3.svg', top: '71%', left: '86%', size: '180px', rotate: '16deg', opacity: '0.1' },
    { id: 'cta', src: '/images/home/coffee-svgrepo-com.svg', top: '90%', left: '66%', size: '200px', rotate: '-10deg', opacity: '0.3', light: true },
  ];

  calculateAge(): number {
    const birthYear = 2000;
    const today = new Date();
    const age = today.getFullYear() - birthYear;
    return age;
  }
}
