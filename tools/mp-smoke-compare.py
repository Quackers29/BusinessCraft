#!/usr/bin/env python3
"""Compare two multiplayer smoke observation files.

Exits 0 when both clients saw the seeded scene and agree with each other, and the
client that ran the reach stage (BC_Beta) shows its out-of-reach platform packets changed nothing.
"""

from __future__ import annotations

import copy
import difflib
import json
import sys

FLOAT_TOLERANCE = 0.01
EXPECTED_TOWNS = {"Oakvale", "Hillcrest", "Rivertown"}
EXPECTED_LEARNED = {"Hillcrest", "Rivertown"}
EXPECTED_ENABLED = ["Hillcrest"]
EXPECTED_NAMES = {"Morgan", "Eleanor"}
EXPECTED_LINES = {"Oakvale -> Hillcrest", "Oakvale -> Rivertown"}
EXPECTED_SCREEN = "PlatformInterfaceScreen"
MIN_REACH_DISTANCE = 100
EXPECTED_SHOTS = ["mp_nametags", "mp_track", "mp_platform"]


def load(path: str) -> dict:
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def required_issues(doc: dict, label: str) -> list[str]:
    issues: list[str] = []
    if not doc.get("ok"):
        issues.append(f"{label}: observation ok is not true ({doc.get('failure', '')})")
    if doc.get("holdingClipboard") is not True:
        issues.append(f"{label}: conductor clipboard was not held")
    towns = doc.get("towns") or []
    names = {town.get("name") for town in towns}
    if names != EXPECTED_TOWNS:
        issues.append(f"{label}: towns {sorted(names)} != {sorted(EXPECTED_TOWNS)}")
    platform = doc.get("platform") or {}
    if platform.get("mode") != "auto":
        issues.append(f"{label}: platform mode is {platform.get('mode')!r}, expected auto")
    if platform.get("enabled") is not True:
        issues.append(f"{label}: platform is not enabled")
    enabled = list(platform.get("enabledTowns") or [])
    if enabled != EXPECTED_ENABLED:
        issues.append(f"{label}: enabled towns {enabled} != {EXPECTED_ENABLED}")
    learned = {route.get("name") for route in platform.get("learnedRoutes") or []}
    if not EXPECTED_LEARNED.issubset(learned):
        issues.append(f"{label}: learned routes {sorted(learned)} missing {sorted(EXPECTED_LEARNED)}")
    tourists = doc.get("tourists") or []
    seen_names = {tourist.get("nameTag") for tourist in tourists}
    if not EXPECTED_NAMES.issubset(seen_names):
        issues.append(f"{label}: name tags {sorted(seen_names)} missing {sorted(EXPECTED_NAMES)}")
    lines = {tourist.get("clipboardLine") for tourist in tourists}
    if not EXPECTED_LINES.issubset(lines):
        issues.append(f"{label}: clipboard lines {sorted(lines)} missing {sorted(EXPECTED_LINES)}")
    ui = doc.get("ui") or {}
    if ui.get("screen") != EXPECTED_SCREEN:
        issues.append(f"{label}: screen is {ui.get('screen')!r}, expected {EXPECTED_SCREEN}")
    shots = {row.get("name"): row for row in doc.get("shots") or []}
    for name in EXPECTED_SHOTS:
        row = shots.get(name)
        if row is None or not row.get("ok"):
            issues.append(f"{label}: shot {name} was not saved")
    return issues


def reach_issues(doc: dict, label: str) -> list[str]:
    reach = doc.get("reach")
    if not isinstance(reach, dict):
        return [f"{label}: no reach stage result"]
    issues: list[str] = []
    if reach.get("sent") is not True:
        issues.append(f"{label}: reach stage never sent its packets")
    if (reach.get("distance") or 0) < MIN_REACH_DISTANCE:
        issues.append(f"{label}: reach packets were sent from {reach.get('distance')} blocks, "
                      f"expected at least {MIN_REACH_DISTANCE}")
    if reach.get("stateUnchanged") is not True:
        issues.append(f"{label}: an out-of-reach platform packet changed the platform")
    return issues


def comparable(doc: dict) -> dict:
    body = copy.deepcopy(doc)
    body.pop("viewer", None)
    body.pop("reach", None)
    body.pop("failure", None)
    return body


def differences(left, right, path: str = "") -> list[str]:
    found: list[str] = []
    if isinstance(left, float) or isinstance(right, float):
        try:
            if abs(float(left) - float(right)) > FLOAT_TOLERANCE:
                found.append(f"{path}: {left} != {right}")
        except (TypeError, ValueError):
            found.append(f"{path}: {left!r} != {right!r}")
        return found
    if isinstance(left, dict) and isinstance(right, dict):
        keys = sorted(set(left) | set(right))
        for key in keys:
            child = f"{path}.{key}" if path else str(key)
            if key not in left or key not in right:
                found.append(f"{child}: {left.get(key)!r} != {right.get(key)!r}")
            else:
                found.extend(differences(left[key], right[key], child))
        return found
    if isinstance(left, list) and isinstance(right, list):
        if len(left) != len(right):
            found.append(f"{path}: length {len(left)} != {len(right)}")
        for index, (item_left, item_right) in enumerate(zip(left, right)):
            found.extend(differences(item_left, item_right, f"{path}[{index}]"))
        return found
    if left != right:
        found.append(f"{path}: {left!r} != {right!r}")
    return found


def unified(left: dict, right: dict, left_name: str, right_name: str) -> str:
    left_text = json.dumps(comparable(left), indent=2, sort_keys=True).splitlines(keepends=True)
    right_text = json.dumps(comparable(right), indent=2, sort_keys=True).splitlines(keepends=True)
    return "".join(difflib.unified_diff(left_text, right_text, left_name, right_name))


def evaluate(left: dict, right: dict, left_name: str = "alpha", right_name: str = "beta") -> tuple[list[str], str]:
    """The right-hand document is the one that ran the reach stage."""
    issues = required_issues(left, left_name) + required_issues(right, right_name)
    issues.extend(reach_issues(right, right_name))
    issues.extend(differences(comparable(left), comparable(right)))
    return issues, unified(left, right, left_name, right_name)


def sample(clipboard: str, strength: float = 0.625, reach: bool = False) -> dict:
    doc = {
        "ok": True,
        "failure": "",
        "viewer": "BC_Alpha",
        "holdingClipboard": True,
        "towns": [
            {"name": "Hillcrest", "x": 0, "y": 5, "z": 120, "population": 5, "touristCount": 0},
            {"name": "Oakvale", "x": 0, "y": 5, "z": 0, "population": 5, "touristCount": 0},
            {"name": "Rivertown", "x": 0, "y": 5, "z": 240, "population": 5, "touristCount": 0},
        ],
        "platform": {
            "name": "Platform 1",
            "pos": {"x": 4, "y": 5, "z": 0},
            "mode": "auto",
            "enabled": True,
            "enabledTowns": ["Hillcrest"],
            "towns": [
                {"name": "Hillcrest", "enabled": True, "distance": 120, "direction": "S"},
                {"name": "Rivertown", "enabled": False, "distance": 240, "direction": "S"},
            ],
            "learnedRoutes": [
                {"name": "Hillcrest", "strength": strength},
                {"name": "Rivertown", "strength": 0.375},
            ],
        },
        "tourists": [
            {"nameTag": "Eleanor", "clipboardLine": "Oakvale -> Rivertown"},
            {"nameTag": "Morgan", "clipboardLine": clipboard},
        ],
        "ui": {
            "screen": "PlatformInterfaceScreen",
            "tab": "",
            "buttonIds": [],
            "scaledWidth": 960,
            "scaledHeight": 540,
        },
        "shots": [
            {"name": name, "file": f"showcase_{name}.png", "ok": True}
            for name in ("mp_nametags", "mp_track", "mp_platform")
        ],
    }
    if reach:
        doc["reach"] = {"sent": True, "distance": 120, "packets": ["SetPlatformEnabledPacket"],
                        "stateUnchanged": True}
    return doc


def self_test() -> None:
    agree, diff = evaluate(sample("Oakvale -> Hillcrest"), sample("Oakvale -> Hillcrest", 0.626, True))
    if agree:
        raise SystemExit("self-test: identical observations were reported as different: " + "; ".join(agree))
    if "strength" in diff and "-  0.625" in diff:
        # Tolerance keeps 0.625 and 0.626 as the same, so the unified text may still differ.
        # The issue list is what fails the run. A textual diff alone must not fail.
        pass
    left = sample("Oakvale -> Hillcrest")
    right = sample("Oakvale -> Somewhere", reach=True)
    disagree, diff = evaluate(left, right)
    if not disagree:
        raise SystemExit("self-test: clipboard disagreement was accepted")
    if not any("clipboard" in issue or "clipboardLine" in issue for issue in disagree):
        raise SystemExit("self-test: diff did not mention the clipboard: " + "; ".join(disagree))
    if "Oakvale -> Somewhere" not in diff:
        raise SystemExit("self-test: unified diff missing the changed line")
    empty = sample("Oakvale -> Hillcrest")
    empty["towns"] = []
    missing, _ = evaluate(empty, empty)
    if not any("towns" in issue for issue in missing):
        raise SystemExit("self-test: two empty town lists were accepted")
    wrong_screen = sample("Oakvale -> Hillcrest")
    wrong_screen["ui"]["screen"] = "TitleScreen"
    screen_issues, _ = evaluate(wrong_screen, wrong_screen)
    if not any("screen" in issue for issue in screen_issues):
        raise SystemExit("self-test: wrong screen was accepted")
    clean = evaluate(sample("Oakvale -> Hillcrest"), sample("Oakvale -> Hillcrest", reach=True))[0]
    if clean:
        raise SystemExit("self-test: matching observations were rejected: " + "; ".join(clean))
    missing_reach = evaluate(sample("Oakvale -> Hillcrest"), sample("Oakvale -> Hillcrest"))[0]
    if not any("reach" in issue for issue in missing_reach):
        raise SystemExit("self-test: a missing reach stage was accepted")
    applied = sample("Oakvale -> Hillcrest", reach=True)
    applied["reach"]["stateUnchanged"] = False
    if not any("changed the platform" in issue for issue in evaluate(sample("Oakvale -> Hillcrest"), applied)[0]):
        raise SystemExit("self-test: an applied out-of-reach packet was accepted")
    near = sample("Oakvale -> Hillcrest", reach=True)
    near["reach"]["distance"] = 5
    if not any("blocks" in issue for issue in evaluate(sample("Oakvale -> Hillcrest"), near)[0]):
        raise SystemExit("self-test: a reach stage from 5 blocks was accepted")
    print("mp-smoke compare self-test passed")


def main() -> None:
    if len(sys.argv) == 2 and sys.argv[1] == "--self-test":
        self_test()
        return
    if len(sys.argv) != 3:
        print("usage: mp-smoke-compare.py alpha.json beta.json", file=sys.stderr)
        sys.exit(2)
    issues, diff = evaluate(load(sys.argv[1]), load(sys.argv[2]), "BC_Alpha", "BC_Beta")
    if diff.strip():
        print(diff, end="" if diff.endswith("\n") else "\n")
    if issues:
        print("Multiplayer smoke disagreement:", file=sys.stderr)
        for issue in issues:
            print(f"  {issue}", file=sys.stderr)
        sys.exit(1)
    print("Multiplayer smoke observations agree")


if __name__ == "__main__":
    main()
