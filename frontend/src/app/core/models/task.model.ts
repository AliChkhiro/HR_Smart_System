export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface TaskDto {
  id: number;
  name: string;
  description?: string;
  projectId?: number;
  projectName?: string;
  assigneeId?: number;
  assigneeName?: string;
  skillIds?: number[];
  status: TaskStatus;
  priority: TaskPriority;
  estimatedHours?: number;
  startDate?: string;
  dueDate?: string;
  createdAt: string;
}

export interface TaskCreateRequest {
  name: string;
  description?: string;
  projectId: number;
  assigneeId?: number;
  skillIds?: number[];
  status?: TaskStatus;
  priority?: TaskPriority;
  estimatedHours?: number;
  startDate?: string;
  dueDate?: string;
}

export interface TaskUpdateRequest {
  name?: string;
  description?: string;
  projectId?: number;
  assigneeId?: number;
  skillIds?: number[];
  status?: TaskStatus;
  priority?: TaskPriority;
  estimatedHours?: number;
  startDate?: string;
  dueDate?: string;
}

export interface TaskStatusRequest {
  status: TaskStatus;
}

export interface CriterionScoreDto {
  name: string;
  score: number;
  explanation: string;
}

export interface RecommendationDto {
  employeeId: number;
  employeeName: string;
  totalScore: number;
  criteria: CriterionScoreDto[];
  explanation: string;
}
