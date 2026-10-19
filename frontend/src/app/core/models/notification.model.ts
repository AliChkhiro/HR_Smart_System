export type NotificationType =
  | 'LEAVE_REQUEST'
  | 'LEAVE_REVIEW'
  | 'TASK_ASSIGNED'
  | 'USER_REGISTRATION'
  | 'USER_APPROVED'
  | 'USER_REJECTED'
  | 'ERASURE_REQUESTED'
  | 'ERASURE_DECIDED';

export interface NotificationDto {
  id: number;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}
