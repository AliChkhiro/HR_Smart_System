from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

DEFAULT_DOCS_DIR = os.getenv("HR_DOCS_DIR", "data/hr_docs")


@dataclass(frozen=True)
class Chunk:
    """Section d'un document RH, unité de base de l'index de recherche."""

    doc_title: str
    source: str
    heading: str
    text: str


def _split_sections(text: str) -> tuple[str | None, list[tuple[str, str]]]:
    """Découpe un document Markdown en (titre, [(sous-titre, corps), ...])."""
    title: str | None = None
    sections: list[tuple[str, str]] = []
    current_heading = ""
    body_lines: list[str] = []

    def flush() -> None:
        body = "\n".join(body_lines).strip()
        if body:
            sections.append((current_heading, body))
        body_lines.clear()

    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("## "):
            flush()
            current_heading = stripped[3:].strip()
        elif stripped.startswith("# "):
            title = stripped[2:].strip()
        else:
            body_lines.append(line)
    flush()
    return title, sections


def load_corpus(docs_dir: str | None = None) -> list[Chunk]:
    directory = Path(docs_dir or DEFAULT_DOCS_DIR)
    if not directory.is_dir():
        return []
    chunks: list[Chunk] = []
    for path in sorted(directory.glob("*.md")):
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            continue
        doc_title, sections = _split_sections(text)
        resolved_title = doc_title or path.stem.replace("-", " ").replace("_", " ").title()
        for heading, body in sections:
            chunks.append(Chunk(resolved_title, path.name, heading, body))
    return chunks
