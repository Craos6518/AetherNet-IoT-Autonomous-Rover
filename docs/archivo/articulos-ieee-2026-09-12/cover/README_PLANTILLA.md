# Recomendación de plantilla IEEE (de las 17 listadas)

**Recomendación principal: #15 IEEE Conference Template**
- **URL:** https://es.overleaf.com/latex/templates/ieee-conference-template/grfzhhncsfqn
- **Autor:** IEEE (oficial)
- **Por qué:** Es la plantilla *starter* oficial para artículos de conferencias IEEE en LaTeX, doble columna, clase `IEEEtran` 1.8b, con ejemplo completo (título, abstract, keywords, secciones, tablas, figuras, bibliografía). Es exactamente el formato que esperan los evaluadores UTP para un Proyecto Integrador entregado como "artículo IEEE". Es compatible 100% con nuestro `IEEEtran.cls` vendorado y con las opciones `conference, compsoc, a4paper, 10pt` que ya usamos.

**Alternativa mínima equivalente: #11 IEEE Bare Demo Template for Conferences**
- **URL:** https://es.overleaf.com/latex/templates/ieee-bare-demo-template-for-conferences/ypypvwjmvtdf
- **Autor:** Michael Shell
- **Por qué:** Es el esqueleto bare-bones de #15, sin contenido de ejemplo, ideal si quieres partir de cero y mantener control total. Nuestros 7 `.tex` ya están basados en esta variante (bare conference, compsoc, doble columna). Migrar a #15 implica solo copiar el preámbulo completo del template (incluye mejoras de peer review).

**No recomendadas para este proyecto:**
- #1, #8, #14, #16 (journal) → formato de revista (una o dos columnas journal, no conference).
- #5 (one-column journal) → no es doble columna conference.
- #2, #6, #10 (Trans. Magnetics / MTT / SOCC) → journals/conferencias muy específicas.
- #7 (ITherm), #12 (CS Conferences), #13 (Magazine), #17 (OJIES) → variantes de nicho; #12 es cercano pero #15 es más genérico y reconocido.

**Acción para aplicar #15:**
1. En Overleaf, "Open as template" en #15.
2. Copiar tu contenido (abstract, secciones, tablas, `thebibliography`) dentro del `main.tex` del template.
3. Mantener `\documentclass[conference,compsoc,a4paper]{IEEEtran}` y `\usepackage[english]{babel}` + `\usepackage{cite,amsmath,booktabs,hyperref}` como en este repo. El `IEEEtran.cls` local ya es 1.8b idéntico al de Overleaf.
4. Si prefieres no migrar: **quédate con #11** — es 95% idéntico a #15 y ya está validado en `build/` (7 PDFs sin errores).
