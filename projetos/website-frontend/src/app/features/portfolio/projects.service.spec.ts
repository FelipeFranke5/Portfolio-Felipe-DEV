import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Project, ProjectDetail, ProjectsService } from './projects.service';
import { environment } from '../../../environments/environment';

describe('ProjectsService', () => {
  let service: ProjectsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ProjectsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET the project list from /projects', () => {
    const projects: Project[] = [
      {
        id: '11111111-1111-1111-1111-111111111111',
        name: 'API Gateway de Pagamentos',
        description: 'Gateway de pagamentos',
        stack: ['Java', 'Spring Boot'],
      },
    ];

    let actualResponse: Project[] | undefined;

    service.getProjects().subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(`${environment.apiUrl}/projects`);

    expect(request.request.method).toBe('GET');

    request.flush(projects);

    expect(actualResponse).toEqual(projects);
  });

  it('should GET a single project by id from /projects/{id}', () => {
    const projectDetail: ProjectDetail = {
      id: '11111111-1111-1111-1111-111111111111',
      name: 'API Gateway de Pagamentos',
      description: 'Gateway de pagamentos',
      stack: ['Java', 'Spring Boot'],
      githubURL: 'https://github.com/example/repo',
      demoURL: null,
      featured: true,
      createdAt: '2026-03-14T09:32:00',
    };

    let actualResponse: ProjectDetail | undefined;

    service.getProjectById(projectDetail.id).subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(
      `${environment.apiUrl}/projects/${projectDetail.id}`
    );

    expect(request.request.method).toBe('GET');

    request.flush(projectDetail);

    expect(actualResponse).toEqual(projectDetail);
  });
});
