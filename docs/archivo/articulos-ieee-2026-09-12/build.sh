#!/usr/bin/env bash
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build"
mkdir -p "$BUILD"

if ! command -v pdflatex >/dev/null 2>&1; then
  echo "ERROR: pdflatex no encontrado."
  echo "Instala TeX Live en Linux Mint/Ubuntu con:"
  echo "  sudo apt update && sudo apt install -y texlive-latex-recommended texlive-latex-extra texlive-fonts-recommended latexmk"
  echo ""
  echo "Alternativas sin instalar:"
  echo "  1) Overleaf: sube cada .tex (clase IEEEtran ya incluida)"
  echo "  2) Docker: docker run --rm -v \"$ROOT:/work\" -w /work texlive/texlive:latest bash -c 'for f in *.tex; do latexmk -pdf -outdir=build \"\$f\"; done'"
  exit 1
fi

export TEXINPUTS="$ROOT:$BUILD:${TEXINPUTS:-}"
for tex in "$ROOT"/*.tex; do
  base="$(basename "$tex" .tex)"
  echo "== Building $base =="
  pdflatex -interaction=nonstopmode -output-directory="$BUILD" "$tex" || true
  bibtex "$BUILD/$base" 2>/dev/null || true
  pdflatex -interaction=nonstopmode -output-directory="$BUILD" "$tex" || true
  pdflatex -interaction=nonstopmode -output-directory="$BUILD" "$tex" || true
done
# Portada y contraportada (cover/)
for tex in "$ROOT"/cover/*.tex; do
  [ -e "$tex" ] || continue
  base="$(basename "$tex" .tex)"
  echo "== Building cover/$base =="
  pdflatex -interaction=nonstopmode -output-directory="$BUILD" "$tex" || true
  pdflatex -interaction=nonstopmode -output-directory="$BUILD" "$tex" || true
done
echo "Done. PDFs in $BUILD/"
ls -lh "$BUILD"/*.pdf 2>/dev/null || echo "No PDFs generados (revisa log en $BUILD/*.log)"
