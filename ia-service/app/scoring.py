from .models import CriterionScore, EmployeeProfile, RecommendationResult, TaskProfile

DEFAULT_WEIGHTS = {
    "skills": 0.40,
    "workload": 0.25,
    "availability": 0.20,
    "reliability": 0.15,
}


def _skills_score(task: TaskProfile, employee: EmployeeProfile) -> tuple[float, str]:
    required = set(task.required_skill_ids)
    if not required:
        return 1.0, "aucune compétence requise sur la tâche"
    owned = set(employee.skill_ids)
    covered = required & owned
    if required == covered:
        return 1.0, f"{len(covered)}/{len(required)} compétences requises couvertes"
    return len(covered) / len(required), f"{len(covered)}/{len(required)} compétences requises couvertes"


def _workload_score(employee: EmployeeProfile) -> tuple[float, str]:
    count = employee.ongoing_task_count
    if count == 0:
        return 1.0, "aucune tâche en cours"
    if count <= 3:
        return 0.8, f"charge légère ({count} tâches en cours)"
    if count <= 6:
        return 0.5, f"charge modérée ({count} tâches en cours)"
    return 0.2, f"charge élevée ({count} tâches en cours)"


def _availability_score(task: TaskProfile, employee: EmployeeProfile) -> tuple[float, str]:
    if not employee.leave_periods or not task.start_date or not task.due_date:
        return 1.0, "disponible sur la période"
    task_start = _to_date(task.start_date)
    task_end = _to_date(task.due_date)
    for period in employee.leave_periods:
        start = _to_date(period.get("start"))
        end = _to_date(period.get("end"))
        if start and end and start <= task_end and end >= task_start:
            return 0.0, "congé validé sur la période de la tâche"
    return 1.0, "disponible sur la période"


def _reliability_score(employee: EmployeeProfile) -> tuple[float, str]:
    rate = employee.reliability
    return rate, f"{int(rate * 100)} % de respect des délais"


def _to_date(value: str) -> str:
    return value[:10]


def score_employee(task: TaskProfile, employee: EmployeeProfile, weights: dict[str, float]) -> RecommendationResult:
    weights = {**DEFAULT_WEIGHTS, **(weights or {})}
    criteria_data = {
        "skills": _skills_score(task, employee),
        "workload": _workload_score(employee),
        "availability": _availability_score(task, employee),
        "reliability": _reliability_score(employee),
    }

    usable = [name for name, (score, _) in criteria_data.items() if name != "skills" or task.required_skill_ids]
    total_weight = sum(weights[name] for name in usable)
    if total_weight == 0:
        total_weight = 1.0

    scores: list[CriterionScore] = []
    total = 0.0
    for name, (score, explanation) in criteria_data.items():
        weight = weights[name] if name in usable else 0.0
        total += weight * score
        scores.append(CriterionScore(name=name, score=score, explanation=explanation))

    total = total / total_weight
    summary = "; ".join(f"{c.explanation}" for c in scores)
    return RecommendationResult(
        employee_id=employee.id,
        total_score=round(total, 3),
        criteria=scores,
        explanation=summary,
    )


def recommend(request: "RecommendationRequest") -> list[RecommendationResult]:
    results = [
        score_employee(request.task, employee, request.weights or DEFAULT_WEIGHTS)
        for employee in request.employees
    ]
    results.sort(key=lambda r: r.total_score, reverse=True)
    return results
