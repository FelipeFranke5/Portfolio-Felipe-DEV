import { Component } from '@angular/core';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [HeaderComponent, FooterComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  fullName: string = "Felipe Franke Bernardo";
  currentJobTitle: string = "Analista de Ongoing Júnior";
  currentJobCompany: string = "NetBR by SEK";
  currentJobWebsiteURL: string = "https://www.netbr.com.br/";
  currentAge: number = this.calculateAge();
  currentSlideIndex: number = 0;

  calculateAge(): number {
    const birthYear = 2000;
    const today = new Date();
    let age = today.getFullYear() - birthYear;
    return age;
  }

  goToSlide(slideIndex: number): void {
    this.currentSlideIndex = slideIndex;
  }
}
