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
        choices=["plantuml", "yuml"],
        help="Output format.",
    )
    p.add_argument(
        "--ignore",
        metavar="PATTERN",
        action="append",
        default=[],
        help="Class name pattern to exclude (repeatable).",
    )
    p.add_argument("--output", metavar="FILE", default=None, help="Write output to FILE.")
    p.add_argument(
        "--yuml-mode",
        choices=["SIMPLE", "CLASSES"],
        default="SIMPLE",
        dest="yuml_mode",
        help="yUML rendering mode (only with --format yuml). Default: SIMPLE.",
    )
    return p


def main(argv: list[str] | None = None) -> None:
    args = build_parser().parse_args(argv)
    config = DecompileConfig(
        format=args.format,
        ignore=args.ignore,
        output=args.output,
        yuml_mode=args.yuml_mode,
    )
    result = route(args.input, config)
    if args.output is None:
        sys.stdout.write(result)


if __name__ == "__main__":
    main()
