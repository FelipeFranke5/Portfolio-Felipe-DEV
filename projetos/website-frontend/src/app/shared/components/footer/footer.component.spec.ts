import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { FooterComponent } from './footer.component';

describe('FooterComponent', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the brand wordmark pointing to the home route', () => {
    const brand: HTMLAnchorElement = fixture.nativeElement.querySelector('.app-footer__brand');

    expect(brand.textContent).toContain(component.footerBrandName);
    expect(brand.getAttribute('href')).toBe('/');
  });

  it('should render the current year and site name in the copyright text', () => {
    const copyright = fixture.nativeElement.querySelector('.app-footer__copyright');

    expect(copyright.textContent).toContain(String(component.currentYear));
    expect(copyright.textContent).toContain(component.siteName);
  });

  it('should render links for each real site route split across two nav columns', () => {
    const navs: NodeListOf<HTMLElement> = fixture.nativeElement.querySelectorAll('.app-footer__nav');
    const hrefs = Array.from(navs)
      .flatMap((nav) => Array.from(nav.querySelectorAll('a')))
      .map((link) => link.getAttribute('href'));

    expect(hrefs).toEqual(['/', '/portfolio', '/skills', '/contact']);
  });

  it('should render the three social links', () => {
    const socialLinks: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('.app-footer__social-link');

    expect(socialLinks.length).toBe(3);
    expect(socialLinks[0].getAttribute('href')).toBe(component.linkedinUrl);
    expect(socialLinks[1].getAttribute('href')).toBe(component.githubUrl);
    expect(socialLinks[2].getAttribute('href')).toBe(component.instagramUrl);
  });
});
