from __future__ import annotations

import math
import re
import unicodedata
from dataclasses import dataclass

from .corpus import Chunk

_STOPWORDS = {
    "les", "des", "une", "des", "aux", "des", "une", "est", "sont", "pour",
    "avec", "sans", "dans", "sous", "vers", "chez", "sur", "par", "que",
    "qui", "quoi", "dont", "ou", "et", "or", "ni", "car", "donc", "mais",
    "je", "tu", "il", "elle", "on", "nous", "vous", "ils", "elles", "me",
    "te", "se", "mon", "ton", "son", "ma", "ta", "sa", "mes", "tes", "ses",
    "notre", "votre", "leur", "nos", "vos", "leurs", "ce", "cet", "cette",
    "ces", "ne", "pas", "plus", "moins", "tres", "tout", "tous", "toute",
    "toutes", "bien", "comment", "pourquoi", "quand", "quel", "quelle",
    "quels", "quelles", "suis", "es", "sommes", "etes", "sont", "etre",
    "avoir", "faire", "peux", "peut", "ai", "as", "avons", "avez", "ont",
    "mes", "mon", "ma", "the", "and", "for", "with", "you", "are", "est",
    "quel", "quelle", "y", "a", "ou", "ni", "combien",
}


def tokenize(text: str) -> list[str]:
    normalized = unicodedata.normalize("NFD", text.lower())
    normalized = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    tokens = re.findall(r"[a-z0-9]+", normalized)
    return [token for token in tokens if len(token) > 2 and token not in _STOPWORDS]


class RetrievedChunk:
    __slots__ = ("chunk", "score")

    def __init__(self, chunk, score: float):
        self.chunk = chunk
        self.score = score

    def __repr__(self) -> str:  # pragma: no cover - debug
        return f"RetrievedChunk(score={self.score:.2f}, title={self.chunk.doc_title!r})"


class Bm25Index:
    """Index BM25 minimaliste, sans dépendance externe (100 % local)."""

    def __init__(self, chunks: list, k1: float = 1.5, b: float = 0.75):
        self.chunks = chunks
        self.k1 = k1
        self.b = b
        self._doc_tokens = [tokenize(f"{chunk.heading} {chunk.text}") for chunk in chunks]
        self._doc_len = [len(tokens) for tokens in self._doc_tokens]
        self._avg_len = (sum(self._doc_len) / len(self._doc_len)) if self._doc_len else 0.0
        self._doc_freq: dict[str, int] = {}
        for tokens in self._doc_tokens:
            for term in set(tokens):
                self._doc_freq[term] = self._doc_freq.get(term, 0) + 1
        self._n = len(chunks)

    def _idf(self, term: str) -> float:
        df = self._doc_freq.get(term, 0)
        return math.log(1 + (self._n - df + 0.5) / (df + 0.5))

    def search(self, query: str, top_k: int = 3, min_score: float = 0.5) -> list:
        query_terms = set(tokenize(query))
        if not query_terms or self._n == 0:
            return []
        scored: list[RetrievedChunk] = []
        for index, tokens in enumerate(self._doc_tokens):
            term_freq: dict[str, int] = {}
            for token in tokens:
                if token in query_terms:
                    term_freq[token] = term_freq.get(token, 0) + 1
            if not term_freq:
                continue
            score = 0.0
            length = self._doc_len[index] or 1
            for term, freq in term_freq.items():
                norm = freq + self.k1 * (1 - self.b + self.b * length / (self._avg_len or 1.0))
                score += self._idf(term) * (freq * (self.k1 + 1)) / norm
            if score > 0:
                scored.append(RetrievedChunk(self.chunks[index], score))
        scored.sort(key=lambda item: item.score, reverse=True)
        return [item for item in scored if item.score >= min_score][:top_k]


class HrRetriever:
    """Charge le corpus RH une seule fois et expose une recherche par requête."""

    def __init__(self, docs_dir: str | None = None):
        from .corpus import load_corpus

        self._index = Bm25Index(load_corpus(docs_dir))

    def retrieve(self, query: str, top_k: int = 3) -> list:
        return self._index.search(query, top_k=top_k)

    def documents(self) -> list[str]:
        return sorted({chunk.doc_title for chunk in self._index.chunks})


_retriever: HrRetriever | None = None


def get_retriever() -> HrRetriever:
    global _retriever
    if _retriever is None:
        _retriever = HrRetriever()
    return _retriever
