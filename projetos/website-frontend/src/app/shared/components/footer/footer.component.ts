import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  siteName: string = 'Felipe Franke Bernardo';
  footerBrandName: string = 'FELIPE';
  availabilityText: string = 'Disponível para novos desafios e oportunidades.';
  location: string = 'Santo André, SP - Brasil';
  currentYear: number = this.getCurrentYear();
  linkedinUrl: string = 'https://www.linkedin.com/in/felipe-f-938334178/';
  githubUrl: string = 'https://github.com/FelipeFranke5';
  instagramUrl: string = 'https://www.instagram.com/frankefelipee/';

  getCurrentYear(): number {
    const today = new Date();
    return today.getFullYear();
  }
}
