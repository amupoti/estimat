#!/usr/bin/env python3
"""Valida questions.json contra tags.json i mostra estadístiques del banc.

Ús: python3 questions/validate.py
"""
import json
import sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).parent
DATA = HERE.parent / "app" / "src" / "main" / "assets" / "questions"
REQUIRED = ["id", "pregunta", "resposta", "unitat", "tags", "font", "confianca", "estat"]
CONFIANCA = {"alta", "mitjana", "baixa"}
ESTAT = {"pendent", "verificat", "rebutjat"}


def main() -> int:
    taxonomy = json.loads((DATA / "tags.json").read_text())["grups"]
    questions = json.loads((DATA / "questions.json").read_text())
    tag_group = {t: g for g, spec in taxonomy.items() for t in spec["tags"]}
    errors: list[str] = []
    seen_ids, seen_text = set(), {}

    for q in questions:
        qid = q.get("id", "?")
        err = lambda msg: errors.append(f"{qid}: {msg}")
        for f in REQUIRED:
            if f not in q:
                err(f"falta el camp '{f}'")
        if qid in seen_ids:
            err("id duplicat")
        seen_ids.add(qid)
        for lang in ("ca", "es"):
            text = (q.get("pregunta") or {}).get(lang, "").strip()
            if not text:
                err(f"falta la pregunta en '{lang}'")
            elif lang == "ca":
                if text.lower() in seen_text:
                    err(f"pregunta duplicada de {seen_text[text.lower()]}")
                seen_text[text.lower()] = qid
            if q.get("explicacio") and not q["explicacio"].get(lang):
                err(f"falta l'explicació en '{lang}'")
        if not isinstance(q.get("resposta"), (int, float)) or isinstance(q.get("resposta"), bool):
            err("la resposta ha de ser numèrica")
        if q.get("confianca") not in CONFIANCA:
            err(f"confianca ha de ser una de {sorted(CONFIANCA)}")
        if q.get("estat") not in ESTAT:
            err(f"estat ha de ser un de {sorted(ESTAT)}")
        if not str(q.get("font", "")).startswith("http"):
            err("la font ha de ser una URL")

        tags = q.get("tags", [])
        unknown = [t for t in tags if t not in tag_group]
        if unknown:
            err(f"tags desconeguts: {unknown}")
        per_group = Counter(tag_group[t] for t in tags if t in tag_group)
        for g, spec in taxonomy.items():
            n = per_group.get(g, 0)
            if spec["obligatori"] and n == 0:
                err(f"cal almenys un tag del grup '{g}'")
            if not spec["multiple"] and n > 1:
                err(f"només es permet un tag del grup '{g}'")
        if len(tags) != len(set(tags)):
            err("tags repetits")

    for e in errors:
        print("ERROR", e)

    print(f"\n{len(questions)} preguntes, {len(errors)} errors\n")
    for g in taxonomy:
        c = Counter(t for q in questions for t in q.get("tags", []) if tag_group.get(t) == g)
        print(f"{g:11} " + "  ".join(f"{t}={n}" for t, n in c.most_common()))
    print("confianca   " + "  ".join(f"{k}={v}" for k, v in Counter(q.get("confianca") for q in questions).most_common()))
    print("estat       " + "  ".join(f"{k}={v}" for k, v in Counter(q.get("estat") for q in questions).most_common()))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
