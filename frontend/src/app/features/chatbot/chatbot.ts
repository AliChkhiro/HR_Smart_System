import { Component, ElementRef, HostListener, NgZone, computed, effect, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChatbotService } from '../../core/services/chatbot.service';
import { ChatHistoryMessage, ChatStreamEvent } from '../../core/models/chat.model';
import { MarkdownPipe } from './markdown.pipe';

export interface ChatMessage {
  id: number;
  from: 'user' | 'assistant';
  text: string;
  sources?: string[];
}

/**
 * Widget flottant de l'assistant RH.
 * Appelle POST /api/chatbot/chat/stream ; le backend relaie la réponse de l'ia-service
 * (Ollama) en flux NDJSON, affichée progressivement. L'historique est conservé dans
 * sessionStorage afin de survivre à un rechargement de page.
 */
@Component({
  selector: 'app-chatbot',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, MarkdownPipe],
  templateUrl: './chatbot.html',
  styleUrl: './chatbot.scss',
})
export class Chatbot {
  private static readonly STORAGE_KEY = 'apprh.chatbot.messages';
  private static readonly MAX_HISTORY_MESSAGES = 8;
  private static readonly SUGGESTIONS = [
    'Combien de congés approuvés ai-je ?',
    'Quelles sont mes tâches en cours ?',
    'Sur quels projets je travaille ?',
  ];

  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly chatbotService = inject(ChatbotService);
  private readonly zone = inject(NgZone);

  private readonly messagesArea = viewChild<ElementRef<HTMLElement>>('messagesArea');
  private readonly composer = viewChild<ElementRef<HTMLTextAreaElement>>('composer');
  private readonly launcher = viewChild<ElementRef<HTMLButtonElement>>('launcher');
  private nextId = 1;
  private controller: AbortController | null = null;
  private copiedTimer?: ReturnType<typeof setTimeout>;

  protected readonly suggestions = Chatbot.SUGGESTIONS;

  protected readonly open = signal(false);
  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly thinking = signal(false);
  protected readonly aiAvailable = signal(true);
  protected readonly copiedId = signal<number | null>(null);
  protected readonly messages = signal<ChatMessage[]>([]);
  protected readonly showSuggestions = computed(() => !this.sending() && this.messages().length <= 1);

  constructor() {
    this.messages.set(this.restore());
    this.loadServerHistory();

    effect(() => {
      if (this.open() && this.composer()) {
        this.composer()!.nativeElement.focus();
      }
    });
  }

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
    this.launcher()?.nativeElement.focus();
  }

  protected onDraftInput(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
    this.autoGrow();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected send(): void {
    this.submit(this.draft());
  }

  protected submit(text: string): void {
    const trimmed = text.trim();
    if (!trimmed || this.sending()) {
      return;
    }
    const history = this.buildHistory();
    this.draft.set('');
    this.resetComposer();
    this.append('user', trimmed);
    const placeholderId = this.appendPlaceholder();
    this.sending.set(true);
    this.thinking.set(true);
    const controller = new AbortController();
    this.controller = controller;
    this.chatbotService
      .stream(trimmed, history, event => this.zone.run(() => this.onStreamEvent(placeholderId, event)), controller.signal)
      .then(() => this.zone.run(() => this.finish()))
      .catch(error => this.zone.run(() => this.onStreamError(placeholderId, error)));
  }

  protected stop(): void {
    this.controller?.abort();
  }

  protected clear(): void {
    this.controller?.abort();
    this.controller = null;
    this.nextId = 1;
    this.messages.set([this.welcome()]);
    this.draft.set('');
    this.resetComposer();
    this.sending.set(false);
    this.thinking.set(false);
    this.aiAvailable.set(true);
    this.copiedId.set(null);
    this.save();
    this.chatbotService.clearHistory().catch(() => undefined);
  }

  protected copy(message: ChatMessage): void {
    navigator.clipboard?.writeText(message.text).then(() => {
      this.copiedId.set(message.id);
      if (this.copiedTimer) {
        clearTimeout(this.copiedTimer);
      }
      this.copiedTimer = setTimeout(() => this.copiedId.set(null), 1500);
    });
  }

  private buildHistory(): ChatHistoryMessage[] {
    return this.messages()
      .filter(message => message.text)
      .slice(-Chatbot.MAX_HISTORY_MESSAGES)
      .map(message => ({ role: message.from, content: message.text }));
  }

  private onStreamEvent(id: number, event: ChatStreamEvent): void {
    if (event.type === 'token') {
      this.aiAvailable.set(true);
      this.thinking.set(false);
      this.appendText(id, event.text);
    } else if (event.type === 'sources') {
      this.setSources(id, event.sources);
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

  private onStreamError(id: number, error: unknown): void {
    if ((error as { name?: string })?.name === 'AbortError') {
      if (!this.textOf(id)) {
        this.updateMessage(id, 'Réponse interrompue.');
      }
    } else {
      this.aiAvailable.set(false);
      this.updateMessage(id, "Je n'ai pas pu joindre le service IA. Réessayez dans un instant.");
    }
    this.finish();
  }

  private finish(): void {
    this.controller = null;
    this.sending.set(false);
    this.thinking.set(false);
    this.scrollToBottom();
    if (this.open()) {
      this.composer()?.nativeElement.focus();
    }
    this.save();
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

  private setSources(id: number, sources: string[]): void {
    if (!sources || sources.length === 0) {
      return;
    }
    this.messages.update(list => list.map(message =>
      message.id === id ? { ...message, sources } : message));
  }

  private loadServerHistory(): void {
    this.chatbotService.loadHistory()
      .then(messages => {
        if (!this.sending() && messages.length > 0) {
          this.applyServerHistory(messages);
        }
      })
      .catch(() => undefined);
  }

  private applyServerHistory(messages: ChatHistoryMessage[]): void {
    const mapped: ChatMessage[] = [
      this.welcome(),
      ...messages.map((message, index) => ({
        id: index + 1,
        from: message.role,
        text: message.content,
      })),
    ];
    this.nextId = mapped.reduce((max, message) => Math.max(max, message.id), 0) + 1;
    this.messages.set(mapped);
    this.save();
    this.scrollToBottom();
  }

  private textOf(id: number): string {
    return this.messages().find(message => message.id === id)?.text ?? '';
  }

  private autoGrow(): void {
    const el = this.composer()?.nativeElement;
    if (!el) {
      return;
    }
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  }

  private resetComposer(): void {
    const el = this.composer()?.nativeElement;
    if (el) {
      el.style.height = 'auto';
    }
  }

  private scrollToBottom(): void {
    const area = this.messagesArea()?.nativeElement;
    if (area) {
      area.scrollTop = area.scrollHeight;
    }
  }

  private welcome(): ChatMessage {
    return {
      id: 0,
      from: 'assistant',
      text: 'Bonjour, je suis votre assistant RH. Posez-moi une question sur vos congés, vos tâches ou les projets.',
    };
  }

  private restore(): ChatMessage[] {
    try {
      const raw = sessionStorage.getItem(Chatbot.STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as ChatMessage[];
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.nextId = parsed.reduce((max, message) => Math.max(max, message.id), 0) + 1;
          return parsed;
        }
      }
    } catch {
      // stockage indisponible : on repart de l'accueil
    }
    return [this.welcome()];
  }

  private save(): void {
    try {
      sessionStorage.setItem(Chatbot.STORAGE_KEY, JSON.stringify(this.messages()));
    } catch {
      // quota dépassé : on ignore
    }
  }
}
