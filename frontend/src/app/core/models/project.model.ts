export type ProjectStatus = 'PLANNED' | 'IN_PROGRESS' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type ProjectMemberRole = 'MANAGER' | 'MEMBER';

export interface ProjectDto {
  id: number;
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  status: ProjectStatus;
  priority: ProjectPriority;
  memberCount: number;
  createdAt: string;
}

export interface ProjectMemberDto {
  employeeId: number;
  employeeName: string;
  jobTitle: string;
  role: ProjectMemberRole;
}

export interface ProjectDetailDto extends ProjectDto {
  members: ProjectMemberDto[];
}

export interface ProjectRequest {
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  status?: ProjectStatus;
  priority?: ProjectPriority;
}

export interface ProjectUpdateRequest {
  name?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  status?: ProjectStatus;
  priority?: ProjectPriority;
}

export interface ProjectMemberRequest {
  employeeId: number;
  role: ProjectMemberRole;
}
