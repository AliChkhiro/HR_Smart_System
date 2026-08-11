export interface SkillDto {
  id: number;
  name: string;
  category: string;
  employeeCount: number;
  createdAt: string;
}

export interface SkillRequest {
  name: string;
  category: string;
}
