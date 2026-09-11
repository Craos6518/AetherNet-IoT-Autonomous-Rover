# Diario de Campo — DevOps

> **Notebook:** `notebooks/Diario_DevOps.ipynb` · **Infra:** `docker-compose.yml` + `.github/workflows/ci.yml` + `backend/mosquitto/config/`

## Capturas / logs — checklist

- [ ] **Docker ps** — `capturas/docker-ps.png` — `docker compose ps` FastAPI 1.0.0 + Postgres 16 + Mosquitto 2.0 healthy
- [ ] **CI verde** — `capturas/ci-green.png` — screenshot GitHub Actions 6 jobs verde (backend-test, firmware-compile, docker-build, stats-test, security-scan)
- [ ] **Mosquitto ACL** — `capturas/mosquitto-acl.png` — `mosquitto_sub aethernet/#` + `acl.conf aethernet/#`
- [ ] **Health 200** — `capturas/health-200.png` — `curl http://192.168.1.14:8000/health {"status":"ok"}`

Si falta:

```markdown
![CAPTURA PENDIENTE](capturas/docker-ps.png) — tomar `docker compose ps` + `curl /health`
```

## Logs canónicos

- `docs/logs/firmware_sprint3/live_2026-09-11/T1_docker_live.log` + `T1_mqtt_live.log`
- `docs/logs/firmware_sprint3/T1_docker_2026-09-11.log` (raíz)
- `backend/mosquitto/config/mosquitto.conf:32` + `acl.conf:1` `aethernet/#`
