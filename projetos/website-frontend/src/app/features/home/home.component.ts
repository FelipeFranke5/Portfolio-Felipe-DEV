import { Component, ChangeDetectionStrategy } from '@angular/core';
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
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  fullName = 'Felipe';
  currentJobTitle = 'Analista de Ongoing Júnior';
  currentJobCompany = 'NetBR by SEK';
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
      eyebrow: 'Infância e Interesse',
      title: 'Curiosidade e Busca de Soluções',
      description:
        'Desde cedo tive curiosidade em saber o porquê das coisas. Mesmo antes de saber programar, buscava tentar entender as etapas lógicas de tudo que acontecia na minha vida e na vida da minha familia. Sempre fui uma pessoa tímida, mas que gosta de pensar bastante - Isso é fundamental em qualquer área, mas hoje vejo o quão importante é ter sempre um tempo para refletir, seja sobre uma solução para um problema, ou sobre questionamentos da vida.',
    },
    {
      eyebrow: 'Primeiros Passos - Dev',
      title: 'Projetos Iniciais',
      description:
        'Surgem as primeiras experiências reais com a construção de algoritmos. Mais ou menos no fim do Ensino Médio, descobri os vídeos do Gustavo Guanabara (curso em video) sobre estrutura de dados e algoritmos - Com isso, pude começar a criar alguns algoritmos e scripts em Python.',
    },
    {
      eyebrow: 'Entrada no Mercado de Trabalho',
      title: 'Suporte N1',
      description:
        'Primeira experiência profissional como Suporte N1. Permitiu que eu aprendesse MUITO sobre procedimentos, regras de negócio e leitura de documentações - Por muito tempo, tive contato com a área de E-commerce em uma adquirente, onde aprendi sobre como analisar logs e entender sobre alguns fluxos que ocorrem no Back-end. Sou grato à todos que responderam os meus chamados no Jira com detalhes técnicos, eu sempre parava para ler e tentar entender melhor sobre os processos quando tinha tempo.',
    },
    {
      eyebrow: 'Evolução',
      title: 'Contato com APIs e Back-End',
      description:
        'Um pouco antes da minha primeira oportunidade como Desenvolvedor de fato e ainda na empresa atuando como Suporte N1, comecei a me interessar cada vez mais por APIs, Banco de Dados e integrações. A empresa tinha um ambiente Sandbox no qual eu conseguia disparar requisições para criar transações fictícias e isso foi muito importante no meu aprendizado. Além disso, eu já participava de reuniões técnicas, onde eu ajudava outros desenvolvedores e gestores com relação à integração das APIs da empresa - Não apenas eu auxiliava os desenvolvedores, mas também aprendia com eles! Conheci pessoas que trabalhavam em todas as pontas: Desde um Front-end interessado em fazer uma nova integração e tinha dúvidas de como usar as APIs, até Desenvolvedores Back-End com sugestões de melhorias. Nessa época eu costumava enviar as sugestões dos clientes (e outras ideias que eu mesmo tinha) para a empresa e fico feliz de lembrar que algumas delas ajudaram no crescimento da empresa.',
    },
    {
      eyebrow: 'Cursos e Projetos Pessoais',
      title: 'Curso de Django, Leitura de Livros e Criação de Projetos',
      description:
        'Comecei a ler alguns livros para tentar me aprofundar em Algoritmos e Estrutura de Dados, aprender sobre Python e JavaScript. Confesso que não gostei muito do livro de JavaScript (aquele que é bem grande), mas li dois sobre Python que gostei bastante e me aprofundei: Um deles focado mais em explicar sobre Algoritmos e Estrutura de Dados (com trechos de exemplo em Python) e outro focado na linguagem Python em si, além de alguns frameworks como Django. Em paralelo, iniciei um curso de Django, onde aprendi como criar projetos, conectar com Banco de Dados e criar páginas web simples - Gostei bastante do Django e praticava bastante, dessa forma aprendi bastante sobre análise de erros, criação de novos Endpoints, inserção de Autenticação e Autorização, entre outros temas.',
    },
    {
      eyebrow: 'Faculdade',
      title: 'Analise e Desenvolvimento de Sistemas',
      description:
        'O curso de ADS me ajudou bastante a entender diversos conceitos que utilizo até hoje no Desenvolvimento de Software. Aprendi a configurar um Banco de Dados (utilizar um SGBD), reforcei meus conhecimentos sobre Algoritmos / Estrutura de Dados e tive a oportunidade de construir alguns projetos para praticar.',
    },
    {
      eyebrow: 'Atualidade',
      title: 'Analista Ongoing Júnior',
      description:
        'Minha primeira oportunidade como Desenvolvedor 🙏🙏 Onde de fato realizei meu primeiro deploy. Comecei como ajustes simples para melhorar a performance e deixar alguns processos de IAM mais robustos - Com o tempo, fiquei mais confortável em contribuir mais e sugerir novos fluxos.',
    },
    {
      eyebrow: 'Certificados',
      title: 'Cursos e Certificados SailPoint',
      description:
        'Obtive alguns certificados estudando na SailPoint University, graças à NetBR. Recentemente obtive o certificado de IdentityIQ Security Engineer e estou em busca do IdentityIQ Security Professional.',
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
    { src: '/images/home/configuracao-ci-cd.jpg', alt: 'Imagem de uma Pipeline de CI / CI no GitHub Actions com um arquivo de configuração para geração de um artetafo e envio para AWS' },
    { src: '/images/home/imagem-ci-cd-pipeline-gha.jpg', alt: 'Imagem de uma Pipeline de CI / CI no GitHub Actions com algumas ações e fluxos executados' },
    { src: '/images/home/print-desenvolvimento.jpg', alt: 'Imagem do editor VS Code com linhas de código em um arquivo HTML de um projeto Django' },
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
