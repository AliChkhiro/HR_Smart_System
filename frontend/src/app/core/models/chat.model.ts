export type ChatAuthor = 'user' | 'assistant';

export interface ChatReplyDto {
  reply: string;
  aiAvailable: boolean;
}

export type ChatStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'done'; interrupted?: boolean }
  | { type: 'fallback'; text: string };
