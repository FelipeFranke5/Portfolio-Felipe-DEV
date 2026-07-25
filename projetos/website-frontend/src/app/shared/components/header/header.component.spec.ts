import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the brand link pointing to the home route', () => {
    const brand: HTMLAnchorElement = fixture.nativeElement.querySelector('.app-header__brand');

    expect(brand.textContent).toContain(component.siteName);
    expect(brand.getAttribute('href')).toBe('/');
  });

  it('should render a link for each real site route', () => {
    const links: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('.app-header__link');
    const hrefs = Array.from(links).map((link) => link.getAttribute('href'));

    expect(hrefs).toEqual(['/', '/portfolio', '/skills', '/contact']);
  });
});
