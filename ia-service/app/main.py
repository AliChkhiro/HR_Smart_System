import os
import threading
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI

from app.api.recommendation import router as recommendation_router
from app.api.chat import router as chat_router


def _warm_up() -> None:
    """Précharge le modèle Ollama en tâche de fond pour éviter un cold start
    de ~20-25 s sur la première requête. Non bloquant et silencieux en cas
    d'échec (Ollama peut démarrer plus lentement que ce service)."""
    url = os.getenv("OLLAMA_URL", "http://ollama:11434")
    model = os.getenv("OLLAMA_MODEL", "llama3.2:3b")
    try:
        with httpx.Client(timeout=120) as client:
            client.post(
                f"{url}/api/chat",
                json={
                    "model": model,
                    "messages": [{"role": "user", "content": "ping"}],
                    "stream": False,
                    "keep_alive": os.getenv("OLLAMA_KEEP_ALIVE", "24h"),
                    "options": {"num_predict": 1},
                },
            )
    except Exception:
        pass


@asynccontextmanager
async def lifespan(app: FastAPI):
    if os.getenv("OLLAMA_WARMUP", "true").lower() in {"1", "true", "yes", "on"}:
        threading.Thread(target=_warm_up, name="ollama-warmup", daemon=True).start()
    yield


app = FastAPI(
    title="AppRH IA Service",
    description="Moteur de scoring pour la recommandation d'attribution de tâches (Sprint 5).",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(recommendation_router, prefix="/api/v1")
app.include_router(chat_router, prefix="/api/v1")


@app.get("/health", tags=["health"])
def health() -> dict[str, str]:
    return {"status": "ok"}
