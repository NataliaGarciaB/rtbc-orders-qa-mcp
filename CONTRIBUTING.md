# Guía de contribución y versionamiento

## Principios
- **TDD:** primero la prueba que falla, luego el mínimo código que la hace pasar, luego el refactor.
- **Herramientas deterministas:** la misma entrada produce siempre el mismo resultado ([ADR-003](docs/decisiones.md#adr-003-herramientas-deterministas)).
- **SOLID, DRY, KISS y YAGNI.** La lógica de análisis vive en `core` sin depender del protocolo ni del sistema de archivos ([ADR-004](docs/decisiones.md#adr-004-arquitectura-en-tres-capas)).
- **Calidad basada en riesgo.** Ver [docs/qa/estrategia-pruebas.md](docs/qa/estrategia-pruebas.md).

## Commits: Conventional Commits
Formato: `tipo(ámbito opcional): descripción en español, en minúscula y en imperativo`

| Tipo | Uso | Ejemplo |
|---|---|---|
| `feat` | Funcionalidad nueva | `feat(core): detectar transiciones sin escenario de aceptación` |
| `test` | Agregar o mejorar pruebas | `test(core): reportar la transición ENTREGADA → PAGADA sin escenario` |
| `refactor` | Cambio interno sin cambiar el comportamiento | `refactor(sources): extraer la lectura de tablas Markdown` |
| `fix` | Corrección de un defecto | `fix(sources): aceptar celdas con formato en negrita` |
| `docs` | Documentación | `docs: agregar catálogo de herramientas` |
| `build` | Dependencias, compilación, configuración de herramientas | `build: configurar Stryker con umbral de 70 %` |
| `ci` | Pipeline | `ci: agregar pipeline con quality gates` |
| `chore` | Mantenimiento sin impacto funcional | `chore: corregir .gitattributes` |

**Ámbitos:** `core`, `sources`, `server`, `qa`, `arch`.

Cada herramienta deja en el historial su secuencia TDD: un commit `test(...)` en rojo seguido de un commit `feat(...)` en verde, y un `refactor(...)` si aplica. Los commits en rojo pueden romper el build dentro de la rama; el CI valida solo el último commit del PR y los pushes a `main`.

## Ramas
Una rama por subfase, creada desde `main` actualizado:

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `docs/` | Documentación | `docs/fase-10-1-diseno` |
| `chore/` | Estructura y mantenimiento | `chore/fase-10-2-esqueleto` |
| `feature/` | Funcionalidad | `feature/fase-10-3-lectores` |

## Pull requests
1. El título sigue Conventional Commits.
2. La descripción incluye qué cambia, por qué, cómo se probó y qué riesgos cubre.
3. El CI debe estar en verde: compilación, pruebas, cobertura y mutación.
4. Se hace merge con **"Create a merge commit"**. No se usa squash ni rebase merge, para conservar la secuencia TDD.
5. La rama se borra después del merge.

## Versiones y tags

| Tag | Hito |
|---|---|
| `v0.1.0` | Diseño |
| `v0.2.0` | Esqueleto y CI |
| `v0.3.0` | Lectores de artefactos |
| `v0.4.0` | Herramientas y servidor MCP |
| `v1.0.0` | Integración con Claude Code, documentación y release |
