import { Injectable, inject } from '@angular/core';

import { ApiService } from '../../core/services/api.service';

export interface ContactRequest {
  name: string;
  email: string;
  message: string;
}

export interface ContactResponse {
  messageId: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class ContactService {
  private readonly apiService = inject(ApiService);

  sendMessage(contactRequest: ContactRequest) {
    return this.apiService.post<ContactResponse>('/contact', contactRequest);
  }
}
