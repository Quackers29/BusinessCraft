#!/usr/bin/env python3
"""Launch a Minecraft dev command dumped by tools/mp-smoke-run.gradle.

The process image is replaced with java so the caller's pid stays the game.
"""

from __future__ import annotations

import argparse
import json
import os
import shlex
import sys


def replace_flag(argv: list[str], flag: str, value: str, add: bool) -> list[str]:
    out: list[str] = []
    replaced = False
    skip = False
    for arg in argv:
        if skip:
            skip = False
            continue
        if arg == flag:
            out.extend([flag, value])
            skip = True
            replaced = True
        else:
            out.append(arg)
    if add and not replaced:
        out.extend([flag, value])
    return out


def upsert_xmx(jvm_args: list[str], xmx: str | None) -> list[str]:
    if not xmx:
        return jvm_args
    flag = xmx if xmx.startswith("-Xmx") else f"-Xmx{xmx}"
    out = [arg for arg in jvm_args if not arg.startswith("-Xmx")]
    out.insert(0, flag)
    return out


def upsert_sysprops(jvm_args: list[str], props: dict[str, str]) -> list[str]:
    prefixes = {f"-D{key}=": value for key, value in props.items()}
    out: list[str] = []
    seen: set[str] = set()
    for arg in jvm_args:
        replaced = False
        for prefix, value in prefixes.items():
            if arg.startswith(prefix):
                out.append(prefix + value)
                seen.add(prefix)
                replaced = True
                break
        if not replaced:
            out.append(arg)
    for prefix, value in prefixes.items():
        if prefix not in seen:
            out.insert(0, prefix + value)
    return out


def strip_flag(argv: list[str], flag: str) -> list[str]:
    out: list[str] = []
    skip = False
    for arg in argv:
        if skip:
            skip = False
            continue
        if arg == flag:
            skip = True
            continue
        out.append(arg)
    return out


def classpath_argfile(working_dir: str, entries: list[str]) -> str:
    path = os.path.join(working_dir, "mp-smoke-classpath.args")
    os.makedirs(working_dir, exist_ok=True)
    # One argv element per line. The classpath itself is a single argument.
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("-cp\n")
        handle.write(":".join(entries))
        handle.write("\n")
    return "@" + path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("spec")
    parser.add_argument("--working-dir")
    parser.add_argument("--username")
    parser.add_argument("--uuid")
    parser.add_argument("--game-dir")
    parser.add_argument("--xmx")
    parser.add_argument("--sysprop", action="append", default=[])
    parser.add_argument("--append-arg", action="append", default=[])
    args = parser.parse_args()

    with open(args.spec, encoding="utf-8") as handle:
        spec = json.load(handle)

    working = os.path.abspath(args.working_dir or spec["workingDir"])
    os.makedirs(working, exist_ok=True)
    program = [str(part) for part in spec.get("args", [])]
    if args.username:
        program = replace_flag(program, "--username", args.username, True)
        if "--accessToken" not in program:
            program.extend(["--accessToken", "0", "--userType", "msa"])
    if args.uuid:
        program = replace_flag(program, "--uuid", args.uuid, True)
    if args.game_dir:
        program = replace_flag(program, "--gameDir", os.path.abspath(args.game_dir), True)
    # A dev init script pins Forge runClient to a singleplayer world. This smoke joins a server.
    program = strip_flag(program, "--quickPlaySingleplayer")
    program.extend(args.append_arg)

    jvm = [str(part) for part in spec.get("jvmArgs", [])]
    jvm = upsert_xmx(jvm, args.xmx)
    props = {str(key): str(value) for key, value in spec.get("systemProperties", {}).items()}
    for raw in args.sysprop:
        key, value = raw.split("=", 1)
        props[key] = value
    jvm = upsert_sysprops(jvm, props)

    classpath = [str(entry) for entry in spec.get("classpath", []) if entry]
    if classpath and not any(part.startswith("@") or part == "-p" or part == "--module-path" for part in jvm):
        jvm.append(classpath_argfile(working, classpath))

    command = [spec["executable"], *jvm, spec["mainClass"], *program]
    for key, value in spec.get("environment", {}).items():
        os.environ[str(key)] = str(value)
    os.chdir(working)
    if os.environ.get("MP_SMOKE_PRINT_COMMAND") == "1":
        print(" ".join(shlex.quote(part) for part in command))
        return
    os.execv(command[0], command)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"mp-smoke-launch failed: {error}", file=sys.stderr)
        sys.exit(1)
