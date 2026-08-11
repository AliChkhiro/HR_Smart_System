from fastapi import APIRouter

from ..models import RecommendationRequest, RecommendationResponse
from ..scoring import recommend

router = APIRouter(tags=["recommendation"])


@router.post("/tasks/recommend", response_model=RecommendationResponse)
def recommend_assignments(request: RecommendationRequest) -> RecommendationResponse:
    results = recommend(request)
    return RecommendationResponse(recommendations=results)
