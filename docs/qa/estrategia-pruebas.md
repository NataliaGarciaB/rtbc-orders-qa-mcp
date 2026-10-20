# Estrategia de pruebas basada en riesgo

## 1. Objetivo
Dar confianza de que cada herramienta dice la verdad sobre los artefactos de la API. Un servidor de análisis de calidad que reporta un vacío inexistente, u omite uno real, hace más daño que no tenerlo: lleva a escribir pruebas innecesarias o a confiar en una cobertura que no existe.

## 2. Matriz de riesgos

**Escala:** probabilidad (P) e impacto (I) de 1 a 3. Nivel = P × I: **Alto ≥ 6**, Medio de 3 a 5, Bajo ≤ 2.

| ID | Riesgo | P | I | Nivel | Mitigación |
|---|---|---|---|---|---|
| Q01 | **Falso vacío:** se reporta como faltante un escenario que sí existe | 3 | 2 | **6 Alto** | Pruebas sobre la copia real de los `.feature` de la API, con el resultado esperado calculado a mano y revisado |
| Q02 | **Omisión:** un vacío real no se reporta | 2 | 3 | **6 Alto** | Casos con un escenario quitado a propósito: la herramienta debe reportar exactamente ese vacío. Mutation testing sobre la lógica de comparación |
| Q03 | Un cambio de formato en un artefacto produce resultados incompletos sin avisar | 2 | 3 | **6 Alto** | Cada lector valida la estructura esperada y falla con un error explícito; pruebas con artefactos mal formados |
| Q04 | Una ruta maliciosa permite leer archivos fuera del repositorio | 1 | 3 | 3 Medio | Pruebas de rutas con `../`, absolutas y enlaces fuera del repositorio |
| Q05 | Un escenario generado no es Gherkin válido o usa helpers inexistentes | 2 | 2 | 4 Medio | Cada escenario generado se valida con el parser; en la demo, se ejecuta en el CI de la API |
| Q06 | El contrato MCP de una herramienta es incorrecto (esquema de entrada o forma de salida) | 2 | 2 | 4 Medio | Prueba de integración con un cliente MCP real del SDK, en memoria |
| Q07 | Resultados no deterministas (orden variable entre ejecuciones) | 2 | 2 | 4 Medio | Comparación de resultados completos, no solo de fragmentos; dos ejecuciones seguidas deben ser idénticas |
| Q08 | La lógica de análisis se acopla al protocolo o al sistema de archivos | 2 | 1 | 2 Bajo | Prueba de arquitectura sobre los imports de `core` |

## 3. Pirámide de pruebas

| Nivel | Qué valida | Herramienta | Proporción aproximada |
|---|---|---|---|
| Unitarias de `core` | Cobertura de transiciones y respuestas, riesgo, datos de prueba, trazabilidad y generación de escenarios, con modelos en memoria | Vitest | ~60 % |
| Lectores (`sources`) | Conversión de cada artefacto real a modelos, y errores ante formatos inválidos | Vitest + copia de los artefactos de la API | ~25 % |
| Integración MCP | Registro de herramientas, validación de entradas y forma de las respuestas, con un cliente MCP real | Vitest + transporte en memoria del SDK | ~10 % |
| Arquitectura | `core` no depende de MCP ni del sistema de archivos | Vitest | 1 prueba |
| Demostración de punta a punta | Claude Code usa el servidor sobre el repositorio real de la API | Manual, con evidencia en un PR | 1 demo |

## 4. Datos de prueba
- `test/fixtures/api-v1.0.0/`: copia de los artefactos de la API en su versión `v1.0.0`. Fija las condiciones de las pruebas aunque la API cambie después.
- Las variantes (un escenario quitado, una tabla mal formada) se construyen en cada prueba a partir de esa copia, sin duplicar archivos.
- Los resultados esperados sobre la copia real se calculan a mano a partir de los artefactos y se revisan en el PR. Por ejemplo, qué transiciones cubre hoy Karate.

## 5. Quality gates

| Gate | Umbral |
|---|---|
| Pruebas fallidas | 0 |
| Compilación TypeScript en modo estricto | 0 errores |
| Cobertura de líneas y de ramas | ≥ 80 % |
| Mutation score (Stryker) | ≥ 70 % |

Cada gate es un paso del pipeline de CI y un check obligatorio para hacer merge a `main`.

## 6. Fuera de alcance
- Rendimiento: los artefactos son pequeños y el análisis es local.
- Otros clientes MCP distintos de Claude Code: el protocolo es estándar, pero solo se valida con el cliente del SDK y con Claude Code.
