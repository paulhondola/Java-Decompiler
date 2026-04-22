import argparse
import sys

from archlens.config import DecompileConfig
from archlens.router import route


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="decompile",
        description="Generate UML diagrams from a .jar or .dll file.",
    )
    p.add_argument("input", metavar="FILE", help=".jar or .dll file to analyse")
    p.add_argument(
        "--format",
        required=True,
        help="Output format, case-insensitive (e.g. plantuml, yuml).",
    )
    p.add_argument(
        "--ignore",
        metavar="PATTERN",
        action="append",
        default=[],
        help="Class name pattern to exclude (repeatable).",
    )
    p.add_argument("--output", metavar="FILE", default=None, help="Write output to FILE.")
    return p


def main(argv: list[str] | None = None) -> None:
    args = build_parser().parse_args(argv)
    config = DecompileConfig(
        format=args.format,
        ignore=args.ignore,
        output=args.output,
    )
    result = route(args.input, config)
    if args.output is None:
        sys.stdout.write(result)


if __name__ == "__main__":
    main()
