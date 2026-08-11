export type LeaveType = 'ANNUAL' | 'SICK' | 'MATERNITY' | 'PATERNITY' | 'UNPAID' | 'OTHER';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveDto {
  id: number;
  employeeId: number;
  employeeName: string;
  type: LeaveType;
  startDate: string;
  endDate: string;
  status: LeaveStatus;
  reason?: string;
  reviewerId?: number;
  reviewerName?: string;
  reviewDate?: string;
  reviewComment?: string;
  createdAt: string;
}

export interface LeaveCreateRequest {
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface LeaveReviewRequest {
  status: 'APPROVED' | 'REJECTED';
  comment?: string;
}
