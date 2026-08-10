import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
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

  it('should calculate the current age based on the fixed birth year', () => {
    const birthYear = 2000;
    const expectedAge = new Date().getFullYear() - birthYear;

    expect(component.calculateAge()).toBe(expectedAge);
    expect(component.currentAge).toBe(expectedAge);
  });

  it('should render the hero heading with the accent word', () => {
    const heading = fixture.nativeElement.querySelector('.hero__heading');
    const accent = fixture.nativeElement.querySelector('.hero__heading-accent');

    expect(heading.textContent).toContain('Seja bem-vindo(a) ao meu pequeno');
    expect(accent.textContent).toContain('Portfólio!');
  });

  it('should mark the hero image as a high-priority LCP candidate', () => {
    const heroImage: HTMLImageElement = fixture.nativeElement.querySelector('.hero__media-image');

    expect(heroImage.getAttribute('fetchpriority')).toBe('high');
    expect(heroImage.getAttribute('width')).toBe('1000');
    expect(heroImage.getAttribute('height')).toBe('1000');
    expect(heroImage.getAttribute('src')).toContain('/images/home/imagem-felipe-2.jpg');
  });

  it('should render one trail step per entry in trailSteps', () => {
    const steps = fixture.nativeElement.querySelectorAll('.trail__step');

    expect(steps.length).toBe(component.trailSteps.length);
  });

  it('should render one trail separator between each trail step', () => {
    const separators = fixture.nativeElement.querySelectorAll('.trail__separator');

    expect(separators.length).toBe(component.trailSteps.length - 1);
  });

  it('should render one timeline entry per item in aboutTimeline', () => {
    const entries = fixture.nativeElement.querySelectorAll('.about__entry');

    expect(entries.length).toBe(component.aboutTimeline.length);
  });

  it('should render a "Contato" link pointing to /contact', () => {
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('.cta__button');

    expect(link).not.toBeNull();
    expect(link.textContent).toContain('Contato');
    expect(link.getAttribute('href')).toBe('/contact');
  });

  it('should render one journey step per item in journeySteps', () => {
    const steps = fixture.nativeElement.querySelectorAll('.journey__step');

    expect(steps.length).toBe(component.journeySteps.length);
  });

  it('should render one skill card per item in skillCards', () => {
    const cards = fixture.nativeElement.querySelectorAll('.skills__card');

    expect(cards.length).toBe(component.skillCards.length);
  });

  it('should render one roadmap card per item in roadmapCards', () => {
    const cards = fixture.nativeElement.querySelectorAll('.roadmap__card');

    expect(cards.length).toBe(component.roadmapCards.length);
  });

  it('should render one gallery image per item in galleryImages', () => {
    const images = fixture.nativeElement.querySelectorAll('.gallery__item img');

    expect(images.length).toBe(component.galleryImages.length);
  });

  it('should render a CTA button pointing to /contact', () => {
    const button: HTMLAnchorElement = fixture.nativeElement.querySelector('.cta__button');

    expect(button.textContent).toContain('Contato');
    expect(button.getAttribute('href')).toBe('/contact');
  });

  it('should keep the CTA section anchored at #contato', () => {
    const section: HTMLElement = fixture.nativeElement.querySelector('section.cta');

    expect(section.getAttribute('id')).toBe('contato');
  });

  it('should resolve the CTA colors from its local custom properties', () => {
    const section: HTMLElement = fixture.nativeElement.querySelector('.cta');
    const button: HTMLElement = fixture.nativeElement.querySelector('.cta__button');

    expect(getComputedStyle(section).backgroundColor).toBe('rgb(59, 130, 246)');
    expect(getComputedStyle(button).color).toBe('rgb(255, 255, 255)');
  });

  it('should keep the full accent word in the DOM for the typewriter animation', () => {
    const accent: HTMLElement = fixture.nativeElement.querySelector('.hero__heading-accent');

    expect(accent.textContent?.trim()).toBe('Portfólio!');
  });

  it('should render one coffee accent per item in coffeeAccents', () => {
    const cups = fixture.nativeElement.querySelectorAll('.coffee-layer__cup');

    expect(cups.length).toBe(component.coffeeAccents.length);
  });

  it('should apply the configured placement to each coffee accent', () => {
    const cups: NodeListOf<HTMLImageElement> =
      fixture.nativeElement.querySelectorAll('.coffee-layer__cup');

    cups.forEach((cup, index) => {
      const accent = component.coffeeAccents[index];

      expect(cup.getAttribute('src')).toBe(accent.src);
      expect(cup.style.top).toBe(accent.top);
      expect(cup.style.left).toBe(accent.left);
      expect(cup.style.width).toBe(accent.size);
      expect(cup.style.rotate).toBe(accent.rotate);
      expect(cup.style.opacity).toBe(accent.opacity);
      expect(cup.classList.contains('coffee-layer__cup--light')).toBe(!!accent.light);
    });
  });

  it('should hide every decorative coffee image from assistive technology', () => {
    const layer: HTMLElement = fixture.nativeElement.querySelector('.coffee-layer');
    const decorativeCups: NodeListOf<HTMLImageElement> = fixture.nativeElement.querySelectorAll(
      '.coffee-layer__cup, .roadmap__divider-cup',
    );

    expect(layer.getAttribute('aria-hidden')).toBe('true');
    expect(decorativeCups.length).toBe(component.coffeeAccents.length + 1);

    decorativeCups.forEach((cup) => {
      expect(cup.getAttribute('alt')).toBe('');
      expect(cup.getAttribute('src')).toContain('/images/home/coffee-');
    });
  });
});
