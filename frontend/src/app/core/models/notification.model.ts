export type NotificationType = 'LEAVE_REQUEST' | 'LEAVE_REVIEW' | 'TASK_ASSIGNED';

export interface NotificationDto {
  id: number;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}
