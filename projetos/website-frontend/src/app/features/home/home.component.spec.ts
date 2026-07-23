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

  it('should start on the first carousel slide', () => {
    expect(component.currentSlideIndex).toBe(0);

    const activeDot = fixture.nativeElement.querySelector('.home-carousel__dot--active');

    expect(activeDot.getAttribute('aria-label')).toBe('Ir para a introdução');
  });

  it('should change the current slide index when goToSlide is called', () => {
    const targetSlideIndex = 2;
    component.goToSlide(targetSlideIndex);
    fixture.detectChanges();

    expect(component.currentSlideIndex).toBe(targetSlideIndex);

    const activeDot = fixture.nativeElement.querySelector('.home-carousel__dot--active');

    expect(activeDot.getAttribute('aria-label')).toBe('Ir para Primeiros passos na programação');
  });

  it('should move the carousel track by clicking on a dot', () => {
    const dots = fixture.nativeElement.querySelectorAll('.home-carousel__dot');
    const secondDotIndex = 1;

    dots[secondDotIndex].click();
    fixture.detectChanges();

    expect(component.currentSlideIndex).toBe(secondDotIndex);

    const track = fixture.nativeElement.querySelector('.home-carousel__track');
    const expectedTranslatePercentage = secondDotIndex * 100;

    expect(track.style.transform).toBe(`translateX(-${expectedTranslatePercentage}%)`);
  });
});
