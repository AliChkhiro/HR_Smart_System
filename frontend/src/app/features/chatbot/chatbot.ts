import { Component, ElementRef, HostListener, NgZone, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChatbotService } from '../../core/services/chatbot.service';
import { ChatStreamEvent } from '../../core/models/chat.model';

export interface ChatMessage {
  id: number;
  from: 'user' | 'assistant';
  text: string;
}

/**
 * Widget flottant de l'assistant RH.
 * Appelle POST /api/chatbot/chat/stream ; le backend relaie la réponse de l'ia-service
 * (Ollama) en flux NDJSON, affichée progressivement.
 */
@Component({
  selector: 'app-chatbot',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.scss',
})
export class Chatbot {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly chatbotService = inject(ChatbotService);
  private readonly zone = inject(NgZone);

  private readonly messagesArea = viewChild<ElementRef<HTMLElement>>('messagesArea');
  private nextId = 1;

  protected readonly open = signal(false);
  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly thinking = signal(false);
  protected readonly aiAvailable = signal(true);

  protected readonly messages = signal<ChatMessage[]>([
    {
      id: 0,
      from: 'assistant',
      text: 'Bonjour, je suis votre assistant RH. Posez-moi une question sur vos congés, vos tâches ou les projets.',
    },
  ]);

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.close();
  }

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.open()) {
      return;
    }
    if (this.host.nativeElement.contains(event.target as Node)) {
      return;
    }
    this.close();
  }

  protected toggle(): void {
    this.open.update(value => !value);
  }

  protected close(): void {
    this.open.set(false);
  }

  protected onDraftInput(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected send(): void {
    const text = this.draft().trim();
    if (!text || this.sending()) {
      return;
    }
    this.append('user', text);
    this.draft.set('');
    this.sending.set(true);
    this.thinking.set(true);
    const assistantId = this.appendPlaceholder();
    this.chatbotService
      .stream(text, event => this.zone.run(() => this.onStreamEvent(assistantId, event)))
      .then(() => this.zone.run(() => this.finish()))
      .catch(() => this.zone.run(() => {
        this.updateMessage(assistantId, 'Le service est injoignable pour le moment. Réessayez dans un instant.');
        this.finish();
      }));
  }

  private onStreamEvent(id: number, event: ChatStreamEvent): void {
    if (event.type === 'token') {
      this.thinking.set(false);
      this.appendText(id, event.text);
    } else if (event.type === 'fallback') {
      this.aiAvailable.set(false);
      this.thinking.set(false);
      this.appendText(id, event.text);
    } else if (event.type === 'done') {
      if (event.interrupted) {
        this.aiAvailable.set(false);
      }
      this.thinking.set(false);
    }
  }

  private finish(): void {
    this.sending.set(false);
    this.thinking.set(false);
    this.scrollToBottom();
  }

  private append(from: 'user' | 'assistant', text: string): void {
    this.messages.update(list => [...list, { id: this.nextId++, from, text }]);
    this.scrollToBottom();
  }

  private appendPlaceholder(): number {
    const id = this.nextId++;
    this.messages.update(list => [...list, { id, from: 'assistant', text: '' }]);
    this.scrollToBottom();
    return id;
  }

  private appendText(id: number, text: string): void {
    this.messages.update(list => list.map(message =>
      message.id === id ? { ...message, text: message.text + text } : message));
    this.scrollToBottom();
  }

  private updateMessage(id: number, text: string): void {
    this.messages.update(list => list.map(message =>
      message.id === id ? { ...message, text } : message));
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    const area = this.messagesArea()?.nativeElement;
    if (area) {
      area.scrollTop = area.scrollHeight;
    }
  }
}
