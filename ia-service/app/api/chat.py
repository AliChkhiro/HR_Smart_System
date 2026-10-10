import json

from fastapi import APIRouter
from pydantic import BaseModel, Field

from ..chat.prompts import SYSTEM_PROMPT
from ..chat.ollama import OllamaClient


router = APIRouter(tags=["chat"])


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    user_context: dict | None = None


class ChatResponse(BaseModel):
    reply: str
    ai_available: bool


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    client = OllamaClient()
    try:
        messages = [{"role": "system", "content": SYSTEM_PROMPT}]
        if req.user_context:
            messages.append({
                "role": "system",
                "content": "Contexte utilisateur (données personnelles autorisées, en lecture seule):\n"
                           + json.dumps(req.user_context, ensure_ascii=False),
            })
        messages.append({"role": "user", "content": req.message})
        text = client.generate(messages)
        if not text:
            text = "Je n'ai pas pu générer de réponse."
        return ChatResponse(reply=text, ai_available=True)
    except Exception:
        return ChatResponse(
            reply="L'IA est momentanément indisponible. Je reste à votre écoute avec des réponses contextuelles une fois reconnectée.",
            ai_available=False,
        )
