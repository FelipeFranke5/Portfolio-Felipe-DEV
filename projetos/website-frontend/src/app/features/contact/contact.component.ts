import {
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { ContactService } from './contact.service';

type ContactFieldName = 'name' | 'email' | 'message';
type ContactStep = 1 | 2 | 3;

interface ContactReason {
  text: string;
}

interface CompletedField {
  label: string;
  value: string;
}

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss',
})
export class ContactComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly contactService = inject(ContactService);

  private readonly defaultErrorMessage =
    'Não foi possível enviar sua mensagem. Tente novamente mais tarde.';

  readonly totalSteps = 3;
  readonly nameMaxLength = 100;
  readonly emailMaxLength = 254;
  readonly messageMaxLength = 3000;
  readonly directEmail = 'frankefelipee@gmail.com';

  readonly contactReasons: ContactReason[] = [
    { text: 'Ajuda na parte de Back-end de algum projeto' },
    { text: 'Ajuda na criação ou manutenção do seu site' },
    { text: 'Parceria (incluir link do seu site ou app neste site)' },
    { text: 'Entrar em contato sendo um(a) recrutador(a)' },
  ];

  readonly contactForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(this.nameMaxLength)]],
    email: [
      '',
      [Validators.required, Validators.email, Validators.maxLength(this.emailMaxLength)],
    ],
    message: ['', [Validators.required, Validators.maxLength(this.messageMaxLength)]],
  });

  readonly isSubmitting = signal(false);
  readonly submitSucceeded = signal(false);
  readonly submitErrorMessage = signal<string | null>(null);

  readonly currentStep = signal<ContactStep>(1);
  readonly focusedField = signal<ContactFieldName | null>(null);

  private readonly nameInput = viewChild<ElementRef<HTMLInputElement>>('nameInput');
  private readonly emailInput = viewChild<ElementRef<HTMLInputElement>>('emailInput');
  private readonly messageInput = viewChild<ElementRef<HTMLTextAreaElement>>('messageInput');
  private readonly stepInputs = [this.nameInput, this.emailInput, this.messageInput] as const;

  constructor() {
    effect(() => {
      const step = this.currentStep();
      this.stepInputs[step - 1]()?.nativeElement.focus({ preventScroll: true });
    });
  }

  readonly completedFields = computed<CompletedField[]>(() => {
    const completedFields: CompletedField[] = [];
    const step = this.currentStep();

    if (step > 1) {
      completedFields.push({ label: 'Nome', value: this.contactForm.controls.name.value });
    }

    if (step > 2) {
      completedFields.push({ label: 'E-mail', value: this.contactForm.controls.email.value });
    }

    return completedFields;
  });

  isFieldInvalid(fieldName: ContactFieldName): boolean {
    const control = this.contactForm.controls[fieldName];
    return control.invalid && (control.touched || control.dirty);
  }

  isStepButtonDisabled(fieldName: ContactFieldName): boolean {
    return this.contactForm.controls[fieldName].invalid;
  }

  onFieldFocus(fieldName: ContactFieldName): void {
    this.focusedField.set(fieldName);
  }

  onFieldBlur(fieldName: ContactFieldName): void {
    this.contactForm.controls[fieldName].markAsTouched();

    if (this.focusedField() === fieldName) {
      this.focusedField.set(null);
    }
  }

  continueFromName(): void {
    this.continueFromStep('name', 2);
  }

  continueFromEmail(): void {
    this.continueFromStep('email', 3);
  }

  private continueFromStep(fieldName: ContactFieldName, nextStep: ContactStep): void {
    const control = this.contactForm.controls[fieldName];

    if (control.invalid) {
      control.markAsTouched();
      return;
    }

    this.currentStep.set(nextStep);
  }

  submit(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.submitErrorMessage.set(null);
    this.submitSucceeded.set(false);

    this.contactService.sendMessage(this.contactForm.getRawValue()).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.submitSucceeded.set(true);
        this.contactForm.reset();
        this.currentStep.set(1);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.submitErrorMessage.set(error.error?.message ?? this.defaultErrorMessage);
      },
    });
  }
}
