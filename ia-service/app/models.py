from pydantic import BaseModel, Field


class TaskProfile(BaseModel):
    id: int
    required_skill_ids: list[int] = Field(default_factory=list)
    start_date: str | None = None
    due_date: str | None = None
    priority: int = Field(default=2, ge=1, le=5)


class EmployeeProfile(BaseModel):
    id: int
    skill_ids: list[int] = Field(default_factory=list)
    ongoing_task_count: int = 0
    open_tasks: list[dict] = Field(default_factory=list)
    leave_periods: list[dict] = Field(default_factory=list)
    reliability: float = Field(default=1.0, ge=0.0, le=1.0)


class RecommendationRequest(BaseModel):
    task: TaskProfile
    employees: list[EmployeeProfile]
    weights: dict[str, float] | None = None


class CriterionScore(BaseModel):
    name: str
    score: float
    explanation: str


class RecommendationResult(BaseModel):
    employee_id: int
    total_score: float
    criteria: list[CriterionScore]
    explanation: str


class RecommendationResponse(BaseModel):
    recommendations: list[RecommendationResult]
