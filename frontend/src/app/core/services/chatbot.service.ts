import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ChatReplyDto, ChatStreamEvent } from '../models/chat.model';
import { AuthService } from './auth.service';

@Service()
export class ChatbotService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  ask(message: string) {
    return this.http.post<ChatReplyDto>(`${environment.apiUrl}/chatbot/chat`, { message });
  }

  /**
   * Diffuse la réponse du backend (NDJSON) et remonte chaque événement au fur et à mesure.
   * Utilise fetch pour lire le corps en flux (l'intercepteur HttpClient ne le permet pas).
   */
  async stream(
    message: string,
    onEvent: (event: ChatStreamEvent) => void,
    signal?: AbortSignal,
  ): Promise<void> {
    const token = this.auth.accessToken;
    const response = await fetch(`${environment.apiUrl}/chatbot/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ message }),
      signal,
    });
    if (!response.ok || !response.body) {
      throw new Error(`API ${response.status}`);
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      let newline = buffer.indexOf('\n');
      while (newline >= 0) {
        this.emitLine(buffer.slice(0, newline), onEvent);
        buffer = buffer.slice(newline + 1);
        newline = buffer.indexOf('\n');
      }
    }
    buffer += decoder.decode();
    this.emitLine(buffer, onEvent);
  }

  private emitLine(line: string, onEvent: (event: ChatStreamEvent) => void): void {
    const trimmed = line.trim();
    if (!trimmed) {
      return;
    }
    try {
      onEvent(JSON.parse(trimmed) as ChatStreamEvent);
    } catch {
      // fragment incomplet : ignoré
    }
  }
}
