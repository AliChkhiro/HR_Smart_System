export type ErasureStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ErasureRequestDto {
  id: number;
  userId: number;
  userEmail: string | null;
  status: ErasureStatus;
  reason: string | null;
  requestedAt: string;
  decidedAt: string | null;
  decidedByUserId: number | null;
}

export interface AuditLogDto {
  id: number;
  actorUserId: number | null;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  detail: string | null;
  createdAt: string;
}

export interface PersonalDataExport {
  generatedAt: string;
  account: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    status: string;
    createdAt: string;
  };
  employee: {
    id: number;
    jobTitle: string;
    hireDate: string;
    department: string | null;
    status: string;
  } | null;
  skills: { name: string; category: string; level: number }[];
  leaves: {
    type: string;
    startDate: string;
    endDate: string;
    status: string;
    reason: string | null;
    createdAt: string;
  }[];
  chatMessages: { role: string; content: string; createdAt: string }[];
}
