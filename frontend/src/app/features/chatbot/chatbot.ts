import { Component, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChatbotService } from '../../core/services/chatbot.service';

export interface ChatMessage {
  id: number;
  from: 'user' | 'assistant';
  text: string;
}

/**
 * Widget flottant de l'assistant RH.
 * STEP 2 : appelle POST /api/chatbot/chat, le backend repond sans LLM.
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

  private readonly messagesArea = viewChild<ElementRef<HTMLElement>>('messagesArea');
  private nextId = 1;

  protected readonly open = signal(false);
  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly aiAvailable = signal(false);

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
    this.chatbotService.ask(text).subscribe({
      next: response => {
        this.aiAvailable.set(response.aiAvailable);
        this.append('assistant', response.reply);
        this.sending.set(false);
      },
      error: () => {
        this.append('assistant', "Le service est injoignable pour le moment. Reessayez dans un instant.");
        this.sending.set(false);
      }
    });
  }

  private append(from: 'user' | 'assistant', text: string): void {
    this.messages.update(list => [...list, { id: this.nextId++, from, text }]);
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    const area = this.messagesArea()?.nativeElement;
    if (area) {
      area.scrollTop = area.scrollHeight;
    }
  }
}
