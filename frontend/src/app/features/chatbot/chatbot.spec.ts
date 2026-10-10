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
});
