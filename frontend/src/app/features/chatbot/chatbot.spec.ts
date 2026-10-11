import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Chatbot } from './chatbot';
import { ChatbotService } from '../../core/services/chatbot.service';
import { ChatStreamEvent } from '../../core/models/chat.model';

describe('Chatbot', () => {
  let serviceMock: {
    loadHistory: ReturnType<typeof vi.fn>;
    clearHistory: ReturnType<typeof vi.fn>;
    stream: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    sessionStorage.clear();
    serviceMock = {
      loadHistory: vi.fn().mockResolvedValue([]),
      clearHistory: vi.fn().mockResolvedValue(undefined),
      stream: vi.fn().mockImplementation(
        (_message: string, _history: unknown, onEvent: (event: ChatStreamEvent) => void) => {
          onEvent({ type: 'token', text: 'Bonjour' });
          onEvent({ type: 'done' });
          return Promise.resolve();
        },
      ),
    };
    await TestBed.configureTestingModule({
      imports: [Chatbot],
      providers: [
        provideNoopAnimations(),
        { provide: ChatbotService, useValue: serviceMock },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('starts with the welcome message and loads server history', () => {
    const component = TestBed.createComponent(Chatbot).componentInstance as unknown as { messages: () => unknown[] };
    expect(component.messages().length).toBe(1);
    expect(serviceMock.loadHistory).toHaveBeenCalled();
  });

  it('restores the conversation from sessionStorage', () => {
    sessionStorage.setItem('apprh.chatbot.messages', JSON.stringify([
      { id: 0, from: 'assistant', text: 'Salut' },
      { id: 1, from: 'user', text: 'Coucou' },
    ]));
    const component = TestBed.createComponent(Chatbot).componentInstance as unknown as { messages: () => unknown[] };
    expect(component.messages().length).toBe(2);
  });

  it('appends the streamed reply after submitting', async () => {
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { from: string; text: string }[];
      submit: (text: string) => void;
    };
    component.submit('Ma question');
    await fixture.whenStable();
    const texts = component.messages().map(message => `${message.from}:${message.text}`);
    expect(texts).toContain('user:Ma question');
    expect(texts).toContain('assistant:Bonjour');
  });

  it('clears the conversation and notifies the backend', async () => {
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => unknown[];
      submit: (text: string) => void;
      clear: () => void;
    };
    component.submit('Ma question');
    await fixture.whenStable();
    component.clear();
    await fixture.whenStable();
    expect(component.messages().length).toBe(1);
    expect(serviceMock.clearHistory).toHaveBeenCalled();
  });

  it('attaches documentary sources returned by the stream', async () => {
    serviceMock.stream = vi.fn().mockImplementation(
      (_message: string, _history: unknown, onEvent: (event: ChatStreamEvent) => void) => {
        onEvent({ type: 'sources', sources: ['Télétravail'] });
        onEvent({ type: 'token', text: 'Deux jours par semaine.' });
        onEvent({ type: 'done' });
        return Promise.resolve();
      },
    );
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { from: string; text: string; sources?: string[] }[];
      submit: (text: string) => void;
    };
    component.submit('Combien de jours de télétravail ?');
    await fixture.whenStable();
    component.submit('Combien de jours de télétravail ?');
    await fixture.whenStable();
    const withSources = component.messages().find(message => message.sources?.length);
    expect(withSources?.sources).toContain('Télétravail');
  });

  it('marks the assistant unavailable when a fallback event is received', async () => {
    serviceMock.stream = vi.fn().mockImplementation(
      (_message: string, _history: unknown, onEvent: (event: ChatStreamEvent) => void) => {
        onEvent({ type: 'fallback', text: 'Service IA indisponible.' });
        onEvent({ type: 'done' });
        return Promise.resolve();
      },
    );
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { text: string }[];
      aiAvailable: () => boolean;
      submit: (text: string) => void;
    };
    component.submit('Bonjour');
    await fixture.whenStable();
    expect(component.aiAvailable()).toBe(false);
    expect(component.messages().some(message => message.text.includes('indisponible'))).toBe(true);
  });

  it('shows an error message when the stream fails', async () => {
    serviceMock.stream = vi.fn().mockRejectedValue(new Error('API 500'));
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { text: string }[];
      aiAvailable: () => boolean;
      submit: (text: string) => void;
    };
    component.submit('Bonjour');
    await fixture.whenStable();
    expect(component.aiAvailable()).toBe(false);
    expect(component.messages().some(message => message.text.includes("n'ai pas pu joindre"))).toBe(true);
  });

  it('reports an interrupted answer when aborted before any token', async () => {
    serviceMock.stream = vi.fn().mockRejectedValue(
      Object.assign(new Error('aborted'), { name: 'AbortError' }),
    );
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { text: string }[];
      submit: (text: string) => void;
    };
    component.submit('Bonjour');
    await fixture.whenStable();
    expect(component.messages().some(message => message.text === 'Réponse interrompue.')).toBe(true);
  });

  it('aborts the active stream when stop() is called', () => {
    let capturedSignal: AbortSignal | undefined;
    serviceMock.stream = vi.fn().mockImplementation(
      (_message: string, _history: unknown, _onEvent: unknown, signal?: AbortSignal) => {
        capturedSignal = signal;
        return new Promise<void>(() => undefined);
      },
    );
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      submit: (text: string) => void;
      stop: () => void;
    };
    component.submit('Bonjour');
    expect(capturedSignal?.aborted).toBe(false);
    component.stop();
    expect(capturedSignal?.aborted).toBe(true);
  });

  it('hides suggestions once a message is sent', () => {
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      showSuggestions: () => boolean;
      submit: (text: string) => void;
    };
    expect(component.showSuggestions()).toBe(true);
    component.submit('Bonjour');
    expect(component.showSuggestions()).toBe(false);
  });

  it('sends the draft on Enter without Shift', () => {
    const fixture = TestBed.createComponent(Chatbot);
    const component = fixture.componentInstance as unknown as {
      messages: () => { from: string; text: string }[];
      onDraftInput: (event: Event) => void;
      onKeydown: (event: KeyboardEvent) => void;
    };
    component.onDraftInput({ target: { value: 'Bonjour' } } as unknown as Event);
    component.onKeydown({ key: 'Enter', shiftKey: false, preventDefault: vi.fn() } as unknown as KeyboardEvent);
    expect(component.messages().some(message => message.from === 'user' && message.text === 'Bonjour')).toBe(true);
  });
});
