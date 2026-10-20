# Registro de decisiones técnicas (ADR)

Cada decisión registra el contexto, la opción elegida y su consecuencia. Si una decisión cambia, se agrega una nueva entrada que la reemplaza; no se borra la anterior.

| ID | Decisión | Estado |
|---|---|---|
| [ADR-001](#adr-001-servidor-mcp-en-typescript-con-el-sdk-oficial) | Servidor MCP en TypeScript con el SDK oficial | Aceptada |
| [ADR-002](#adr-002-transporte-stdio) | Transporte stdio | Aceptada |
| [ADR-003](#adr-003-herramientas-deterministas) | Herramientas deterministas: la IA razona, el código calcula | Aceptada |
| [ADR-004](#adr-004-arquitectura-en-tres-capas) | Arquitectura en tres capas: `core`, `sources`, `server` | Aceptada |
| [ADR-005](#adr-005-fuentes-de-verdad-los-artefactos-de-la-api) | Fuentes de verdad: los artefactos versionados de la API | Aceptada |
| [ADR-006](#adr-006-lectura-de-escenarios-karate-con-el-parser-oficial-de-gherkin) | Lectura de escenarios Karate con el parser oficial de Gherkin | Aceptada |
| [ADR-007](#adr-007-seguridad-solo-lectura-y-dentro-del-repositorio) | Seguridad: solo lectura y dentro del repositorio configurado | Aceptada |
| [ADR-008](#adr-008-generar-no-es-modificar) | Generar no es modificar: las herramientas no escriben en la API | Aceptada |
| [ADR-009](#adr-009-skill-analisis-qa-como-flujo-de-trabajo) | Skill `/analisis-qa` como flujo de trabajo sobre las herramientas | Aceptada |
| [ADR-010](#adr-010-estrategia-de-pruebas-y-quality-gates) | Estrategia de pruebas y quality gates | Aceptada |
| [ADR-011](#adr-011-versionamiento) | Versionamiento | Aceptada |

---

## ADR-001: Servidor MCP en TypeScript con el SDK oficial
- **Contexto:** se necesita un servidor que Claude Code pueda usar como fuente de herramientas de análisis de calidad. MCP (Model Context Protocol) es el protocolo estándar para exponer herramientas, recursos y prompts a un asistente de IA.
- **Decisión:** TypeScript sobre Node.js con el SDK oficial `@modelcontextprotocol/sdk`. Las entradas de cada herramienta se validan con esquemas `zod`, que el SDK también usa para publicar el contrato de la herramienta.
- **Consecuencia:** el SDK oficial sigue la especificación del protocolo y trae un transporte en memoria para pruebas de integración con un cliente real. TypeScript en modo estricto detecta en compilación errores de forma en los resultados.

## ADR-002: Transporte stdio
- **Contexto:** el servidor lo usa una persona desde Claude Code en su propio equipo.
- **Decisión:** transporte `stdio`. Claude Code lanza el proceso y se comunica por la entrada y salida estándar.
- **Consecuencia:** no hay puertos abiertos ni autenticación que gestionar. Los logs del servidor van a `stderr`, porque `stdout` es exclusivo del protocolo.

## ADR-003: Herramientas deterministas
- **Contexto:** pedirle a un modelo de IA que "revise qué falta probar" produce respuestas plausibles pero no verificables, y que cambian entre ejecuciones.
- **Decisión:** las herramientas no usan IA. Comparan artefactos reales con reglas explícitas y devuelven **siempre el mismo resultado para la misma entrada**, en JSON estructurado y con un orden estable. La IA usa esos resultados para explicar, priorizar con contexto y redactar.
- **Consecuencia:** cada hallazgo se puede verificar (por ejemplo: "no existe un escenario con `DELETE` y 404") y el análisis se puede repetir después de cada cambio. La lógica se prueba con TDD como cualquier otro código.

## ADR-004: Arquitectura en tres capas
- **Contexto:** la lógica de análisis debe probarse sin depender del protocolo ni del sistema de archivos.
- **Decisión:**

  | Capa | Responsabilidad | Puede depender de |
  |---|---|---|
  | `core` | Análisis: matriz, cobertura, riesgo, datos de prueba, trazabilidad, generación de escenarios. Funciones puras | Nada externo |
  | `sources` | Leer y convertir los artefactos de la API (OpenAPI, máquina de estados, riesgos, Karate, JUnit) en modelos de `core` | `core` |
  | `server` | Registrar herramientas y recursos en MCP, validar entradas y serializar resultados | `core`, `sources` |

  Una prueba de arquitectura verifica que `core` no importe el SDK de MCP ni el sistema de archivos.
- **Consecuencia:** el grueso de las pruebas son unitarias sobre `core`, rápidas y sin efectos laterales.

## ADR-005: Fuentes de verdad: los artefactos de la API
- **Contexto:** el análisis debe basarse en lo que el proyecto declara y prueba, no en suposiciones.
- **Decisión:** el servidor lee del repositorio `rtbc-orders-api`:

  | Artefacto | Qué aporta |
  |---|---|
  | `docs/api/openapi.yaml` | Endpoints, códigos de respuesta y restricciones de cada campo |
  | `docs/maquina-estados.md` | Matriz 5×5 de transiciones con el código HTTP esperado |
  | `docs/qa/estrategia-pruebas.md` | Matriz de riesgos R01 a R10 con su nivel |
  | `src/test/resources/acceptance/*.feature` | Escenarios de aceptación de Karate |
  | `src/test/java/**/*.java` | Pruebas JUnit etiquetadas con `@Tag("RN-xx")` |
  | `README.md` | Tabla de reglas de negocio RN-01 a RN-06 |

  La ruta del repositorio se configura con la variable `RTBC_API_REPO`. Si no se define, se usa el directorio de trabajo, que es la raíz de la API cuando Claude Code lanza el servidor desde allí.
- **Consecuencia:** el análisis refleja siempre el estado actual del repositorio. Si un artefacto cambia de formato, el lector correspondiente falla con un mensaje que indica qué archivo y qué estructura esperaba, en lugar de devolver un resultado silenciosamente incompleto.

## ADR-006: Lectura de escenarios Karate con el parser oficial de Gherkin
- **Contexto:** los `.feature` tienen etiquetas a nivel de feature, de escenario y de bloque `Examples`, además de `Scenario Outline` con tablas. Leerlos con expresiones regulares sería frágil.
- **Decisión:** se usa el parser oficial de Gherkin de Cucumber para obtener la estructura (feature, escenarios, pasos, ejemplos y etiquetas). Sobre esa estructura se interpretan los pasos de Karate que importan para la cobertura: `path`, `request { estado }`, `method`, `status` y la llamada al helper `orden-en-estado` o `crear-orden`, que determina el estado de origen.
- **Consecuencia:** los `Scenario Outline` se expanden fila por fila, así cada fila cuenta como una transición cubierta. **Limitación conocida:** una transición que se ejecuta dentro de una función JavaScript (como el flujo completo que usa `karate.map`) no se detecta. Se documenta en el resultado y no genera falsos vacíos en las transiciones del flujo principal, porque están cubiertas también por el `Scenario Outline`.

## ADR-007: Seguridad: solo lectura y dentro del repositorio
- **Contexto:** un servidor MCP ejecuta código en el equipo de quien lo usa y recibe parámetros que redacta una IA.
- **Decisión:**
  - Solo lee archivos; nunca escribe, borra ni ejecuta comandos.
  - Toda ruta se resuelve dentro del repositorio configurado; una ruta que salga de él (por ejemplo con `../`) se rechaza.
  - No usa red ni secretos.
  - Las entradas de cada herramienta se validan con esquemas estrictos antes de usarlas.
- **Consecuencia:** el peor caso de una entrada maliciosa es un error controlado. Cada regla tiene su prueba.

## ADR-008: Generar no es modificar
- **Contexto:** `generar_escenario_karate` y `generar_datos_prueba` producen contenido que podría agregarse a la API.
- **Decisión:** las herramientas **devuelven** el contenido generado; no lo escriben en el repositorio. Incorporarlo es una decisión humana, mediante un PR que pasa los quality gates de la API.
- **Consecuencia:** la IA propone y la persona decide. Cada escenario generado se valida antes de devolverlo: debe ser Gherkin válido y usar solo los helpers que existen en la API.

## ADR-009: Skill `/analisis-qa` como flujo de trabajo
- **Contexto:** las herramientas son piezas independientes; el valor aparece al encadenarlas en un orden con criterio.
- **Decisión:** una skill de Claude Code, `/analisis-qa`, que vive en el repositorio de la API junto al registro del servidor (`.mcp.json`) y define el flujo:
  1. detectar los escenarios faltantes;
  2. priorizarlos por riesgo;
  3. generar los escenarios y los datos de prueba de los más críticos;
  4. revisar la trazabilidad de las reglas;
  5. presentar un resumen con lo encontrado, lo propuesto y lo que requiere decisión humana.
- **Consecuencia:** el MCP aporta las capacidades y la skill aporta el método. La skill no incorpora cambios: los propone.

## ADR-010: Estrategia de pruebas y quality gates
- **Decisión:** TDD con Vitest. Las pruebas usan una **copia fija de los artefactos de la API `v1.0.0`** como datos de prueba, para que no dependan de cambios posteriores en la API. Quality gates: cobertura de líneas y ramas ≥ 80 %, mutation score ≥ 70 % (Stryker), compilación estricta sin errores y cero pruebas fallidas. Detalle en [qa/estrategia-pruebas.md](qa/estrategia-pruebas.md).

## ADR-011: Versionamiento
- **Decisión:** las mismas convenciones que la API: Conventional Commits en español, una rama por subfase, PR con merge commit y tags semánticos. Detalle en [CONTRIBUTING.md](../CONTRIBUTING.md).
