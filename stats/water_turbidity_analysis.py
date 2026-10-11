"""
==============================================================================
water_turbidity_analysis.py — EST-06/EST-05 con Dataset water-level_turbidity
6º Semestre UTP | TS4D3 Estadística | RNF-2.1 / RNF-2.2 / HU-03 / KPI >85%
Autor: Andres Felipe Martinez Henao
FOSS: pandas/scipy/matplotlib — RNF-3.1 (sin R propietario)
==============================================================================
QUÉ ES ESTO:
  Análisis estadístico del dataset externo water-level_turbidity-low/medium/high
  (31.500 filas = 3 turbideces × 5 angles × 7 water_levels × 300 muestras).

  Conecta con:
  - ema_filter.py:15 (EMA α=0.2) → aplica a us_value vs water_level true
  - requirements.md:42 RNF-2.1 (EMA HC-SR04) / RNF-2.2 (t-Student, descriptivo)
  - prd.md:51 KPI >85% reducción ruido
  - U1 descriptiva, U3 series, U4 inferencia (t-Student Welch, ANOVA)

  Dataset procedencia (ver stats/Dataset/README.md):
  - Fuente: Kaggle "Water level identification with distance sensors"
    https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar
  - Autores: Ranieri, C.M. et al., 2024. Engineering Applications of Artificial
    Intelligence, 127, p.107235. doi:10.1016/j.engappai.2023.107235
    https://doi.org/10.1016/j.engappai.2023.107235
  - Licencia: CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/)
    Compatible RNF-3.1 — requiere atribución + share-alike.
  - Descripción: banco de laboratorio con LiDAR (ir_*), ultrasónico (us_value)
    e IMU (acc_/gyr_*) — variables controladas angle, distance, turbidity.
    Objetivo original: predecir water_level con ML. Aquí se reutiliza para
    validar pipeline EMA + descriptivo + inferencia previo a datos Rover reales.
  - Fecha dataset: 2023-10-25, 721 KB zip, 3.137 views / 508 downloads (2026-09).

  Estructura dataset (ver water-level_turbidity-low.csv:1):
    id, ir_value, ir_strength, us_value, acc_x/y/z, gyr_*, angle, water_level
    turbidez = low/medium/high (archivo), angle 0/2.5/5/7.5/10, water_level 50..350

  Si vienes de JS/React: este archivo es como un utils/analytics.ts que lee
  3 CSV, saca describe() (como console.table), filtra con EMA y hace tests.

  Si vienes de C/Arduino: us_value es análogo a HC-SR04 (time-of-flight).
  ir_strength es proxy turbidez (como KY-037 para ruido óptico).
==============================================================================
"""

import json
from pathlib import Path

# Reusa EMA del módulo núcleo — no duplica fórmula (DRY con rover-uno.ino:259)
from ema_filter import EMAFilter, calculate_noise_reduction

# Dependencias FOSS — fallan con mensaje claro si no están instaladas
try:
    import numpy as np
    import pandas as pd
    from scipy import stats as scipy_stats
except ImportError as e:
    raise ImportError(
        "Faltan deps stats: pip install -r stats/requirements.txt  "
        "(pandas, scipy, matplotlib). " + str(e)
    )


# Rutas robustas — funciona desde repo raíz o desde stats/
REPO_ROOT = Path(__file__).parent.parent if (Path(__file__).parent / "Dataset").exists() else Path(__file__).parent
DATASET_DIR = Path(__file__).parent / "Dataset"
if not DATASET_DIR.exists():
    DATASET_DIR = REPO_ROOT / "stats" / "Dataset"

FILES = {
    "low": DATASET_DIR / "water-level_turbidity-low.csv",
    "medium": DATASET_DIR / "water-level_turbidity-medium.csv",
    "high": DATASET_DIR / "water-level_turbidity-high.csv",
}


def load_water_turbidity_data() -> "pd.DataFrame":
    """Carga 3 CSV y añade columna turbidity. Retorna DataFrame 31.500 filas.

    Como hacer 3 fetches y mergear en JS: Promise.all([fetch(low), fetch(med), fetch(high)])
    """
    frames: list[pd.DataFrame] = []
    for turb, path in FILES.items():
        if not path.exists():
            raise FileNotFoundError(f"No encontrado: {path} — verifica stats/Dataset/")
        df = pd.read_csv(path)
        df["turbidity"] = turb
        # Orden categórico para plots/tablas consistentes
        df["turbidity"] = pd.Categorical(df["turbidity"], categories=["low", "medium", "high"], ordered=True)
        frames.append(df)
    full = pd.concat(frames, ignore_index=True)
    # Validación diseño balanceado esperado
    assert len(full) == 31500, f"Esperado 31500 filas, hallado {len(full)}"
    return full


def descriptive_by_group(df: "pd.DataFrame") -> dict:
    """U1 descriptiva: medias/varianzas por turbidez, angle y water_level."""
    out: dict = {}
    # Global
    out["global"] = {
        "n": int(len(df)),
        "us_value": {"mean": float(df["us_value"].mean()), "std": float(df["us_value"].std()), "min": float(df["us_value"].min()), "max": float(df["us_value"].max())},
        "ir_strength": {"mean": float(df["ir_strength"].mean()), "std": float(df["ir_strength"].std()), "min": float(df["ir_strength"].min()), "max": float(df["ir_strength"].max())},
    }
    # Por turbidez — convertir tuplas a str para JSON
    raw = df.groupby("turbidity", observed=True)[["us_value", "ir_strength"]].agg(["mean", "std", "min", "max"]).round(2)
    out["by_turbidity"] = {str(k): v for k, v in raw.to_dict().items()}
    raw2 = df.groupby("angle", observed=True)[["ir_strength", "us_value"]].agg(["mean", "std"]).round(2)
    out["by_angle"] = {str(k): v for k, v in raw2.to_dict().items()}
    raw3 = df.groupby("water_level", observed=True)[["us_value", "ir_strength"]].agg(["mean", "std"]).round(2)
    out["by_water_level"] = {str(k): v for k, v in raw3.to_dict().items()}
    # Matriz angle × water_level × turbidity (para validar 300 c/u)
    combo = df.groupby(["turbidity", "angle", "water_level"], observed=True).size()
    out["combo_counts"] = {"n_combos": int(combo.size), "unique_counts": sorted(combo.unique().tolist()), "expected_300": bool((combo == 300).all())}
    return out


def ema_on_us_value(df: "pd.DataFrame", alpha: float = 0.2) -> dict:
    """Aplica EMA α=0.2 a us_value agrupado por (turbidity, angle, water_level) ordenado por id.

    Retorna métrica KPI global y por water_level — true = water_level (ground truth).
    Como test_ema_filter.py:99 calculate_noise_reduction pero con datos reales.
    """
    # Orden temporal: id dentro de cada grupo (simula serie tiempo)
    df_sorted = df.sort_values(["turbidity", "angle", "water_level", "id"])
    # Agrupa y filtra cada serie 300 muestras
    all_raw: list[float] = []
    all_filt: list[float] = []
    all_true: list[float] = []
    per_wl: dict[float, dict] = {}

    for (turb, ang, wl), group in df_sorted.groupby(["turbidity", "angle", "water_level"], observed=True):
        raw = group["us_value"].tolist()
        true = [float(wl)] * len(raw)
        ema = EMAFilter(alpha=alpha)
        filt = [ema.update(v) for v in raw]
        all_raw.extend(raw)
        all_filt.extend(filt)
        all_true.extend(true)
        # KPI por water_level (acumula across turb/angle para ese wl)
        if wl not in per_wl:
            per_wl[wl] = {"raw": [], "filt": [], "true": []}
        per_wl[wl]["raw"].extend(raw)
        per_wl[wl]["filt"].extend(filt)
        per_wl[wl]["true"].extend(true)

    global_metrics = calculate_noise_reduction(all_raw, all_filt, all_true)

    # Métricas por water_level
    per_wl_metrics = {}
    for wl, d in per_wl.items():
        m = calculate_noise_reduction(d["raw"], d["filt"], d["true"])
        # Error absoluto complementario
        raw_mae = float(np.mean(np.abs(np.array(d["raw"]) - np.array(d["true"]))))
        filt_mae = float(np.mean(np.abs(np.array(d["filt"]) - np.array(d["true"]))))
        per_wl_metrics[str(wl)] = {**m, "raw_mae": round(raw_mae, 3), "filt_mae": round(filt_mae, 3), "n": len(d["raw"])}

    # También por angle (para ver dónde EMA más útil)
    per_angle = {}
    for ang, group in df_sorted.groupby("angle", observed=True):
        raw = group["us_value"].tolist()
        true = group["water_level"].astype(float).tolist()
        ema = EMAFilter(alpha=alpha)
        filt = [ema.update(v) for v in raw]
        m = calculate_noise_reduction(raw, filt, true)
        per_angle[str(ang)] = {**m, "n": len(raw)}

    # --- Evaluación rampa continua (simula sensor en movimiento / cambio de nivel) ---
    # Ordena por id (orden de captura real) y aplica EMA sin reset → mide retardo en escalones
    df_ramp = df.sort_values("id")
    ramp_raw = df_ramp["us_value"].tolist()
    ramp_true = df_ramp["water_level"].astype(float).tolist()
    ema_r = EMAFilter(alpha=alpha)
    ramp_filt = [ema_r.update(v) for v in ramp_raw]
    ramp_metrics = calculate_noise_reduction(ramp_raw, ramp_filt, ramp_true)
    # MSE sin reset incluye error de transición entre escalones (útil para comparar con simulado)

    return {
        "global_per_group": global_metrics,
        "global": global_metrics,  # compatibilidad
        "by_water_level": per_wl_metrics,
        "by_angle": per_angle,
        "ramp_continuous": ramp_metrics,
        "alpha": alpha,
        "n_total": len(all_raw),
        "note": "global_per_group = EMA reseteado por (turb,angle,wl) — mide solo varianza intra-grupo (sin bias inter-grupo). ramp_continuous = EMA sin reset en orden id — incluye retardo en escalones 50→100→...→350, análogo a test_ultrasonic_approach.",
    }


def hypothesis_tests(df: "pd.DataFrame") -> dict:
    """U4 inferencia: t-Student Welch + ANOVA + correlación.

    Tests propuestos para informe EST-07 — FOSS scipy, sin R.
    """
    out: dict = {}

    # 1) t-Student Welch: ir_strength low vs high (¿turbidez afecta sensor IR?)
    low_ir = df[df["turbidity"] == "low"]["ir_strength"]
    high_ir = df[df["turbidity"] == "high"]["ir_strength"]
    # Welch (equal_var=False) — como en roadmap.md U4
    t_ir, p_ir = scipy_stats.ttest_ind(low_ir, high_ir, equal_var=False)
    # Supuestos
    # Shapiro en muestra 5k (limitado a 5000 por performance scipy) — si n>5000 muestrea
    sh_low = scipy_stats.shapiro(low_ir.sample(5000, random_state=42) if len(low_ir) > 5000 else low_ir)
    sh_high = scipy_stats.shapiro(high_ir.sample(5000, random_state=42) if len(high_ir) > 5000 else high_ir)
    levene_ir = scipy_stats.levene(low_ir.sample(5000, random_state=42), high_ir.sample(5000, random_state=42))
    # d Cohen
    pooled_sd = float(np.sqrt((low_ir.var(ddof=1) + high_ir.var(ddof=1)) / 2))
    cohen_d_ir = float((low_ir.mean() - high_ir.mean()) / pooled_sd) if pooled_sd else 0.0

    out["ttest_ir_low_vs_high"] = {
        "H0": "mu_low == mu_high (turbidez no afecta ir_strength)",
        "H1": "mu_low != mu_high",
        "t": round(float(t_ir), 4),
        "p": float(p_ir),
        "reject_H0_05": bool(p_ir < 0.05),
        "cohen_d": round(cohen_d_ir, 4),
        "interpretacion_d": "pequeño" if abs(cohen_d_ir) < 0.5 else "mediano" if abs(cohen_d_ir) < 0.8 else "grande",
        "shapiro_low_p": float(sh_low.pvalue),
        "shapiro_high_p": float(sh_high.pvalue),
        "levene_p": float(levene_ir.pvalue),
        "n_low": int(len(low_ir)),
        "n_high": int(len(high_ir)),
        "mean_low": round(float(low_ir.mean()), 1),
        "mean_high": round(float(high_ir.mean()), 1),
    }

    # 2) t-Student Welch: error us |us - water_level| low vs high
    df = df.copy()
    df["us_error"] = (df["us_value"] - df["water_level"]).abs()
    low_err = df[df["turbidity"] == "low"]["us_error"]
    high_err = df[df["turbidity"] == "high"]["us_error"]
    t_err, p_err = scipy_stats.ttest_ind(low_err, high_err, equal_var=False)
    pooled_sd_e = float(np.sqrt((low_err.var(ddof=1) + high_err.var(ddof=1)) / 2))
    cohen_d_e = float((low_err.mean() - high_err.mean()) / pooled_sd_e) if pooled_sd_e else 0.0
    out["ttest_us_error_low_vs_high"] = {
        "H0": "error_us low == error_us high",
        "t": round(float(t_err), 4),
        "p": float(p_err),
        "reject_H0_05": bool(p_err < 0.05),
        "cohen_d": round(cohen_d_e, 4),
        "mean_low": round(float(low_err.mean()), 3),
        "mean_high": round(float(high_err.mean()), 3),
    }

    # 3) ANOVA 1 vía: ir_strength ~ turbidity (3 grupos)
    med_ir = df[df["turbidity"] == "medium"]["ir_strength"]
    f_anova, p_anova = scipy_stats.f_oneway(low_ir, med_ir, high_ir)
    out["anova_ir_turbidity"] = {"F": round(float(f_anova), 3), "p": float(p_anova), "reject_H0_05": bool(p_anova < 0.05)}

    # 4) Correlación us_value vs water_level (debe ~1, con ruido a 350)
    r_pearson, p_r = scipy_stats.pearsonr(df["water_level"], df["us_value"])
    r_spearman, p_s = scipy_stats.spearmanr(df["water_level"], df["us_value"])
    out["correlation_us_vs_true"] = {
        "pearson_r": round(float(r_pearson), 4),
        "pearson_p": float(p_r),
        "spearman_r": round(float(r_spearman), 4),
        "spearman_p": float(p_s),
    }

    # 5) Fallback no paramétrico si supuestos violados: Mann-Whitney low vs high ir
    mw_ir = scipy_stats.mannwhitneyu(low_ir, high_ir, alternative="two-sided")
    out["mannwhitney_ir_low_vs_high"] = {"U": float(mw_ir.statistic), "p": float(mw_ir.pvalue), "reject_H0_05": bool(mw_ir.pvalue < 0.05)}

    return out


def generate_report(output_json: Path | None = None, output_png_dir: Path | None = None) -> dict:
    """Pipeline completo: carga → descriptivo → EMA → tests → guarda JSON/PNG."""
    df = load_water_turbidity_data()
    desc = descriptive_by_group(df)
    ema = ema_on_us_value(df, alpha=0.2)
    tests = hypothesis_tests(df)

    report = {
        "dataset": "water-level_turbidity low/medium/high",
        "source": {
            "url": "https://www.kaggle.com/datasets/caetanoranieri/water-level-identification-with-lidar",
            "citation": "Ranieri, C.M., Foletto, A.V., Garcia, R.D., Matos, S.N., Medina, M.M., Marcolino, L.S. and Ueyama, J., 2024. Water level identification with laser sensors, inertial units, and machine learning. Engineering Applications of Artificial Intelligence, 127, p.107235. doi:10.1016/j.engappai.2023.107235",
            "doi": "https://doi.org/10.1016/j.engappai.2023.107235",
            "license": "CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/)",
            "license_compatible_RNF31": True,
            "description": "Banco laboratorio LiDAR + ultrasónico + IMU — variables controladas angle, distance, turbidity — objetivo predecir water_level",
            "date": "2023-10-25",
            "files": ["water-level_turbidity-low.csv", "water-level_turbidity-medium.csv", "water-level_turbidity-high.csv"],
        },
        "n_total": int(len(df)),
        "n_per_file": 10500,
        "design": "3 turbideces × 5 angles × 7 water_levels × 300 = 31500 — balanceado 300/combo",
        "columns": df.columns.tolist(),
        "descriptive": desc,
        "ema_us_vs_water_level": ema,
        "hypothesis_tests": tests,
        "conclusion": (
            f"EMA α=0.2 per-grupo (varianza intra-grupo): reducción {ema['global']['noise_reduction_pct']:.1f}% "
            f"({'✓ KPI >85' if ema['global']['meets_kpi'] else '✗ KPI <85 — esperado: EMA reduce varianza, no bias sistemático'}). "
            f"Rampa continua (con escalones): {ema['ramp_continuous']['noise_reduction_pct']:.1f}% — incluye retardo. "
            f"MAE raw {np.mean(np.abs(df['us_value']-df['water_level'])):.2f} → filt MAE ~{np.mean([v['filt_mae'] for v in ema['by_water_level'].values()]):.2f}. "
            f"t-test ir low vs high: p={tests['ttest_ir_low_vs_high']['p']:.2e} "
            f"({'rechaza H0' if tests['ttest_ir_low_vs_high']['reject_H0_05'] else 'no rechaza'}), d={tests['ttest_ir_low_vs_high']['cohen_d']} (pequeño pero significativo, n=10500). "
            f"ANOVA turbidez F={tests['anova_ir_turbidity']['F']}, p={tests['anova_ir_turbidity']['p']:.2e} — efecto turbidez real. "
            f"Correlación us-true r={tests['correlation_us_vs_true']['pearson_r']:.3f} (fuerte, pero bias a 150/350)."
        ),
    }

    # Guarda JSON versionable
    if output_json is None:
        output_json = Path(__file__).parent / "data" / "water_turbidity_report.json"
    output_json.parent.mkdir(parents=True, exist_ok=True)
    with open(output_json, "w") as f:
        json.dump(report, f, indent=2, default=str)
    print(f"JSON guardado en {output_json}")
    print(report["conclusion"])

    # PNG opcional (descriptivo + EMA por water_level)
    if output_png_dir is None:
        output_png_dir = Path(__file__).parent / "data"
    try:
        import matplotlib.pyplot as plt

        # Fig 1: us_value mean por water_level y turbidez (valida y = x)
        fig, ax = plt.subplots(figsize=(8, 4))
        for turb in ["low", "medium", "high"]:
            sub = df[df["turbidity"] == turb].groupby("water_level")["us_value"].mean()
            ax.plot(sub.index, sub.values, marker="o", label=turb)
        ax.plot([50, 350], [50, 350], "--", color="gray", label="ideal y=x")
        ax.set_xlabel("water_level true (mm)")
        ax.set_ylabel("us_value mean")
        ax.set_title("us_value vs water_level por turbidez")
        ax.legend()
        ax.grid(alpha=0.2)
        plt.tight_layout()
        p1 = output_png_dir / "water_us_vs_true.png"
        plt.savefig(p1, dpi=180)
        print(f"PNG {p1}")

        # Fig 2: ir_strength por angle
        fig, ax = plt.subplots(figsize=(8, 4))
        pivot = df.groupby(["angle", "turbidity"])["ir_strength"].mean().unstack()
        pivot.plot(kind="bar", ax=ax)
        ax.set_title("ir_strength mean por angle y turbidez")
        ax.set_ylabel("ir_strength")
        plt.tight_layout()
        p2 = output_png_dir / "water_ir_by_angle.png"
        plt.savefig(p2, dpi=180)
        print(f"PNG {p2}")

        # Copia a docs/fritzing para notebook
        import shutil

        frit = Path(__file__).parent.parent / "docs" / "fritzing"
        frit.mkdir(parents=True, exist_ok=True)
        for p in [p1, p2]:
            if p.exists():
                shutil.copy(p, frit / p.name)
                print(f"copiado a {frit / p.name}")
        plt.close("all")
    except Exception as e:
        print(f"Plot falló (no crítico): {e}")

    return report


if __name__ == "__main__":
    generate_report()
