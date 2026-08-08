import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CertificatesComponent } from './certificates.component';

describe('CertificatesComponent', () => {
  let component: CertificatesComponent;
  let fixture: ComponentFixture<CertificatesComponent>;

  const firstCard = (): HTMLElement =>
    fixture.nativeElement.querySelector('.certificate-card') as HTMLElement;

  const openFirstCard = (): void => {
    firstCard().click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CertificatesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CertificatesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render one card per certificate', () => {
    const cards = fixture.nativeElement.querySelectorAll('.certificate-card');
    expect(cards.length).toBe(component.certificates.length);
  });

  it('should use a unique track key for every certificate', () => {
    const keys = component.certificates.map((certificate) => certificate.imageSrc);
    expect(new Set(keys).size).toBe(component.certificates.length);
  });

  it('should render the issuer, name, description and image in each card', () => {
    const certificate = component.certificates[0];
    const card = firstCard();

    expect(card.querySelector('.certificate-card__issuer')?.textContent).toContain(
      certificate.issuer
    );
    expect(card.querySelector('.certificate-card__title')?.textContent).toContain(certificate.name);
    expect(card.querySelector('.certificate-card__description')?.textContent).toContain(
      certificate.description
    );

    const image = card.querySelector('.certificate-card__image');
    expect(image?.getAttribute('src')).toBe(certificate.imageSrc);
    expect(image?.getAttribute('alt')).toContain(certificate.name);
  });

  it('should not render the issue date or the external link in the listing', () => {
    const list = fixture.nativeElement.querySelector('.certificates__list') as HTMLElement;

    expect(list.querySelectorAll('a').length).toBe(0);
    for (const certificate of component.certificates) {
      expect(list.textContent).not.toContain(certificate.issuedAt);
    }
  });

  it('should expose each card as an accessible button', () => {
    const card = firstCard();

    expect(card.getAttribute('role')).toBe('button');
    expect(card.getAttribute('tabindex')).toBe('0');
    expect(card.getAttribute('aria-haspopup')).toBe('dialog');
    expect(card.getAttribute('aria-label')).toContain(component.certificates[0].name);
  });

  it('should open the modal when a card is clicked', () => {
    openFirstCard();

    expect(component.isModalOpen()).toBe(true);
    expect(component.activeCertificate()).toBe(component.certificates[0]);

    const dialog = fixture.nativeElement.querySelector('.certificate-modal');
    expect(dialog).not.toBeNull();
    expect(dialog.getAttribute('role')).toBe('dialog');
    expect(dialog.getAttribute('aria-modal')).toBe('true');
  });

  it('should open the modal when Enter is pressed on a card', () => {
    firstCard().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();

    expect(component.isModalOpen()).toBe(true);
  });

  it('should open the modal when Space is pressed on a card', () => {
    firstCard().dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));
    fixture.detectChanges();

    expect(component.isModalOpen()).toBe(true);
  });

  it('should show the issue date and the complete description in the modal', () => {
    const certificate = component.certificates[0];
    openFirstCard();

    expect(fixture.nativeElement.querySelector('.certificate-modal__date')?.textContent).toContain(
      certificate.issuedAt
    );
    expect(
      fixture.nativeElement.querySelector('.certificate-modal__description')?.textContent?.trim()
    ).toBe(certificate.description);
  });

  it('should show the external certificate link in the modal', () => {
    const certificate = component.certificates[0];
    openFirstCard();

    const action = fixture.nativeElement.querySelector('.certificate-modal__action');
    expect(action.getAttribute('href')).toBe(certificate.certificateUrl);
    expect(action.getAttribute('target')).toBe('_blank');
    expect(action.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('should close the modal when Escape is pressed', () => {
    openFirstCard();

    component.onEscapePressed();
    fixture.detectChanges();

    expect(component.isModalOpen()).toBe(false);
    expect(fixture.nativeElement.querySelector('.certificate-modal')).toBeNull();
  });

  it('should return the focus to the card that opened the modal', () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);

    component.openCertificate(component.certificates[0], trigger);
    fixture.detectChanges();

    component.closeModal();
    fixture.detectChanges();

    expect(document.activeElement).toBe(trigger);
    document.body.removeChild(trigger);
  });
});
