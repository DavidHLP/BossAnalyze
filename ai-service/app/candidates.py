import re
from dataclasses import dataclass


@dataclass
class Candidate:
    id: str
    text: str
    evidence: list[dict]
    context: str


def candidates(detail: dict, prefix: str = "") -> list[Candidate]:
    """Only factual source fields; never consume legacy AI output."""
    sources = {k: detail[k] for k in ("jobDescription", "jobTags", "basicInfo") if k in detail}
    result: list[Candidate] = []
    by_text: dict[str, Candidate] = {}

    def walk(value, path):
        if isinstance(value, dict):
            for key, child in value.items():
                walk(child, path + "/" + key.replace("~", "~0").replace("/", "~1"))
        elif isinstance(value, list):
            for i, child in enumerate(value):
                walk(child, f"{path}/{i}")
        elif isinstance(value, str):
            for part in re.finditer(r"[^\n\r;；]+", value):
                start, end = part.span()
                while start < end and value[start].isspace():
                    start += 1
                while end > start and value[end - 1].isspace():
                    end -= 1
                # Overlap preserves conditions around a long-window boundary.
                cursor = start
                while cursor < end:
                    stop = min(cursor + 800, end)
                    text = value[cursor:stop]
                    evidence = dict(sourcePath=path, start=cursor, end=stop, text=text)
                    if text in by_text:
                        by_text[text].evidence.append(evidence)
                    else:
                        item = Candidate(f"c{len(result)}", text, [evidence],
                                         value[max(0, cursor - 160):min(len(value), stop + 160)])
                        result.append(item)
                        by_text[text] = item
                    if stop == end:
                        break
                    cursor = stop - 160
    walk(sources, prefix)
    return result


def resume_candidates(text: str) -> list[Candidate]:
    return [Candidate("r0", text, [dict(sourcePath="/resume", start=0, end=len(text), text=text)], text)] if text.strip() else []
