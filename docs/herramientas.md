# Catálogo de herramientas y recursos

Contrato de cada herramienta del servidor MCP: qué hace, qué recibe, qué devuelve y de qué artefactos de la API depende. Todas son deterministas ([ADR-003](decisiones.md#adr-003-herramientas-deterministas)) y de solo lectura ([ADR-007](decisiones.md#adr-007-seguridad-solo-lectura-y-dentro-del-repositorio)).

Cada herramienta devuelve el resultado estructurado (JSON) y, además, un resumen en texto para que la persona lo lea sin procesar el JSON.

| Herramienta | Para qué sirve |
|---|---|
| [`generar_matriz_transiciones`](#generar_matriz_transiciones) | Conocer el comportamiento esperado de cada transición de estado |
| [`detectar_escenarios_faltantes`](#detectar_escenarios_faltantes) | Identificar qué transiciones y qué respuestas del contrato no tienen escenario de aceptación |
| [`priorizar_por_riesgo`](#priorizar_por_riesgo) | Ordenar los vacíos según la matriz de riesgos del proyecto |
| [`generar_escenario_karate`](#generar_escenario_karate) | Generar el escenario de aceptación para un vacío |
| [`generar_datos_prueba`](#generar_datos_prueba) | Obtener valores límite válidos e inválidos para cada campo del contrato |
| [`construir_trazabilidad`](#construir_trazabilidad) | Relacionar cada regla de negocio con las pruebas que la cubren |

---

## `generar_matriz_transiciones`

**Fuente:** `docs/maquina-estados.md` (tabla "Matriz completa de transiciones").

**Entrada:** ninguna.

**Salida:**

```json
{
  "estados": ["CREADA", "PAGADA", "ENVIADA", "ENTREGADA", "CANCELADA"],
  "transiciones": [
    { "origen": "CREADA", "destino": "PAGADA", "http": 200, "valida": true },
    { "origen": "CREADA", "destino": "ENVIADA", "http": 400, "valida": false, "regla": "RN-05" },
    { "origen": "ENTREGADA", "destino": "CANCELADA", "http": 409, "valida": false, "regla": "RN-04" }
  ],
  "resumen": { "total": 25, "validas": 5, "http400": 1, "http409": 19 }
}
```

`transiciones` contiene las 25 combinaciones, ordenadas por estado de origen y de destino.

**Errores:** si la tabla no existe o no tiene 5×5 celdas reconocibles, devuelve un error que indica el archivo y la estructura esperada.

---

## `detectar_escenarios_faltantes`

**Fuentes:** la matriz de transiciones, `docs/api/openapi.yaml` y `src/test/resources/acceptance/*.feature`.

**Entrada:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `alcance` | `"transiciones"` · `"contrato"` · `"todo"` | No (por defecto `"todo"`) | Qué comparar |

**Qué compara:**
- **Transiciones:** cada una de las 25 combinaciones contra los escenarios que llevan una orden a un estado de origen (helper `orden-en-estado` o `crear-orden`) y luego ejecutan `PATCH` con un estado o `DELETE`. Los `Scenario Outline` se expanden fila por fila.
- **Contrato:** cada respuesta declarada en el contrato (método, ruta y código) contra los pares `method` y `status` de los escenarios.

**Salida:**

```json
{
  "cobertura": {
    "transiciones": { "cubiertas": 10, "total": 25 },
    "respuestas": { "cubiertas": 14, "total": 18 }
  },
  "faltantes": [
    { "tipo": "transicion", "origen": "ENTREGADA", "destino": "PAGADA", "metodo": "PATCH", "httpEsperado": 409 },
    { "tipo": "respuesta", "metodo": "DELETE", "ruta": "/orders/{id}", "http": 404 }
  ],
  "advertencias": ["Transiciones ejecutadas dentro de funciones JavaScript no se detectan (ver ADR-006)"]
}
```

Las cifras del ejemplo son ilustrativas; las reales las calcula la herramienta.

---

## `priorizar_por_riesgo`

**Fuentes:** el resultado de `detectar_escenarios_faltantes` y la matriz de riesgos de `docs/qa/estrategia-pruebas.md`.

**Entrada:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `limite` | entero ≥ 1 | No | Cantidad máxima de vacíos a devolver |

**Cómo asigna el riesgo:**

| Vacío | Riesgo | Motivo |
|---|---|---|
| Transición involucrada en RN-04 o RN-05 | R01 | Regla de negocio de mayor impacto |
| Cualquier otra transición | R01 | Una transición inválida aceptada corrompe el ciclo de vida |
| Respuesta 400 de validación | R03 | Una orden inválida podría crearse |
| Respuesta 404 | R05 | Un id inexistente no debe provocar un 500 |
| Otras respuestas | R04 | Contrato inconsistente para los clientes |

El nivel (Alto, Medio, Bajo) y su puntaje se leen de la matriz de riesgos; las reglas de asignación están en el código y tienen sus pruebas.

**Salida:** los vacíos ordenados por puntaje de riesgo descendente; a igual puntaje, en orden estable por tipo, método, ruta y estados.

```json
{
  "priorizados": [
    { "riesgo": "R01", "nivel": "Alto", "puntaje": 9, "vacio": { "tipo": "transicion", "origen": "ENTREGADA", "destino": "PAGADA", "metodo": "PATCH", "httpEsperado": 409 } }
  ]
}
```

---

## `generar_escenario_karate`

**Fuente:** los helpers existentes en `src/test/resources/acceptance/helpers/`.

**Entrada:** un vacío con la misma forma que devuelve `detectar_escenarios_faltantes`.

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `tipo` | `"transicion"` · `"respuesta"` | Sí | Tipo de vacío |
| `metodo` | `"GET"` · `"POST"` · `"PATCH"` · `"DELETE"` | Sí | Método HTTP |
| `ruta` | texto | Para `respuesta` | Ruta del contrato, por ejemplo `/orders/{id}` |
| `origen`, `destino` | estado | Para `transicion` | Estados de la transición |
| `http` / `httpEsperado` | entero | Sí | Código esperado |

**Salida:**

```json
{
  "featureSugerido": "transiciones.feature",
  "etiquetas": ["@RN-06"],
  "escenario": "  @RN-06\n  Scenario: Transición ENTREGADA → PAGADA responde 409\n    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'ENTREGADA' }\n    ..."
}
```

**Garantías:** el escenario generado es Gherkin válido (se verifica con el parser antes de devolverlo), usa solo helpers existentes y valida el `codigo` del error cuando corresponde. **No se escribe en la API** ([ADR-008](decisiones.md#adr-008-generar-no-es-modificar)).

---

## `generar_datos_prueba`

**Fuente:** `docs/api/openapi.yaml` (esquemas y restricciones).

**Entrada:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `esquema` | texto | No (por defecto todos los de request) | Esquema del contrato, por ejemplo `CreateOrderRequest` |

**Qué genera por campo**, según sus restricciones:

| Restricción | Valores válidos | Valores inválidos |
|---|---|---|
| `minLength` / `maxLength` | longitud mínima y máxima | vacío, mínimo − 1, máximo + 1 |
| `minimum` / `exclusiveMinimum` | el mínimo permitido | el valor justo por debajo |
| `multipleOf` (decimales) | valor con el máximo de decimales | un decimal más |
| `enum` | cada valor | un valor fuera del enum |
| `minItems` | la lista mínima | la lista vacía |
| `required` | — | el campo ausente |

**Salida:**

```json
{
  "esquema": "CreateOrderRequest",
  "campos": [
    { "campo": "cliente", "validos": ["a", "aaa…(120)"], "invalidos": [{ "valor": "", "motivo": "minLength 1" }, { "valor": "aaa…(121)", "motivo": "maxLength 120" }] }
  ]
}
```

---

## `construir_trazabilidad`

**Fuentes:** la tabla de reglas de `README.md`, las pruebas JUnit (`@Tag("RN-xx")`) y los escenarios Karate (`@RN-xx`).

**Entrada:** ninguna.

**Salida:** por cada regla, las pruebas que la cubren en cada nivel y las reglas sin cobertura en algún nivel.

```json
{
  "reglas": [
    {
      "regla": "RN-05",
      "descripcion": "Una orden no puede ser enviada si no ha sido pagada",
      "junit": ["OrderStatusTransitionTest.ordenNoPagadaNoSeEnvia", "ChangeOrderStatusTest.enviarSinPagar"],
      "karate": ["transiciones.feature: Intentar enviar una orden no pagada"],
      "niveles": { "unitario": true, "aceptacion": true }
    }
  ],
  "sinCobertura": []
}
```

---

## Recursos

Además de las herramientas, el servidor expone como recursos MCP de solo lectura:

| URI | Contenido |
|---|---|
| `rtbc://reglas-negocio` | Tabla de reglas RN-01 a RN-06 |
| `rtbc://contrato` | `docs/api/openapi.yaml` |
| `rtbc://maquina-estados` | `docs/maquina-estados.md` |

## Alcance y limitaciones

- Las herramientas basadas en el contrato OpenAPI (`generar_datos_prueba` y la parte de contrato de `detectar_escenarios_faltantes`) funcionan con cualquier API documentada con OpenAPI.
- Las basadas en la máquina de estados y la trazabilidad dependen de las convenciones de este proyecto: la tabla de `maquina-estados.md`, los helpers de Karate y las etiquetas `@RN-xx`.
- Las transiciones ejecutadas dentro de funciones JavaScript de Karate no se detectan ([ADR-006](decisiones.md#adr-006-lectura-de-escenarios-karate-con-el-parser-oficial-de-gherkin)).
