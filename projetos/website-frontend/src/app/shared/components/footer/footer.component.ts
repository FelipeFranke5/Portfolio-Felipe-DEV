import { Component } from '@angular/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  siteName: string = 'Felipe Franke Bernardo';
  currentYear: number = this.getCurrentYear();
  linkedinUrl: string = 'https://www.linkedin.com/in/felipe-f-938334178/';
  githubUrl: string = 'https://github.com/FelipeFranke5';
  instagramUrl: string = 'https://www.instagram.com/frankefelipee/';

  getCurrentYear(): number {
    const today = new Date();
    return today.getFullYear();
  }
}
