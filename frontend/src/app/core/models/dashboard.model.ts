export interface DashboardStats {
  employeeCount: number;
  projectCount: number;
  tasksByStatus: Record<string, number>;
  overdueTaskCount: number;
  upcomingTaskCount: number;
  latestOverdueTask?: TaskSummary;
  nextUpcomingTask?: TaskSummary;
  pendingLeaveCount: number;
  unreadNotificationCount: number;
}

export interface TaskSummary {
  id: number;
  name: string;
  projectName?: string;
  assigneeName?: string;
  status: string;
  priority: string;
  dueDate: string;
}