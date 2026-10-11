# Docs — Índice de Documentación AetherNet

> Proyecto Integrador 5º semestre — Ingeniería de Sistemas / Desarrollo de Software (UTP). La documentación se mantiene en sincronia con el estado real del repositorio y la implementación vigente a 2026-09-14.

## Estado general del proyecto

| Documento                                      | Propósito                            | Estado actual                                           |
| ---------------------------------------------- | ------------------------------------ | ------------------------------------------------------- |
| [prd.md](prd.md)                               | Visión, alcance e hitos del producto | Refrescado con alcance vigente y estado real del sprint |
| [requirements.md](requirements.md)             | RF/RNF y criterios BDD               | Ajustado a Telegram directo y alcance simplificado      |
| [hardware-inventory.md](hardware-inventory.md) | Hardware y pines por subsistema      | Actualizado con LED local, laser y chasis del rover     |
| [architecture.md](architecture.md)             | Comunicación entre subsistemas       | Alineado con MQTT + UART + RF + Telegram directo        |
| [sprints.md](sprints.md)                       | Planificación por sprint             | Estado actual de Sprint 3 actualizado                   |
| [backlog.md](backlog.md)                       | Tareas operativas por área           | Estado de entregas y deuda revisado                     |
| [roadmap.md](roadmap.md)                       | Aprendizaje requerido por materia    | Ajustado a la implementación actual                     |
| [risk-register.md](risk-register.md)           | Riesgos, mitigaciones y cierre       | Mantiene riesgos relevantes y decisiones ya tomadas     |
| [branching-strategy.md](branching-strategy.md) | Modelo de ramas                      | Vigente                                                 |
| [gantt.md](gantt.md)                           | Cronograma y dependencias            | Sincronizado con el sprint activo                       |
| [tablero-scrum.md](tablero-scrum.md)           | Seguimiento del backlog              | Al día con el estado del proyecto                       |

## Orden recomendado de lectura

1. [prd.md](prd.md)
2. [requirements.md](requirements.md)
3. [hardware-inventory.md](hardware-inventory.md)
4. [sprints.md](sprints.md)
5. [roadmap.md](roadmap.md)
6. [backlog.md](backlog.md)
7. [architecture.md](architecture.md)

## Resumen ejecutivo actual

- El sistema sigue siendo un proyecto de domótica local y robótica autónoma, con toda la lógica crítica dentro de la LAN.
- El rover y el control de acceso están en la capa de edge; la app y el backend son capas de coordinación y observación.
- La notificación de intrusión se lleva a cabo con Telegram HTTP directo, sin depender de Node-RED como flujo activo.
- El bombillo inteligente Tuya quedó descartado por no cumplir el criterio de FOSS y por depender de una API/clave propietaria.
- La documentación proyecta las decisiones reales del proyecto, no una versión idealizada del alcance.

## Índice por materia

| Materia                | Carpeta                                               | Documento principal                                                                                          | Roadmap                                                                                                  | Backlog                                                                                                  | Notebook                                                                                                                       |
| ---------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Estadística            | [Estadistica](Estadistica/)                           | [Estadistica/estadistica.md](Estadistica/estadistica.md)                                                     | [Estadistica/roadmap-estadistica.md](Estadistica/roadmap-estadistica.md)                                 | [Estadistica/backlog-estadistica.md](Estadistica/backlog-estadistica.md)                                 | [Estadistica/notebook/EMA_Estadistica.ipynb](Estadistica/notebook/EMA_Estadistica.ipynb)                                       |
| DevOps                 | [DevOps](DevOps/)                                     | [DevOps/devops.md](DevOps/devops.md)                                                                         | [DevOps/roadmap-devops.md](DevOps/roadmap-devops.md)                                                     | [DevOps/backlog-devops.md](DevOps/backlog-devops.md)                                                     | [DevOps/notebook/Diario_DevOps.ipynb](DevOps/notebook/Diario_DevOps.ipynb)                                                     |
| Administración         | [Administracion-Proyectos](Administracion-Proyectos/) | [Administracion-Proyectos/administracion-proyectos.md](Administracion-Proyectos/administracion-proyectos.md) | [Administracion-Proyectos/roadmap-administracion.md](Administracion-Proyectos/roadmap-administracion.md) | [Administracion-Proyectos/backlog-administracion.md](Administracion-Proyectos/backlog-administracion.md) | [Administracion-Proyectos/notebook/Diario_Administracion.ipynb](Administracion-Proyectos/notebook/Diario_Administracion.ipynb) |
| Programación Móvil     | [Programacion-Movil](Programacion-Movil/)             | [Programacion-Movil/programacion-movil.md](Programacion-Movil/programacion-movil.md)                         | [Programacion-Movil/roadmap-movil.md](Programacion-Movil/roadmap-movil.md)                               | [Programacion-Movil/backlog-movil.md](Programacion-Movil/backlog-movil.md)                               | [Programacion-Movil/notebook/AetherControl_Notebook.ipynb](Programacion-Movil/notebook/AetherControl_Notebook.ipynb)           |
| Automatización LowCode | [Automatizacion-LowCode](Automatizacion-LowCode/)     | —                                                                                                            | [Automatizacion-LowCode/roadmap-lowcode.md](Automatizacion-LowCode/roadmap-lowcode.md)                   | [Automatizacion-LowCode/backlog-lowcode.md](Automatizacion-LowCode/backlog-lowcode.md)                   | [Automatizacion-LowCode/notebook/Diario_LowCode.ipynb](Automatizacion-LowCode/notebook/Diario_LowCode.ipynb)                   |
| Firmware               | [Firmware](Firmware/)                                 | [Firmware/firmware.md](Firmware/firmware.md)                                                                 | [Firmware/roadmap-firmware.md](Firmware/roadmap-firmware.md)                                             | —                                                                                                        | [Firmware/notebook/Firmware_Notebook.ipynb](Firmware/notebook/Firmware_Notebook.ipynb)                                         |
| Hardware               | [Hardware](Hardware/)                                 | [Hardware/hardware.md](Hardware/hardware.md)                                                                 | [Hardware/roadmap-hardware.md](Hardware/roadmap-hardware.md)                                             | —                                                                                                        | [Hardware/notebook/Diario_Hardware.ipynb](Hardware/notebook/Diario_Hardware.ipynb)                                             |

## Notebooks y datasets

- Canónico: [docs/Estadistica/notebook/EMA_Estadistica.ipynb](Estadistica/notebook/EMA_Estadistica.ipynb)
- Datasets: [stats/Dataset](../stats/Dataset)
- Referencia de validación: [stats/data/water_turbidity_report.json](../stats/data/water_turbidity_report.json)

## Reglas de priorización

Si algo en la documentación contradice una decisión técnica o una implementación real, la fuente de verdad es el código y los cambios de alcance documentados en [sprints.md](sprints.md) y [backlog.md](backlog.md).
