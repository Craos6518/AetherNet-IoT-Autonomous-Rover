# `.gitignore` — Mapa de Ignorados AetherNet

> **Autor:** Andres Felipe Martinez Henao
> **Experiencia:** 2 años electrónica y Arduino | 1 año Programación C | 2 años Python | 2 años HTML/CSS/JavaScript/React | 1 año PostgreSQL  
> **Stack:** Git (glob `fnmatch` como `.gitignore`), Python 3.12, Android Gradle, C++ Arduino, Node-RED, Docker — 100% FOSS (RNF-3.1)  
> **Archivo:** `.gitignore:1` (333 líneas, 13 bloques, 26 `!` whitelists) — ver `git check-ignore -v` para debug

---

## 0. Cómo leer este `.gitignore` si vienes de web / electrónica / Python

Si vienes de **React (2 años)**: este `.gitignore` es el `package.json` de `files: ["!dist"]` pero para Git — `__pycache__/` es `node_modules/.cache`, `build/` es `dist/` (72M `app/build`), `*.log` es `npm-debug.log`, y `!gradlew` es whitelist como `!dist/keep.json` en npm.

Si vienes de **electrónica / C (1 año C, 2 años Arduino)**: `*.o`, `*.elf`, `*.hex`, `.pio/` son artefactos de `gcc`/`arduino-cli`/`PlatformIO` (como `*.o` en `avr-gcc`), nunca se commitean. `firmware/**/secrets.h` es `WIFI_SSID "FELIPE."` real — se ignora y CI usa `secrets.h.example` fallback `ci.yml:101` (como `.env` en React).

Si vienes de **Python / PostgreSQL (2 años Python, 1 año PG)**: `.venv/`, `__pycache__/`, `.coverage`, `postgres_data/` son entornos/datos locales. `stats/data/*` con `!stats/data/ema_demo.json` es whitelist para trazabilidad KPI `89.3%` `prd.md:51` sin `git add -f`.

Este doc cubre **todo** lo que hay en `.gitignore:1` línea por línea con `ruta:línea` y analogías.

---

## 1. Mapa de 13 Bloques (333 líneas, 26 `!`)

```
.gitignore:1 333 líneas
├── Header 1 (6º sem, FOSS RNF-3.1, `#` como // en C, `!` como whitelist) [1-12]
├── Python / Backend / Stats [4-42] — __pycache__, *.so, .venv, .env
├── Docker [43-48] — .docker, docker-compose.override.yml
├── Android / Kotlin / Gradle [50-100] — *.apk, .gradle, build/, gradle-wrapper ! rescata
├── C++ / Arduino / Firmware [102-144] — *.o, .build/, .pio/, .vscode/arduino.json
├── Node-RED / Automation [145-162] — node_modules, flows*.json
├── IDE / Editor [164-181] — .vscode/*, .idea, *.swp, .DS_Store
├── OS / Filesystem [183-194] — .DS_Store, Thumbs.db, *.tmp
├── Logs / Runtime [196-203] — *.log, logs/
├── Testing / Coverage [205-210] — coverage/, .nyc_output
├── Secrets / Credentials [212-227] — *.pem, firmware/**/secrets.h (DEVOPS-11)
├── Project-specific [229-291] — *.db, postgres_data, mosquitto, firmware build, stats/data !, fritzing !, docs/_build, *.bak
└── Temporary [288-292] — tmp/, *.tmp
```

`!` = negación (whitelist). `26` en total — `8` para `gradle-wrapper`, `4` para `.vscode`, `10` para `stats/data`, `6` para `docs/fritzing`.

---

## 2. Header `1-12`

```gitignore
# 6º sem ... FOSS RNF-3.1 ... `#` como // en C, `!` como whitelist
```
Explica que línea con `#` es comentario (como `//` en C o `#` en Python), patrón es `glob` (`*.py[cod]` como `fnmatch`), `!` rescata (como `!gradlew` pese a `build/`). Referencia `docs/deuda-sprint1-sprint2.md:36` `DEVOPS-11` y `ci.yml:38`.

---

## 3. Python / Backend / Stats `4-42` — 2 años Python

| Patrón `.gitignore` | Qué ignora | Analogía React | Por qué no versionar |
|---|---|---|---|
| `__pycache__/` `*.py[cod]` `*$py.class` `*.so` | bytecode `*.pyc` + libs C | `.next/cache` en Next.js | Se regenera con `python -m compileall` |
| `.pytest_cache/` `.coverage` `htmlcov/` `.tox/` | cache `pytest`/`coverage` | `coverage/` en Jest | `pytest --cov` lo crea (52K) |
| `.venv/ venv/ env/` `pip-log.txt` `.mypy_cache/` `.ruff_cache/` | envs `python -m venv` | `node_modules` | Cada `pip install -r requirements.txt` crea uno (`backend/.venv/`, `stats/.venv/`) |
| `backend/.venv/` `stats/.venv/` `automation/.venv/` | envs por subproyecto | `app/node_modules` | Evita 3 `venv` de 300M en repo |
| `.env` `.env.local` `backend/.env` | secretos `DATABASE_URL` `POSTGRES_PASSWORD` | `.env` en React (`dotenv`) | `backend/.env.example:1` sí trackeado, `.env` real no (1 año PG) |

---

## 4. Docker `43-48`

| Patrón | Qué ignora | Analogía |
|---|---|---|
| `.docker/` | config Docker Desktop | `.docker/` en React |
| `docker-compose.override.yml` | override local (puertos/env) | `.env.local` en Docker |
| `*.dockerfile~` | backup | `*~` en editor |

---

## 5. Android / Kotlin / Gradle `50-100` — 2 años React (Gradle como Vite)

| Patrón | Qué ignora | Equiv. React |
|---|---|---|
| `*.apk *.aab *.jks *.keystore` | binarios firmados + keystores (clave Play Store) | `dist/*.js` minificado + `*.pem` |
| `.gradle/` `build/` `out/` | cache Gradle + `app/build` 72M | `node_modules/.cache` + `dist/` |
| `local.properties` `signing.properties` | `sdk.dir` no portable + passwords | `.env.local` |
| `*.iml` `.idea/` `*.iws` | IntelliJ | `.vscode/` |
| `.cxx/` `.externalNativeBuild/` | NDK CMake | `build/` NDK |
| `!gradle/wrapper/gradle-wrapper.jar` `!gradlew` (8×) | **Rescata** wrapper pese a `*.jar`/`build/` | `!dist/keep.json` en npm (whitelist) — `FIX MOV-12` quitó `*.gradle.kts` que bloqueaba `app/build.gradle.kts` |

`!` es clave: sin él, `gradlew` no se versiona y `ci.yml:317 assembleDebug` falla.

---

## 6. C++ / Arduino / Firmware `102-144` — 1 año C + 2 años Arduino

| Patrón | Artefacto `gcc`/`arduino-cli` | Se regenera con |
|---|---|---|
| `*.o *.obj *.elf *.hex *.bin *.map` | objeto `avr-gcc`, ejecutable, hex para flashear | `arduino-cli compile --fqbn` |
| `.build/` `.build-*/` `*.ino.cpp` | `arduino-cli` temp | `arduino-cli` |
| `.pio/` `.pioenvs/` `.piolibdeps/` | PlatformIO `lib_deps` (`firmware/*/platformio.ini`) | `pio run` |
| `.vscode/arduino.json` `c_cpp_properties.json` | config local Arduino | VS Code |

---

## 7. Node-RED `145-162`

| Patrón | Qué ignora | Analogía |
|---|---|---|
| `node_modules/` | deps `automation/package.json` | `node_modules` React |
| `automation/.node-red/node_modules/` `settings.js` `flows*.json` | Node-RED user dir con credenciales | `automation/.node-red/` activo; se versiona `automation/flows/*.json` canónico |

---

## 8. IDE / Editor `164-181` + OS `183-194` + Logs `196-203` + Coverage `205-210`

Clásico `React` `.gitignore`: `.vscode/*` (pero `!extensions.json` rescata), `.idea/`, `*.swp` (vim), `.DS_Store` (macOS), `Thumbs.db` (Windows), `*.log`, `logs/`, `coverage/` (Jest `coverage/`), `.nyc_output`.

---

## 9. Secrets `212-227` — `DEVOPS-11` (nunca `git add` `FELIPE.`)

| Patrón | Secreto | Historia |
|---|---|---|
| `*.pem *.key *.crt *.p12 *.pfx secrets/` | TLS, tokens | Como `secrets.h` en firmware |
| `firmware/gateway-esp32/secrets.h` `firmware/**/secrets.h` | `WIFI_SSID "FELIPE."` + `2516f751` real | `docs/auditoria-secretos-sprint1.md:15` H-01; CI `ci.yml:101` `cp secrets.h.example secrets.h` fallback `gateway-esp32.ino:28 #if __has_include` |

---

## 10. Project-specific `229-291` — 1 año PG + trazabilidad Sprint 1

| Patrón | Qué ignora / rescata | Por qué `!` whitelist |
|---|---|---|
| `*.db postgres_data/` `mosquitto/data/` `log/` | SQLite + volúmenes Docker | Datos, no código (`docker-compose.yml:9`) |
| `firmware/*/build/` `*/.pio/` | build `arduino-cli` | Como `app/build` |
| `stats/output/` | bench temporales (no versionar) | `stats/output/` sí ignorado |
| `stats/data/*` + `!stats/data/ema_demo.json` (6×) + `stats/*.csv !stats/data/*.csv` (4×) | **Antes:** `stats/data/` ignoraba todo → requería `git add -f` para evidencia EST-08; **Ahora:** `!` permite `git add` normal | `ema_demo.json` (KPI `89.3%`), `alpha_sweep.json/.png`, `ema-real-531.csv`, `ema_demo.csv`, `.gitkeep` — ver `stats/README.md §10` |
| `docs/fritzing/*.fzz !mega-cerrojo-v1.fzz` + `!AetherNet-P*.png` `!ema-*.png` `!alpha_sweep.png` | **Antes:** `*.fzz` bloqueaba `mega-cerrojo-v1.fzz` (538K Sprint 1) → `git add -f`; **Ahora:** rescata canónico + 7 breadboards `AetherNet-P1..P7` (276K-522K) + `ema-*.png` | Trazabilidad `docs/sprints.md:13` plano nRF24 |
| `docs/_build/ docs/site/ *.pdf` `*.bak *.backup` `tmp/` | Sphinx/MkDocs, backups `secrets.h.bak:1` (1 línea `FELIPE.` vs `FELIPE`) | `*.bak` cubre `firmware/gateway-esp32/secrets.h.bak` (537B, ver revisión) |

`26 !` en total — cada uno documentado como `!keep` en React (whitelist).

---

## 11. Cómo verificar trazabilidad (debug)

```bash
# ¿Un PNG está ignorado o trazable?
git check-ignore -v docs/fritzing/AetherNet-P1-EMA-UNO-v1-breadboard.png  # !... → no ignorado (?? untracked listo para git add)
git check-ignore -v stats/data/alpha_sweep.json                          # :: → no ignorado (??)
git check-ignore -v docs/fritzing/mega-cerrojo-v1.fzz                    # :: → no ignorado (trackeado)
git check-ignore -v .coverage                                             # .gitignore:12:.coverage → ignorado (coverage)
git check-ignore -v app/build                                              # app/.gitignore:1:/build → ignorado (72M)

# Estado trazabilidad
git status --porcelain | grep -E "AetherNet-P|ema-demo|alpha_sweep"  # ?? = listo para git add, !! = ignorado, M = trackeado
git ls-files | grep -E "\.png|\.fzz" | head  # ya trackeados: ema-*.png, mega-cerrojo-v1.fzz
wc -l .gitignore  # 333 (26 !)
```

---

## 12. Referencias cruzadas

- `docs/prd.md:51` KPI `>85%` (`stats/data/ema_demo.json`), `docs/requirements.md:22 RF-2.2` cerrojo, `docs/sprints.md:13` DEVOPS-05 nRF24 plano `docs/fritzing/mega-cerrojo-v1.fzz`
- `docs/architecture.md:31` `FastAPI+Mosquitto+PG` + `docs/roadmap.md:86` PG
- `firmware/gateway-esp32/secrets.h:1` + `secrets.h.example:1` ↔ `.gitignore:225` `firmware/**/secrets.h`
- `stats/README.md §10` `data/` evidencia + `docs/README.md` central `notebooks/` (no `stats/notebooks` duplicado 1.3M)
- `.github/workflows/ci.yml:38` `ruff/mypy` + `:101` `cp secrets.h.example` fallback + `:317` `gradlew` (gracias a `!gradlew`)

---

## 13. Glosario (si vienes de web)

| Término `.gitignore` | Equiv. React | Qué es |
|---|---|---|
| `__pycache__/ *.py[cod]` | `node_modules/.cache` `*.js` compilado | Bytecode Python |
| `build/` `app/build` 72M | `dist/` `build/` | Salida `gradlew`/`setuptools` |
| `.venv/` `backend/.venv` | `node_modules` | Env `python -m venv` |
| `.pio/` | `node_modules` PlatformIO | Deps `lib_deps` |
| `!gradlew` `!*.png` | `!dist/keep.json` | Whitelist pese a `build/` ignore |
| `firmware/**/secrets.h` | `.env` | Secreto LAN (DEVOPS-11) |
| `stats/data/* !ema_demo.json` | `dist/* !keep.json` | Ignora carpeta pero rescata evidencia |

---

*Documentado como estudiante 6º semestre que compara `.gitignore` Python (`__pycache__` como `.next`), React (`build/` como `dist/`, `!gradlew` como whitelist), C (`*.o` `*.elf` `gcc`), PostgreSQL (`postgres_data/` volumen), FOSS `RNF-3.1` nunca commitear `secrets.h`/`*.jks`, con `26 !` para trazabilidad sin `git add -f`.*
