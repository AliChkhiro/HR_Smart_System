import httpx
import os


class OllamaClient:
    def __init__(self):
        self.url = os.getenv("OLLAMA_URL", "http://ollama:11434")
        self.model = os.getenv("OLLAMA_MODEL", "llama3.2:3b")
        self.timeout = int(os.getenv("CHATBOT_TIMEOUT_SECONDS", "30"))

    def generate(self, messages: list[dict], temperature: float = 0.2, top_p: float = 0.9, num_predict: int = 180) -> str:
        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    f"{self.url}/api/chat",
                    json={
                        "model": self.model,
                        "messages": messages,
                        "stream": False,
                        "options": {
                            "temperature": temperature,
                            "top_p": top_p,
                            "num_predict": num_predict,
                        },
                    },
                )
                response.raise_for_status()
                data = response.json()
                message = data.get("message") or {}
                return (message.get("content") or "").strip()
        except Exception as exc:
            raise RuntimeError(str(exc))
