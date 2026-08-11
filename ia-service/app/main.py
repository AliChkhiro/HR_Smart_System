from fastapi import FastAPI

from app.api.recommendation import router as recommendation_router

app = FastAPI(
    title="AppRH IA Service",
    description="Moteur de scoring pour la recommandation d'attribution de tâches (Sprint 5).",
    version="0.1.0",
)

app.include_router(recommendation_router, prefix="/api/v1")

@app.get("/health", tags=["health"])
def health() -> dict[str, str]:
    return {"status": "ok"}
