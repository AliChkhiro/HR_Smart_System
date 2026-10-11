export type ChatAuthor = 'user' | 'assistant';

export interface ChatReplyDto {
  reply: string;
  aiAvailable: boolean;
  sources?: string[];
}

export interface ChatHistoryMessage {
  role: ChatAuthor;
  content: string;
}

export type ChatStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'sources'; sources: string[] }
  | { type: 'done'; interrupted?: boolean }
  | { type: 'fallback'; text: string };
