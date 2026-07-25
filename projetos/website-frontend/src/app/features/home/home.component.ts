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
  currentJobWebsiteURL: string = 'https://www.netbr.com.br/';
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

  calculateAge(): number {
    const birthYear = 2000;
    const today = new Date();
    const age = today.getFullYear() - birthYear;
    return age;
  }
}
