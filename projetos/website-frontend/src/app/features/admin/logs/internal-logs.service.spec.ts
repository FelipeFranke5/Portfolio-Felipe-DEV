import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { InternalLog, InternalLogsService } from './internal-logs.service';
import { environment } from '../../../../environments/environment';
import { SKIP_GLOBAL_LOADING } from '../../../core/interceptors/loading.interceptor';

describe('InternalLogsService', () => {
  let service: InternalLogsService;
  let httpTestingController: HttpTestingController;

  const internalLog: InternalLog = {
    id: '33333333-3333-3333-3333-333333333333',
    simpleClassName: 'HttpRequestMethodNotSupportedException',
    errorMessage: "Request method 'PATCH' is not supported",
    stackTrace: 'org.springframework.web.HttpRequestMethodNotSupportedException: ...',
    createdAt: '2026-07-18T18:48:09.625254',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(InternalLogsService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET the recent log list from /internal_log', () => {
    let actualResponse: InternalLog[] | undefined;

    service.getLogs().subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(`${environment.apiUrl}/internal_log`);

    expect(request.request.method).toBe('GET');

    request.flush([internalLog]);

    expect(actualResponse).toEqual([internalLog]);
  });

  it('should GET a single log by id from /internal_log/{id}', () => {
    let actualResponse: InternalLog | undefined;

    service.getLogById(internalLog.id).subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(
      `${environment.apiUrl}/internal_log/${internalLog.id}`
    );

    expect(request.request.method).toBe('GET');

    request.flush(internalLog);

    expect(actualResponse).toEqual(internalLog);
  });

  it('should skip the global loading overlay so the screen can show its own state', () => {
    service.getLogs().subscribe();

    const request = httpTestingController.expectOne(`${environment.apiUrl}/internal_log`);

    expect(request.request.context.get(SKIP_GLOBAL_LOADING)).toBeTrue();

    request.flush([]);
  });
});
