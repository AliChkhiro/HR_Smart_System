import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatbotService } from './chatbot.service';
import { AuthService } from './auth.service';
import { ChatStreamEvent } from '../models/chat.model';

describe('ChatbotService', () => {
  let service: ChatbotService;
  let http: HttpTestingController;
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ChatbotService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { accessToken: 'test-token' } },
      ],
    });
    service = TestBed.inject(ChatbotService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    globalThis.fetch = originalFetch;
  });

  function mockStream(chunks: string[]): void {
    let index = 0;
    const encoder = new TextEncoder();
    globalThis.fetch = vi.fn(async () => ({
      ok: true,
      status: 200,
      body: {
        getReader: () => ({
          read: async () =>
            index < chunks.length
              ? { value: encoder.encode(chunks[index++]), done: false }
              : { value: undefined, done: true },
        }),
      },
    })) as unknown as typeof fetch;
  }

  it('posts the message for ask()', () => {
    service.ask('bonjour').subscribe();
    const req = http.expectOne(request => request.url.endsWith('/chatbot/chat'));
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ message: 'bonjour' });
    req.flush({ reply: 'salut', aiAvailable: true });
  });

  it('loads the persisted history', async () => {
    const promise = service.loadHistory();
    const req = http.expectOne(request => request.url.endsWith('/chatbot/messages') && request.method === 'GET');
    req.flush([{ role: 'user', content: 'hi' }]);
    await expect(promise).resolves.toEqual([{ role: 'user', content: 'hi' }]);
  });

  it('clears the persisted history', async () => {
    const promise = service.clearHistory();
    const req = http.expectOne(request => request.url.endsWith('/chatbot/messages') && request.method === 'DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await expect(promise).resolves.toBeUndefined();
  });

  it('streams and parses NDJSON events', async () => {
    const encoder = new TextEncoder();
    const chunks = [
      encoder.encode('{"type":"token","text":"Bon"}\n{"type":"token","text":"jour"}\n'),
      encoder.encode('{"type":"done"}\n'),
    ];
    let index = 0;
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      body: {
        getReader: () => ({
          read: async () =>
            index < chunks.length
              ? { value: chunks[index++], done: false }
              : { value: undefined, done: true },
        }),
      },
    }));
    globalThis.fetch = fetchMock as unknown as typeof fetch;

    const events: ChatStreamEvent[] = [];
    await service.stream('salut', [], event => events.push(event));

    expect(events).toEqual([
      { type: 'token', text: 'Bon' },
      { type: 'token', text: 'jour' },
      { type: 'done' },
    ]);
    expect(fetchMock).toHaveBeenCalled();
  });

  it('throws when the stream response is not ok', async () => {
    globalThis.fetch = vi.fn(async () => ({ ok: false, status: 500, body: null })) as unknown as typeof fetch;
    await expect(service.stream('salut', [], () => undefined)).rejects.toThrow('API 500');
  });

  it('emits a sources event from the stream', async () => {
    mockStream(['{"type":"sources","sources":["Télétravail"]}\n{"type":"token","text":"Deux jours."}\n{"type":"done"}\n']);
    const events: ChatStreamEvent[] = [];
    await service.stream('question', [], event => events.push(event));
    expect(events).toEqual([
      { type: 'sources', sources: ['Télétravail'] },
      { type: 'token', text: 'Deux jours.' },
      { type: 'done' },
    ]);
  });

  it('reassembles events split across chunk boundaries', async () => {
    mockStream(['{"type":"tok', 'en","text":"Bon"}\n{"type":"do', 'ne"}\n']);
    const events: ChatStreamEvent[] = [];
    await service.stream('bonjour', [], event => events.push(event));
    expect(events).toEqual([
      { type: 'token', text: 'Bon' },
      { type: 'done' },
    ]);
  });

  it('ignores malformed lines and emits the final partial line', async () => {
    mockStream(['pas-json\n{"type":"token","text":"Hi"}\n', '{"type":"done"}']);
    const events: ChatStreamEvent[] = [];
    await service.stream('bonjour', [], event => events.push(event));
    expect(events).toEqual([
      { type: 'token', text: 'Hi' },
      { type: 'done' },
    ]);
  });
});
