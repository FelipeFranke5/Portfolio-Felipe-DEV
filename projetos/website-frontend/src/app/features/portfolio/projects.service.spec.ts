import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import {
  Project,
  ProjectDetail,
  ProjectRequest,
  ProjectsService,
  emptyToNull,
  parseStack,
} from './projects.service';
import { environment } from '../../../environments/environment';
import { SKIP_GLOBAL_LOADING } from '../../core/interceptors/loading.interceptor';

describe('ProjectsService', () => {
  let service: ProjectsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()],
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

  it('should POST the payload to /projects when creating a project', () => {
    const projectRequest: ProjectRequest = {
      name: 'API Gateway de Pagamentos',
      description: 'Gateway de pagamentos',
      stack: ['Java', 'Spring Boot'],
      githubURL: 'https://github.com/example/repo',
      demoURL: null,
      featured: true,
    };

    service.createProject(projectRequest).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/projects`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(projectRequest);

    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should PUT the payload to /projects/{id} when updating a project', () => {
    const projectId = '11111111-1111-1111-1111-111111111111';
    const projectRequest: ProjectRequest = {
      name: 'API Gateway de Pagamentos',
      description: 'Gateway de pagamentos revisado',
      stack: ['Java'],
      githubURL: null,
      demoURL: null,
      featured: false,
    };

    service.updateProject(projectId, projectRequest).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/projects/${projectId}`);

    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(projectRequest);

    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should DELETE /projects/{id} when removing a project', () => {
    const projectId = '11111111-1111-1111-1111-111111111111';

    service.deleteProject(projectId).subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/projects/${projectId}`);

    expect(request.request.method).toBe('DELETE');

    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('should skip the global loading overlay on the admin requests but not on the public ones', () => {
    service.getProjects().subscribe();
    service.getProjectsForAdmin().subscribe();

    const requests = httpTestingController.match(`${environment.apiUrl}/projects`);

    expect(requests.length).toBe(2);
    expect(requests[0].request.context.get(SKIP_GLOBAL_LOADING)).toBe(false);
    expect(requests[1].request.context.get(SKIP_GLOBAL_LOADING)).toBe(true);

    requests.forEach((request) => request.flush([]));
  });
});

describe('parseStack', () => {
  it('should split the raw value by comma and drop blank entries', () => {
    expect(parseStack(' Java , Spring Boot ,, PostgreSQL , ')).toEqual([
      'Java',
      'Spring Boot',
      'PostgreSQL',
    ]);
  });

  it('should return an empty list when the raw value has no usable entry', () => {
    expect(parseStack('  ,  , ')).toEqual([]);
  });
});

describe('emptyToNull', () => {
  // O back-end valida githubURL com @Pattern, que reprova string vazia — um
  // campo opcional em branco tem que chegar como null.
  it('should convert a blank value to null', () => {
    expect(emptyToNull('')).toBeNull();
    expect(emptyToNull('   ')).toBeNull();
    expect(emptyToNull(null)).toBeNull();
    expect(emptyToNull(undefined)).toBeNull();
  });

  it('should trim and keep a filled value', () => {
    expect(emptyToNull('  https://github.com/example/repo  ')).toBe(
      'https://github.com/example/repo'
    );
  });
});
