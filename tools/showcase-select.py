#!/usr/bin/env python3
"""Choose which showcase runs a change needs.

Reads tools/showcase-targets.json and a list of changed paths, then prints one
showcase.sh run per line:  "<mode> [extra...]"

  routes town/map,platform      all-views limited to those routes (group = prefix)
  all-views | tourists | jade | block | town | platform | tourist-screen | production-tooltip
  testbed trade-contract

Changed paths come from --files, or from git: everything that differs from --base
(default origin/main), committed or not, plus untracked files.

  python3 tools/showcase-select.py                     # plan for this checkout
  python3 tools/showcase-select.py --files a.java b.java
  python3 tools/showcase-select.py --regression        # the full set, ignores the diff
  python3 tools/showcase-select.py --self-test
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG = ROOT / "tools" / "showcase-targets.json"
REGRESSION = ["all", "town", "block", "all-views", "two-towns", "testbed"]
MODES = {"all", "town", "block", "platform", "kiosk", "lamp", "all-views", "two-towns", "testbed",
         "tourists", "jade", "tourist-screen", "production-tooltip"}


def load_config(path: Path = CONFIG) -> dict:
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def changed_files(base: str) -> list[str]:
    def git(*args: str) -> list[str]:
        out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
        if out.returncode != 0:
            raise SystemExit(f"git {' '.join(args)} failed: {out.stderr.strip()}")
        return [line for line in out.stdout.splitlines() if line]

    return sorted(set(git("diff", "--name-only", base) + git("ls-files", "--others", "--exclude-standard")))


def plan(files: list[str], config: dict) -> tuple[list[str], list[str]]:
    """Returns (run lines, unmapped paths). A run line starting with 'full' means regression."""
    ignore = [re.compile(p) for p in config["ignore"]]
    rules = [(re.compile(r["match"]), r["targets"]) for r in config["rules"]]
    targets: list[str] = []
    unmapped: list[str] = []
    for path in files:
        hit = False
        for pattern, rule_targets in rules:
            if pattern.search(path):
                hit = True
                for target in rule_targets:
                    if target not in targets:
                        targets.append(target)
        if not hit and not any(p.search(path) for p in ignore):
            unmapped.append(path)
    if unmapped and config["sanity"] not in targets:
        targets.append(config["sanity"])
    return render(targets), unmapped


def render(targets: list[str]) -> list[str]:
    if "full" in targets:
        return ["full"]
    routes: list[str] = []
    modes: list[str] = []
    for target in targets:
        kind, _, value = target.partition(":")
        if kind == "route":
            if value not in routes:
                routes.append(value)
        elif kind == "mode":
            if value not in modes:
                modes.append(value)
        else:
            raise SystemExit(f"bad target {target!r}")
    if "all-views" in modes:
        routes = []  # every route is already in the full UI walk
    lines: list[str] = []
    if routes:
        lines.append("routes " + ",".join(routes))
    lines.extend(modes)
    return lines


def validate(config: dict) -> list[str]:
    problems: list[str] = []
    for rule in config["rules"]:
        re.compile(rule["match"])
        for target in rule["targets"]:
            kind, _, value = target.partition(":")
            if kind == "mode":
                if value.split()[0] not in MODES:
                    problems.append(f"unknown mode in {target!r}")
            elif kind != "route" and target != "full":
                problems.append(f"bad target {target!r}")
    kind, _, value = config["sanity"].partition(":")
    if kind != "mode" or value.split()[0] not in MODES:
        problems.append("sanity must be a mode target")
    return problems


def self_test() -> None:
    config = load_config()
    problems = validate(config)
    assert not problems, problems

    def check(files, expect, unmapped=()):
        got, miss = plan(files, config)
        assert got == expect, (files, got, expect)
        assert tuple(miss) == tuple(unmapped), (files, miss)

    base = "common/src/main/java/com/quackers29/businesscraft/"
    check([], [])
    check(["README.md", "vault/UI/Screen Style.md", "common/src/test/java/com/quackers29/businesscraft/A.java"], [])
    check([base + "ui/screens/town/PaymentBoardScreen.java"], ["routes town/payment"])
    check([base + "ui/screens/platform/PlatformInterfaceScreen.java"], ["routes platform", "platform"])
    check([base + "ui/components/BCButton.java"], ["all-views"])
    check([base + "client/showcase/ShowcaseAllViews.java"], ["full"])
    check([base + "entity/TouristEntity.java"], ["tourists"])
    check([base + "economy/Whatever.java", base + "ui/screens/town/ContractBoardScreen.java"],
          ["routes town/trade,town/contracts,town/contract-detail", "testbed trade-contract"])
    check([base + "util/Mystery.java"], ["town"], [base + "util/Mystery.java"])
    print("showcase-select self-test passed")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", default="origin/main")
    parser.add_argument("--files", nargs="*")
    parser.add_argument("--regression", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    config = load_config()
    problems = validate(config)
    if problems:
        raise SystemExit("showcase-targets.json: " + "; ".join(problems))
    if args.regression:
        print("\n".join(REGRESSION))
        return
    files = args.files if args.files is not None else changed_files(args.base)
    lines, unmapped = plan(files, config)
    for path in unmapped:
        print(f"# unmapped: {path}", file=sys.stderr)
    if lines == ["full"]:
        print("# harness or shared tooling changed: running the regression set", file=sys.stderr)
        lines = REGRESSION
    print("\n".join(lines))


if __name__ == "__main__":
    main()
