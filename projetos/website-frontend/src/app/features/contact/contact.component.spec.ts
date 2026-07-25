import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { ContactComponent } from './contact.component';
import { ContactService, ContactResponse } from './contact.service';

describe('ContactComponent', () => {
  let component: ContactComponent;
  let fixture: ComponentFixture<ContactComponent>;
  let contactServiceSpy: jasmine.SpyObj<ContactService>;

  beforeEach(async () => {
    contactServiceSpy = jasmine.createSpyObj<ContactService>('ContactService', ['sendMessage']);

    await TestBed.configureTestingModule({
      imports: [ContactComponent],
      providers: [provideRouter([]), { provide: ContactService, useValue: contactServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactComponent);
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

  it('should start on step 1 with the "Continuar" button disabled while the name is empty', () => {
    expect(component.currentStep()).toBe(1);
    expect(component.isStepButtonDisabled('name')).toBeTrue();
  });

  it('should not advance to step 2 and should mark the name field as touched when it is empty', () => {
    component.continueFromName();

    expect(component.currentStep()).toBe(1);
    expect(component.isFieldInvalid('name')).toBeTrue();
  });

  it('should advance to step 2 when the name is valid', () => {
    component.contactForm.controls.name.setValue('Felipe');

    component.continueFromName();

    expect(component.currentStep()).toBe(2);
  });

  it('should not advance to step 3 and should mark the email field as touched when it is invalid', () => {
    component.contactForm.controls.name.setValue('Felipe');
    component.continueFromName();

    component.contactForm.controls.email.setValue('not-an-email');
    component.continueFromEmail();

    expect(component.currentStep()).toBe(2);
    expect(component.isFieldInvalid('email')).toBeTrue();
  });

  it('should advance to step 3 when the email is valid', () => {
    component.contactForm.controls.name.setValue('Felipe');
    component.continueFromName();

    component.contactForm.controls.email.setValue('felipe@example.com');
    component.continueFromEmail();

    expect(component.currentStep()).toBe(3);
  });

  it('should list previously completed fields once the user advances past them', () => {
    component.contactForm.controls.name.setValue('Felipe');
    component.continueFromName();

    expect(component.completedFields).toEqual([{ label: 'Nome', value: 'Felipe' }]);

    component.contactForm.controls.email.setValue('felipe@example.com');
    component.continueFromEmail();

    expect(component.completedFields).toEqual([
      { label: 'Nome', value: 'Felipe' },
      { label: 'E-mail', value: 'felipe@example.com' },
    ]);
  });

  it('should not submit and should mark all fields as touched when name, email and message are empty', () => {
    component.submit();

    expect(contactServiceSpy.sendMessage).not.toHaveBeenCalled();
    expect(component.contactForm.invalid).toBeTrue();
    expect(component.isFieldInvalid('name')).toBeTrue();
    expect(component.isFieldInvalid('email')).toBeTrue();
    expect(component.isFieldInvalid('message')).toBeTrue();
  });

  it('should mark the email field invalid when the value is not a valid email', () => {
    component.contactForm.controls.email.setValue('not-an-email');
    component.contactForm.controls.email.markAsTouched();

    expect(component.isFieldInvalid('email')).toBeTrue();
    expect(component.contactForm.controls.email.hasError('email')).toBeTrue();
  });

  it('should send the message, show a success message and reset back to step 1 when the form is valid', () => {
    const contactResponse: ContactResponse = {
      messageId: '11111111-1111-1111-1111-111111111111',
      createdAt: '2026-07-23T10:00:00',
    };
    contactServiceSpy.sendMessage.and.returnValue(of(contactResponse));

    const filledContact = {
      name: 'Felipe',
      email: 'felipe@example.com',
      message: 'Olá, gostaria de entrar em contato.',
    };
    component.contactForm.setValue(filledContact);
    component.currentStep.set(3);

    component.submit();

    expect(contactServiceSpy.sendMessage).toHaveBeenCalledWith(filledContact);
    expect(component.submitSucceeded()).toBeTrue();
    expect(component.contactForm.controls.name.value).toBe('');
    expect(component.currentStep()).toBe(1);
  });

  it('should show the error message returned by the backend when the request fails', () => {
    const errorResponse = new HttpErrorResponse({
      error: { message: 'Falha ao processar a requisição.' },
      status: 500,
    });
    contactServiceSpy.sendMessage.and.returnValue(throwError(() => errorResponse));

    component.contactForm.setValue({
      name: 'Felipe',
      email: 'felipe@example.com',
      message: 'Olá, gostaria de entrar em contato.',
    });

    component.submit();

    expect(component.submitErrorMessage()).toBe('Falha ao processar a requisição.');
    expect(component.submitSucceeded()).toBeFalse();
  });

  it('should show a generic error message when the backend response has no message', () => {
    const errorResponse = new HttpErrorResponse({ status: 0 });
    contactServiceSpy.sendMessage.and.returnValue(throwError(() => errorResponse));

    component.contactForm.setValue({
      name: 'Felipe',
      email: 'felipe@example.com',
      message: 'Olá, gostaria de entrar em contato.',
    });

    component.submit();

    expect(component.submitErrorMessage()).toBe(
      'Não foi possível enviar sua mensagem. Tente novamente mais tarde.'
    );
  });
});
