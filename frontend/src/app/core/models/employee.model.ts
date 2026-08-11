export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export interface EmployeeSkillDto {
  skillId: number;
  skillName: string;
  category: string;
  level: number;
}

export interface EmployeeDto {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  jobTitle: string;
  hireDate: string;
  departmentId?: number;
  departmentName?: string;
  status: EmployeeStatus;
  skills: EmployeeSkillDto[];
  createdAt: string;
}

export interface EmployeeCreateRequest {
  userId: number;
  departmentId?: number;
  jobTitle: string;
  hireDate: string;
}

export interface EmployeeUpdateRequest {
  departmentId?: number;
  jobTitle?: string;
  hireDate?: string;
  status?: EmployeeStatus;
}

export interface EmployeeSkillRequest {
  skillId: number;
  level: number;
}
