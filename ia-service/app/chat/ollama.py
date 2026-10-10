import json
import os

import httpx


class OllamaClient:
    def __init__(self):
        self.url = os.getenv("OLLAMA_URL", "http://ollama:11434")
        self.model = os.getenv("OLLAMA_MODEL", "llama3.2:3b")
        self.timeout = int(os.getenv("CHATBOT_TIMEOUT_SECONDS", "30"))

    def _payload(self, messages: list[dict], stream: bool,
                 temperature: float, top_p: float, num_predict: int) -> dict:
        return {
            "model": self.model,
            "messages": messages,
            "stream": stream,
            "keep_alive": os.getenv("OLLAMA_KEEP_ALIVE", "24h"),
            "options": {
                "temperature": temperature,
                "top_p": top_p,
                "num_predict": num_predict,
                "repeat_penalty": 1.1,
            },
        }

    def generate(self, messages: list[dict], temperature: float = 0.1, top_p: float = 0.9,
                 num_predict: int = 180) -> str:
        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    f"{self.url}/api/chat",
                    json=self._payload(messages, False, temperature, top_p, num_predict),
                )
                response.raise_for_status()
                data = response.json()
                message = data.get("message") or {}
                return (message.get("content") or "").strip()
        except Exception as exc:
            raise RuntimeError(str(exc))

    def generate_stream(self, messages: list[dict], temperature: float = 0.1, top_p: float = 0.9,
                        num_predict: int = 180):
        try:
            with httpx.Client(timeout=self.timeout) as client:
                with client.stream(
                    "POST",
                    f"{self.url}/api/chat",
                    json=self._payload(messages, True, temperature, top_p, num_predict),
                ) as response:
                    response.raise_for_status()
                    for line in response.iter_lines():
                        if not line:
                            continue
                        data = json.loads(line)
                        message = data.get("message") or {}
                        content = message.get("content")
                        if content:
                            yield content
                        if data.get("done"):
                            break
        except Exception as exc:
            raise RuntimeError(str(exc))
