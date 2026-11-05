#!/usr/bin/env python3
"""Check the exact bundled model, voice and phonemization assets without Gradle."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]


def verify():
    asset_root=ROOT/'app/src/main/assets'
    manifest=json.loads((ROOT/'docs/assets.sha256.json').read_text())
    actual={str(p.relative_to(asset_root)) for p in asset_root.rglob('*') if p.is_file()}
    if actual!=set(manifest):
        raise ValueError(f'Asset inventory differs: {actual ^ set(manifest)}')
    for name,expected in manifest.items():
        if hashlib.sha256((asset_root/name).read_bytes()).hexdigest()!=expected:
            raise ValueError(f'Asset checksum mismatch: {name}')
    print(f'OK: {len(manifest)} bundled assets match SHA-256 checksums.')
    return manifest


if __name__=='__main__':verify()
