import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
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


FALLBACK_REPLY = (
    "L'IA est momentanément indisponible. Je reste à votre écoute avec des "
    "réponses contextuelles une fois reconnectée."
)


def build_messages(req: ChatRequest) -> list[dict]:
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    if req.user_context:
        messages.append({
            "role": "system",
            "content": "Contexte utilisateur (données personnelles autorisées, en lecture seule):\n"
                       + json.dumps(req.user_context, ensure_ascii=False),
        })
    messages.append({"role": "user", "content": req.message})
    return messages


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    try:
        text = OllamaClient().generate(build_messages(req))
        if not text:
            text = "Je n'ai pas pu générer de réponse."
        return ChatResponse(reply=text, ai_available=True)
    except Exception:
        return ChatResponse(reply=FALLBACK_REPLY, ai_available=False)


@router.post("/chat/stream")
def chat_stream(req: ChatRequest):
    def event_stream():
        produced = False
        try:
            for delta in OllamaClient().generate_stream(build_messages(req)):
                produced = True
                yield json.dumps({"type": "token", "text": delta}, ensure_ascii=False) + "\n"
            yield json.dumps({"type": "done"}) + "\n"
        except Exception:
            if produced:
                yield json.dumps({"type": "done", "interrupted": True}) + "\n"
            else:
                yield json.dumps({"type": "fallback", "text": FALLBACK_REPLY},
                                 ensure_ascii=False) + "\n"

    return StreamingResponse(event_stream(), media_type="application/x-ndjson")
