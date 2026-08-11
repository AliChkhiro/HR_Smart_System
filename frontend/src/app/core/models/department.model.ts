export interface DepartmentDto {
  id: number;
  name: string;
  description?: string;
  managerId?: number;
  managerName?: string;
  employeeCount: number;
  createdAt: string;
}

export interface DepartmentRequest {
  name: string;
  description?: string;
  managerId?: number;
}
