import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ChatReplyDto } from '../models/chat.model';

@Service()
export class ChatbotService {
  private readonly http = inject(HttpClient);

  ask(message: string) {
    return this.http.post<ChatReplyDto>(`${environment.apiUrl}/chatbot/chat`, { message });
  }
}
