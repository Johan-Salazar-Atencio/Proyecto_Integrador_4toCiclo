"""Genera los anexos de codigo de la bitacora: un archivo Markdown por commit.

Cada anexo trae, sacado directamente de git (nada escrito a mano):
  - datos del commit (hash, autor, fecha, mensaje completo);
  - tabla de archivos con lineas anadidas/quitadas y tipo de cambio;
  - por cada archivo, las UBICACIONES exactas (archivo:linea en la version nueva,
    con la funcion o clase donde cae el cambio) y el diff completo.

Uso (desde la raiz del repositorio):
    python docs/bitacora/generar_anexos.py

Para anadir commits futuros a la bitacora, agregar su rango en RANGOS y volver a ejecutar:
los anexos existentes se regeneran igual (el resultado es determinista).
"""
from __future__ import annotations

import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SALIDA = Path(__file__).resolve().parent / "anexos"

# (descripcion, argumentos de git log) -> que commits entran en la bitacora
RANGOS = [
    ("Correcciones y funciones de jose desde el 10-nov", ["--no-merges", "--author=jose", "--since=2026-11-10", "main"]),
    ("Rediseno de Johan (rama-de-johan) integrado en 0200c35", ["--no-merges", "e1f499b..0200c35^2"]),
]

BINARIOS = (".png", ".jpg", ".jpeg", ".webp", ".gif", ".ico", ".jar", ".zip", ".tgz")
HUNK = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@ ?(.*)$")


def git(*args: str) -> str:
    r = subprocess.run(["git", *args], cwd=RAIZ, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        sys.exit(f"git {' '.join(args)} fallo: {r.stderr}")
    return r.stdout


def commits() -> list[str]:
    vistos: dict[str, None] = {}
    for _, args in RANGOS:
        for h in git("log", "--format=%H", *args).split():
            vistos[h] = None
    # orden cronologico (del mas antiguo al mas nuevo)
    con_fecha = [(git("show", "-s", "--format=%ct", h).strip(), h) for h in vistos]
    return [h for _, h in sorted(con_fecha)]


def area(ruta: str) -> str:
    for prefijo, nombre in (
        ("backend-spring-boot/src/test", "Spring (tests)"),
        ("backend-spring-boot", "Spring Boot"),
        ("backend-django", "Django / IA"),
        ("frontend-react/src/pages/admin", "Web (panel admin)"),
        ("frontend-react", "Web (React)"),
        ("mobile-kotlin/app/src/test", "Movil (tests)"),
        ("mobile-kotlin", "Movil (Kotlin)"),
        ("db/migration", "Base de datos (Flyway)"),
        (".github", "CI"),
    ):
        if ruta.startswith(prefijo):
            return nombre
    return "Documentacion / raiz"


def lenguaje(ruta: str) -> str:
    ext = ruta.rsplit(".", 1)[-1].lower() if "." in ruta else ""
    return {
        "java": "Java", "kt": "Kotlin", "py": "Python", "jsx": "React JSX", "js": "JavaScript",
        "css": "CSS", "sql": "SQL (PostgreSQL)", "md": "Markdown", "yml": "YAML", "yaml": "YAML",
        "xml": "XML", "json": "JSON", "txt": "Texto", "html": "HTML",
    }.get(ext, ext.upper() or "—")


def partir_diff(patch: str) -> list[tuple[str, str]]:
    """Devuelve [(ruta_nueva, texto_del_diff_de_ese_archivo)]."""
    bloques = re.split(r"(?m)^(?=diff --git )", patch)
    salida = []
    for b in bloques:
        if not b.startswith("diff --git"):
            continue
        m = re.match(r"diff --git a/(.+?) b/(.+)", b.splitlines()[0])
        salida.append((m.group(2) if m else "?", b.rstrip("\n")))
    return salida


def ubicaciones(diff: str) -> list[str]:
    filas = []
    for linea in diff.splitlines():
        m = HUNK.match(linea)
        if not m:
            continue
        ini_viejo, n_viejo, ini, n, ctx = m.groups()
        n = int(n) if n is not None else 1
        n_viejo = int(n_viejo) if n_viejo is not None else 1
        ini = int(ini)
        rango_nuevo = f"L{ini}" if n <= 1 else f"L{ini}-L{ini + n - 1}"
        if n == 0:
            rango_nuevo = f"(borrado; antes L{ini_viejo}-L{int(ini_viejo) + n_viejo - 1})"
        ctx = ctx.strip()
        filas.append(f"{rango_nuevo}" + (f" — dentro de `{ctx[:110]}`" if ctx else ""))
    return filas


def tipo_cambio(diff: str) -> str:
    if "\nnew file mode" in diff:
        return "nuevo"
    if "\ndeleted file mode" in diff:
        return "borrado"
    if "\nrename from" in diff:
        return "renombrado"
    return "modificado"


def anexo(h: str) -> tuple[str, str, dict]:
    corto = h[:7]
    autor, fecha, asunto = git("show", "-s", "--format=%an%x1f%ad%x1f%s", "--date=format:%Y-%m-%d %H:%M", h).split("\x1f")
    cuerpo = git("show", "-s", "--format=%b", h).strip()
    cuerpo = "\n".join(l for l in cuerpo.splitlines() if not l.lower().startswith("co-authored-by"))
    numstat = [l.split("\t") for l in git("show", "--numstat", "--format=", "-M", h).splitlines() if l.strip()]
    patch = git("show", "--format=", "--patch", "-M", "--no-color", "-U3", h)
    diffs = dict(partir_diff(patch))

    total_mas = sum(int(a) for a, _, _ in numstat if a.isdigit())
    total_menos = sum(int(b) for _, b, _ in numstat if b.isdigit())
    areas = Counter(area(r.split(" => ")[-1].strip("{}")) for _, _, r in numstat)

    md = [f"# Anexo `{corto}` — {asunto.strip()}", ""]
    md += ["| Campo | Valor |", "|---|---|",
           f"| Commit | `{h}` |", f"| Autor | {autor} |", f"| Fecha | {fecha} |",
           f"| Áreas | {', '.join(f'{a} ({n})' for a, n in areas.most_common())} |",
           f"| Tamaño | {len(numstat)} archivos, +{total_mas} / −{total_menos} líneas |", ""]
    md += ["## Mensaje del commit", "", "```text", asunto.strip()]
    if cuerpo:
        md += ["", cuerpo]
    md += ["```", ""]

    md += ["## Archivos tocados", "", "| # | Archivo | Tipo | Lenguaje | + | − |", "|---|---|---|---|---|---|"]
    for i, (a, b, ruta) in enumerate(numstat, 1):
        destino = ruta.split(" => ")[-1].replace("{", "").replace("}", "")
        d = diffs.get(destino, "") or next((v for k, v in diffs.items() if k.endswith(destino.split("/")[-1])), "")
        md.append(f"| {i} | `{ruta}` | {tipo_cambio(d) if d else 'binario'} | {lenguaje(destino)} | {a} | {b} |")
    md.append("")

    md += ["## Cambios por archivo", ""]
    for ruta, d in diffs.items():
        md += [f"### `{ruta}`", ""]
        md += [f"- **Área:** {area(ruta)} · **Lenguaje:** {lenguaje(ruta)} · **Tipo:** {tipo_cambio(d)}"]
        if ruta.lower().endswith(BINARIOS) or "Binary files" in d:
            md += ["- Archivo binario (imagen u otro): git no muestra su contenido.", ""]
            continue
        ubic = ubicaciones(d)
        if ubic:
            md += ["- **Ubicaciones** (números de línea en la versión nueva del archivo):"]
            md += [f"  - `{ruta}:{u.split(' ')[0]}`" + (u[len(u.split(' ')[0]):] if ' ' in u else "") for u in ubic]
        md += ["", "````diff", d, "````", ""]

    nombre = f"{fecha[:10]}-{corto}.md"
    meta = {"hash": corto, "fecha": fecha, "autor": autor, "asunto": asunto.strip(), "archivos": len(numstat),
            "mas": total_mas, "menos": total_menos, "nombre": nombre}
    return nombre, "\n".join(md) + "\n", meta


def main() -> None:
    SALIDA.mkdir(parents=True, exist_ok=True)
    metas = []
    for h in commits():
        nombre, texto, meta = anexo(h)
        (SALIDA / nombre).write_text(texto, encoding="utf-8")
        metas.append(meta)

    idx = ["# Índice de anexos de código", "",
           "Generado por `docs/bitacora/generar_anexos.py` a partir de git. Un anexo por commit, en orden cronológico.",
           "Cada anexo contiene el diff completo y las ubicaciones `archivo:línea` de cada cambio.", "",
           "| # | Fecha | Commit | Autor | Asunto | Archivos | + | − |", "|---|---|---|---|---|---|---|---|"]
    for i, m in enumerate(metas, 1):
        idx.append(f"| {i} | {m['fecha']} | [`{m['hash']}`]({m['nombre']}) | {m['autor']} | {m['asunto']} | "
                   f"{m['archivos']} | {m['mas']} | {m['menos']} |")
    (SALIDA / "README.md").write_text("\n".join(idx) + "\n", encoding="utf-8")
    print(f"{len(metas)} anexos generados en {SALIDA}")


if __name__ == "__main__":
    main()
