import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ContactService, ContactRequest, ContactResponse } from './contact.service';
import { environment } from '../../../environments/environment';

describe('ContactService', () => {
  let service: ContactService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()],
    });

    service = TestBed.inject(ContactService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST the contact request to /contact and return the response', () => {
    const contactRequest: ContactRequest = {
      name: 'Felipe',
      email: 'felipe@example.com',
      message: 'Olá, gostaria de entrar em contato.',
    };

    const contactResponse: ContactResponse = {
      messageId: '11111111-1111-1111-1111-111111111111',
      createdAt: '2026-07-23T10:00:00',
    };

    let actualResponse: ContactResponse | undefined;

    service.sendMessage(contactRequest).subscribe((response) => {
      actualResponse = response;
    });

    const request = httpTestingController.expectOne(`${environment.apiUrl}/contact`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(contactRequest);

    request.flush(contactResponse);

    expect(actualResponse).toEqual(contactResponse);
  });
});
