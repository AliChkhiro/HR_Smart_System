import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from ..chat.prompts import SYSTEM_PROMPT
from ..chat.ollama import OllamaClient
from ..rag import get_retriever


router = APIRouter(tags=["chat"])

TOP_K_DOCS = 3


class HistoryMessage(BaseModel):
    role: str = Field(pattern="^(user|assistant)$")
    content: str = Field(min_length=1, max_length=4000)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    user_context: dict | None = None
    history: list[HistoryMessage] | None = None


class ChatResponse(BaseModel):
    reply: str
    ai_available: bool
    sources: list[str] = Field(default_factory=list)


FALLBACK_REPLY = (
    "L'IA est momentanément indisponible. Je reste à votre écoute avec des "
    "réponses contextuelles une fois reconnectée."
)


def retrieve(message: str) -> list:
    try:
        return get_retriever().retrieve(message, top_k=TOP_K_DOCS)
    except Exception:
        return []


def sources_of(retrieved: list) -> list[str]:
    titles: list[str] = []
    for item in retrieved:
        title = item.chunk.doc_title
        if title not in titles:
            titles.append(title)
    return titles


def build_messages(req: ChatRequest, retrieved: list | None = None) -> list[dict]:
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    if retrieved:
        passages = "\n\n".join(
            f"[{item.chunk.doc_title} — {item.chunk.heading}]\n{item.chunk.text}"
            for item in retrieved
        )
        messages.append({
            "role": "system",
            "content": (
                "Documentation RH (extraits pertinents, source de vérité en lecture "
                "seule). Pour les questions de politique ou de procédure, appuie-toi "
                "sur ces extraits et cite le titre du document entre crochets, par "
                "exemple [Télétravail] :\n\n" + passages
            ),
        })
    if req.user_context:
        messages.append({
            "role": "system",
            "content": "Contexte utilisateur (source unique de vérité, lecture seule) :\n"
                       + json.dumps(req.user_context, ensure_ascii=False),
        })
    if req.history:
        for item in req.history[-8:]:
            messages.append({"role": item.role, "content": item.content})
    messages.append({"role": "user", "content": req.message})
    return messages


@router.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    retrieved = retrieve(req.message)
    try:
        text = OllamaClient().generate(build_messages(req, retrieved))
        if not text:
            text = "Je n'ai pas pu générer de réponse."
        return ChatResponse(reply=text, ai_available=True, sources=sources_of(retrieved))
    except Exception:
        return ChatResponse(reply=FALLBACK_REPLY, ai_available=False, sources=[])


@router.post("/chat/stream")
def chat_stream(req: ChatRequest):
    retrieved = retrieve(req.message)
    sources = sources_of(retrieved)
    messages = build_messages(req, retrieved)

    def generate():
        if sources:
            yield json.dumps({"type": "sources", "sources": sources}) + "\n"
        produced = False
        try:
            for chunk in OllamaClient().generate_stream(messages):
                produced = True
                yield json.dumps({"type": "token", "text": chunk}, ensure_ascii=False) + "\n"
            yield json.dumps({"type": "done"}) + "\n"
        except Exception:
            if produced:
                yield json.dumps({"type": "done", "interrupted": True}) + "\n"
            else:
                yield json.dumps({"type": "fallback", "text": FALLBACK_REPLY},
                                 ensure_ascii=False) + "\n"

    return StreamingResponse(generate(), media_type="application/x-ndjson")
