export type ChatAuthor = 'user' | 'assistant';

export interface ChatReplyDto {
  reply: string;
  aiAvailable: boolean;
}
