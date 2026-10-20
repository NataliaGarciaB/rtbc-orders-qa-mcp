# RTBC Orders QA MCP

Servidor [MCP](https://modelcontextprotocol.io) que le da a Claude Code herramientas deterministas de análisis de calidad sobre la API de órdenes [`rtbc-orders-api`](https://github.com/NataliaGarciaB/rtbc-orders-api).

Las herramientas leen los artefactos reales de la API (contrato OpenAPI, máquina de estados, matriz de riesgos y escenarios de aceptación) y devuelven resultados verificables: qué escenarios faltan, cuáles son más riesgosos, qué datos de prueba usar y cómo se relaciona cada regla de negocio con sus pruebas. La IA razona sobre esos resultados; el cálculo lo hace código probado.

> **Estado:** en construcción. El diseño está completo en [`docs/`](docs/); el proyecto, la prueba de arquitectura y el pipeline con quality gates están listos.

| Documento | Contenido |
|---|---|
| [`docs/herramientas.md`](docs/herramientas.md) | Catálogo de herramientas y recursos, con el contrato de cada una |
| [`docs/decisiones.md`](docs/decisiones.md) | Registro de decisiones técnicas (ADR) |
| [`docs/qa/estrategia-pruebas.md`](docs/qa/estrategia-pruebas.md) | Riesgos, pirámide de pruebas y quality gates |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Commits, ramas, pull requests y versiones |

## Licencia

[MIT](LICENSE)
