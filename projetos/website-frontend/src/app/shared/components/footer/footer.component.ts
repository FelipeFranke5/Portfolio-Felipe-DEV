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
  siteName = 'Felipe Franke Bernardo';
  footerBrandName = 'FELIPE';
  availabilityText = 'Disponível para novos desafios e oportunidades.';
  location = 'Santo André, SP - Brasil';
  currentYear: number = this.getCurrentYear();
  linkedinUrl = 'https://www.linkedin.com/in/felipe-f-938334178/';
  githubUrl = 'https://github.com/FelipeFranke5';
  instagramUrl = 'https://www.instagram.com/frankefelipee/';

  getCurrentYear(): number {
    const today = new Date();
    return today.getFullYear();
  }
}
