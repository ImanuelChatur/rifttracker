#!/usr/bin/env python3
"""Fetch Legend art from Riot's Data Dragon and bundle it as app assets.

For every Legend in core/data/src/main/assets/legends/legends.json this writes:
  <id>.webp       512x512 portrait cropped from the top of the loading-screen art
  <id>_wide.webp  960 px wide centered splash, used as the tracker panel background
plus ATTRIBUTION.md listing every source URL and the Data Dragon version.

A file at legend-art-inbox/<id>.(png|jpg|webp) overrides the downloaded portrait.
Existing files are skipped unless --force is passed. The app never fetches art at runtime.

Usage:  python scripts/fetch_legend_art.py [--force]
Needs:  pip install -r scripts/requirements.txt
"""

from __future__ import annotations

import argparse
import io
import json
import sys
import time
from dataclasses import dataclass, field
from pathlib import Path

import requests
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LEGENDS_DIR = ROOT / "core" / "data" / "src" / "main" / "assets" / "legends"
CATALOG = LEGENDS_DIR / "legends.json"
INBOX = ROOT / "legend-art-inbox"
ATTRIBUTION = LEGENDS_DIR / "ATTRIBUTION.md"

DDRAGON = "https://ddragon.leagueoflegends.com"
VERSIONS_URL = f"{DDRAGON}/api/versions.json"
CHAMPIONS_URL = DDRAGON + "/cdn/{version}/data/en_US/champion.json"
PORTRAIT_URL = DDRAGON + "/cdn/img/champion/loading/{key}_0.jpg"
WIDE_URL = DDRAGON + "/cdn/img/champion/centered/{key}_0.jpg"

PORTRAIT_SIZE = 512
PORTRAIT_QUALITY = 82
# Share of the loading art width trimmed from each edge to drop its dark vignette frame.
LOADING_ART_INSET = 0.06
WIDE_WIDTH = 960
WIDE_QUALITY = 80
RETRIES = 3
REQUEST_DELAY_S = 0.2
INBOX_EXTENSIONS = ("png", "jpg", "jpeg", "webp")
USER_AGENT = "rift-tally-art-fetch/1.0"


@dataclass
class Row:
    legend_id: str
    name: str
    key: str | None = None
    portrait: str = "-"
    wide: str = "-"
    sources: list[str] = field(default_factory=list)


class Fetcher:
    def __init__(self) -> None:
        self.session = requests.Session()
        self.session.headers["User-Agent"] = USER_AGENT

    def get(self, url: str) -> bytes:
        last_error: Exception | None = None
        for attempt in range(1, RETRIES + 1):
            time.sleep(REQUEST_DELAY_S)
            try:
                response = self.session.get(url, timeout=30)
                response.raise_for_status()
                return response.content
            except requests.RequestException as error:
                last_error = error
                print(f"  attempt {attempt}/{RETRIES} failed for {url}: {error}", file=sys.stderr)
        raise RuntimeError(f"giving up on {url}") from last_error

    def get_json(self, url: str):
        return json.loads(self.get(url))


def normalize(name: str) -> str:
    return "".join(ch for ch in name.lower() if ch.isalnum())


def square_from_top(image: Image.Image, inset: int = 0) -> Image.Image:
    """Crops the largest square anchored at the top (faces sit near the top of loading art).

    [inset] trims that many pixels from every edge first (loading art has a dark vignette frame).
    """
    image = image.crop((inset, inset, image.width - inset, image.height - inset))
    side = min(image.width, image.height)
    left = (image.width - side) // 2
    return image.crop((left, 0, left + side, side))


def save_portrait(data: bytes, target: Path, inset_fraction: float = 0.0) -> None:
    image = Image.open(io.BytesIO(data)).convert("RGB")
    image = square_from_top(image, inset=round(image.width * inset_fraction))
    image = image.resize((PORTRAIT_SIZE, PORTRAIT_SIZE), Image.Resampling.LANCZOS)
    image.save(target, "WEBP", quality=PORTRAIT_QUALITY, method=6)


def save_wide(data: bytes, target: Path) -> None:
    image = Image.open(io.BytesIO(data)).convert("RGB")
    height = round(image.height * WIDE_WIDTH / image.width)
    image = image.resize((WIDE_WIDTH, height), Image.Resampling.LANCZOS)
    image.save(target, "WEBP", quality=WIDE_QUALITY, method=6)


def find_override(legend_id: str) -> Path | None:
    for ext in INBOX_EXTENSIONS:
        candidate = INBOX / f"{legend_id}.{ext}"
        if candidate.is_file():
            return candidate
    return None


def process(row: Row, fetcher: Fetcher, force: bool) -> None:
    portrait_path = LEGENDS_DIR / f"{row.legend_id}.webp"
    wide_path = LEGENDS_DIR / f"{row.legend_id}_wide.webp"

    override = find_override(row.legend_id)
    if override is not None:
        try:
            save_portrait(override.read_bytes(), portrait_path)
            row.portrait = "override"
            row.sources.append(f"portrait: developer override ({override.name})")
        except OSError as error:
            print(f"  override {override} unreadable: {error}", file=sys.stderr)
            row.portrait = "failed"
    elif row.key is None:
        row.portrait = "failed"
    elif portrait_path.exists() and not force:
        row.portrait = "skipped"
        row.sources.append(PORTRAIT_URL.format(key=row.key))
    else:
        url = PORTRAIT_URL.format(key=row.key)
        try:
            save_portrait(fetcher.get(url), portrait_path, LOADING_ART_INSET)
            row.portrait = "downloaded"
        except (RuntimeError, OSError) as error:
            print(f"  {row.name} portrait: {error}", file=sys.stderr)
            row.portrait = "failed"
        row.sources.append(url)

    if row.key is None:
        row.wide = "failed"
    elif wide_path.exists() and not force:
        row.wide = "skipped"
        row.sources.append(WIDE_URL.format(key=row.key))
    else:
        url = WIDE_URL.format(key=row.key)
        try:
            save_wide(fetcher.get(url), wide_path)
            row.wide = "downloaded"
        except (RuntimeError, OSError) as error:
            print(f"  {row.name} wide: {error}", file=sys.stderr)
            row.wide = "failed"
        row.sources.append(url)


def write_attribution(rows: list[Row], version: str) -> None:
    lines = [
        "# Legend art attribution",
        "",
        "Legend art is owned by Riot Games and comes from Riot's Data Dragon CDN",
        f"(version `{version}`), fetched at build time by `scripts/fetch_legend_art.py`.",
        "",
        "Rift Tally was created under Riot Games' \"Legal Jibber Jabber\" policy using assets owned by",
        "Riot Games. Riot Games does not endorse or sponsor this project.",
        "",
        "| Legend | Data Dragon key | Sources |",
        "|---|---|---|",
    ]
    for row in rows:
        sources = "<br>".join(row.sources) if row.sources else "none"
        lines.append(f"| {row.name} | {row.key or 'no match'} | {sources} |")
    ATTRIBUTION.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


def print_summary(rows: list[Row]) -> None:
    headers = ("Legend", "Key", "Portrait", "Wide")
    table = [(r.name, r.key or "NO MATCH", r.portrait, r.wide) for r in rows]
    widths = [max(len(h), *(len(t[i]) for t in table)) for i, h in enumerate(headers)]
    fmt = "  ".join(f"{{:<{w}}}" for w in widths)
    print(fmt.format(*headers))
    print(fmt.format(*("-" * w for w in widths)))
    for line in table:
        print(fmt.format(*line))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--force", action="store_true", help="re-download files that already exist")
    args = parser.parse_args()

    legends = json.loads(CATALOG.read_text(encoding="utf-8"))
    fetcher = Fetcher()
    version = fetcher.get_json(VERSIONS_URL)[0]
    champions = fetcher.get_json(CHAMPIONS_URL.format(version=version))["data"]
    keys_by_name = {normalize(champ["name"]): key for key, champ in champions.items()}
    print(f"Data Dragon {version}: {len(champions)} champions, {len(legends)} Legends\n")

    rows: list[Row] = []
    for entry in legends:
        row = Row(legend_id=entry["id"], name=entry["name"], key=keys_by_name.get(normalize(entry["name"])))
        if row.key is None:
            print(f"WARNING: no Data Dragon champion matches Legend '{row.name}'", file=sys.stderr)
        process(row, fetcher, args.force)
        rows.append(row)

    write_attribution(rows, version)
    print()
    print_summary(rows)
    failed = [r.name for r in rows if "failed" in (r.portrait, r.wide)]
    if failed:
        print(f"\nFailed: {', '.join(failed)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
