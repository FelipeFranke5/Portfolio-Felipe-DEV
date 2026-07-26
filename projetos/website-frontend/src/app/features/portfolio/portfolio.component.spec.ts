import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { PortfolioComponent } from './portfolio.component';
import { Project, ProjectDetail, ProjectsService } from './projects.service';

describe('PortfolioComponent', () => {
  let component: PortfolioComponent;
  let fixture: ComponentFixture<PortfolioComponent>;
  let projectsServiceSpy: jasmine.SpyObj<ProjectsService>;

  const mockProjects: Project[] = [
    {
      id: '11111111-1111-1111-1111-111111111111',
      name: 'API Gateway de Pagamentos',
      description: 'Gateway de pagamentos em Java.',
      stack: ['Java', 'Spring Boot'],
    },
    {
      id: '22222222-2222-2222-2222-222222222222',
      name: 'Dashboard de Métricas',
      description: 'Dashboard em React.',
      stack: ['React', 'TypeScript'],
    },
  ];

  const mockProjectDetail: ProjectDetail = {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'API Gateway de Pagamentos',
    description: 'Gateway de pagamentos em Java, com Kafka para mensageria.',
    stack: ['Java', 'Spring Boot', 'Kafka'],
    githubURL: 'https://github.com/example/api-gateway',
    demoURL: null,
    featured: true,
    createdAt: '2026-03-14T09:32:00',
  };

  beforeEach(async () => {
    projectsServiceSpy = jasmine.createSpyObj<ProjectsService>('ProjectsService', [
      'getProjects',
      'getProjectById',
    ]);
    projectsServiceSpy.getProjects.and.returnValue(of(mockProjects));

    await TestBed.configureTestingModule({
      imports: [PortfolioComponent],
      providers: [provideRouter([]), { provide: ProjectsService, useValue: projectsServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PortfolioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the header and footer components', () => {
    const headerElement = fixture.nativeElement.querySelector('app-header');
    const footerElement = fixture.nativeElement.querySelector('app-footer');

    expect(headerElement).not.toBeNull();
    expect(footerElement).not.toBeNull();
  });

  it('should fetch the projects on init and render one card per project', () => {
    expect(projectsServiceSpy.getProjects).toHaveBeenCalled();

    const cards = fixture.nativeElement.querySelectorAll('.project-card');
    expect(cards.length).toBe(mockProjects.length);
  });

  it('should filter the projects by name when searching', () => {
    component.search.set('dashboard');
    fixture.detectChanges();

    expect(component.filteredProjects().length).toBe(1);
    expect(component.filteredProjects()[0].name).toBe('Dashboard de Métricas');
  });

  it('should show the empty state when the filter matches nothing', () => {
    component.search.set('kubernetes123');
    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector('.portfolio-state--empty');
    expect(emptyState).not.toBeNull();
    expect(emptyState.textContent).toContain('kubernetes123');
  });

  it('should show the error state and allow retrying when the request fails', () => {
    const errorResponse = new HttpErrorResponse({ status: 0 });
    projectsServiceSpy.getProjects.and.returnValue(throwError(() => errorResponse));

    component.fetchProjects();
    fixture.detectChanges();

    const errorState = fixture.nativeElement.querySelector('.portfolio-state--error');
    expect(errorState).not.toBeNull();
    expect(component.errorMessage()).toBe(
      'Houve uma falha ao consultar /api/projects. Tente novamente.'
    );

    projectsServiceSpy.getProjects.and.returnValue(of(mockProjects));
    fixture.nativeElement.querySelector('.portfolio-state--error button').click();
    fixture.detectChanges();

    expect(component.hasResults()).toBeTrue();
  });

  it('should open the modal and load the project detail when a card is activated', () => {
    projectsServiceSpy.getProjectById.and.returnValue(of(mockProjectDetail));

    component.openProject(mockProjects[0], document.createElement('div'));
    fixture.detectChanges();

    expect(projectsServiceSpy.getProjectById).toHaveBeenCalledWith(mockProjects[0].id);
    expect(component.isModalOpen()).toBeTrue();
    expect(component.activeProject()).toEqual(mockProjectDetail);
  });

  it('should show the GitHub link but not the demo link when only githubURL is present', () => {
    projectsServiceSpy.getProjectById.and.returnValue(of(mockProjectDetail));

    component.openProject(mockProjects[0], document.createElement('div'));
    fixture.detectChanges();

    const actions = fixture.nativeElement.querySelectorAll('.project-modal__action');
    expect(actions.length).toBe(1);
    expect(actions[0].textContent).toContain('Ver Repositório GitHub');
  });

  it('should hide both action links when neither githubURL nor demoURL are present', () => {
    projectsServiceSpy.getProjectById.and.returnValue(
      of({ ...mockProjectDetail, githubURL: null, demoURL: null })
    );

    component.openProject(mockProjects[0], document.createElement('div'));
    fixture.detectChanges();

    const actions = fixture.nativeElement.querySelectorAll('.project-modal__action');
    expect(actions.length).toBe(0);
  });

  it('should show an error message inside the modal when loading the detail fails', () => {
    const errorResponse = new HttpErrorResponse({
      status: 500,
      error: { reason: 'log-id-123' },
    });
    projectsServiceSpy.getProjectById.and.returnValue(throwError(() => errorResponse));

    component.openProject(mockProjects[0], document.createElement('div'));
    fixture.detectChanges();

    expect(component.modalErrorMessage()).toBe('log-id-123');
  });

  it('should close the modal when closeModal is called', () => {
    projectsServiceSpy.getProjectById.and.returnValue(of(mockProjectDetail));

    component.openProject(mockProjects[0], document.createElement('div'));
    fixture.detectChanges();

    component.closeModal();
    fixture.detectChanges();

    expect(component.isModalOpen()).toBeFalse();
  });
});
