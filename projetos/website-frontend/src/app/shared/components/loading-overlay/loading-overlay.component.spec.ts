import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoadingOverlayComponent } from './loading-overlay.component';
import { LoadingService } from '../../../core/services/loading.service';

describe('LoadingOverlayComponent', () => {
  let component: LoadingOverlayComponent;
  let fixture: ComponentFixture<LoadingOverlayComponent>;
  let loadingService: LoadingService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingOverlayComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingOverlayComponent);
    component = fixture.componentInstance;
    loadingService = TestBed.inject(LoadingService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render the overlay when isLoading is false', () => {
    fixture.detectChanges();

    const overlayElement = fixture.nativeElement.querySelector('.loading-overlay');

    expect(overlayElement).toBeNull();
  });

  it('should render the overlay with the correct accessibility attributes when isLoading is true', () => {
    loadingService.show();
    fixture.detectChanges();

    const overlayElement = fixture.nativeElement.querySelector('.loading-overlay');

    expect(overlayElement).not.toBeNull();
    expect(overlayElement.getAttribute('role')).toBe('status');
    expect(overlayElement.getAttribute('aria-live')).toBe('polite');
    expect(overlayElement.getAttribute('aria-label')).toBe('Carregando conteúdo');
  });

  it('should keep the overlay visible while there are still pending requests', () => {
    loadingService.show();
    loadingService.show();
    loadingService.hide();
    fixture.detectChanges();

    const overlayElement = fixture.nativeElement.querySelector('.loading-overlay');

    expect(overlayElement).not.toBeNull();
  });

  it('should hide the overlay again once the last pending request finishes', () => {
    loadingService.show();
    loadingService.show();
    loadingService.hide();
    loadingService.hide();
    fixture.detectChanges();

    const overlayElement = fixture.nativeElement.querySelector('.loading-overlay');

    expect(overlayElement).toBeNull();
  });
});
