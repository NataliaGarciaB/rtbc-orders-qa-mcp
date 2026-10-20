# Datos de prueba

`api-v1.0.0/` es una copia de los artefactos de [`rtbc-orders-api`](https://github.com/NataliaGarciaB/rtbc-orders-api) en el tag `v1.0.0` (commit `1447515`). Fija las condiciones de las pruebas aunque la API cambie después ([ADR-010](../../docs/decisiones.md#adr-010-estrategia-de-pruebas-y-quality-gates)).

| Archivo | Lo usa |
|---|---|
| `README.md` | Tabla de reglas de negocio (`construir_trazabilidad`, recurso `rtbc://reglas-negocio`) |
| `docs/api/openapi.yaml` | Contrato (`detectar_escenarios_faltantes`, `generar_datos_prueba`) |
| `docs/maquina-estados.md` | Matriz de transiciones (`generar_matriz_transiciones`) |
| `docs/qa/estrategia-pruebas.md` | Matriz de riesgos (`priorizar_por_riesgo`) |
| `src/test/resources/acceptance/` | Escenarios de Karate y sus helpers |
| `src/test/java/` | Pruebas JUnit con `@Tag("RN-xx")` (`construir_trazabilidad`) |

Para actualizar la copia a otra versión de la API, desde el repositorio de la API:

```bash
git ls-tree -r --name-only <tag> -- README.md docs/api/openapi.yaml docs/maquina-estados.md \
  docs/qa/estrategia-pruebas.md src/test/resources/acceptance src/test/java
```

y copiar cada archivo con `git show <tag>:<ruta>`. Los resultados esperados de las pruebas deben revisarse con la nueva versión.
