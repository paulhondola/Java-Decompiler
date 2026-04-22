from dataclasses import dataclass, field


@dataclass
class DecompileConfig:
    format: str  # e.g. "PlantUml", "Yuml", "Mermaid" — matches formatter class-name prefix
    ignore: list[str] = field(default_factory=list)
    output: str | None = None
