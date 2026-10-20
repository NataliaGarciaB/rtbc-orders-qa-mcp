@operacion
Feature: Operación: contrato publicado e id de correlación

  Background:
    * url baseUrl

  Scenario: El contrato de diseño se publica como YAML y Swagger UI está disponible
    # Riesgo R04 / ADR-018: MockMvc no aplica server.mime-mappings; el tipo se verifica en el servidor real
    Given path 'openapi.yaml'
    When method get
    Then status 200
    And match karate.response.header('Content-Type') contains 'application/yaml'
    And match response contains 'title: RTBC Orders API'
    Given path 'swagger-ui.html'
    When method get
    Then status 200
    And match response contains "url: '/openapi.yaml'"

  Scenario: Cada respuesta incluye un X-Request-Id generado si el cliente no lo envía
    # ADR-019: correlación entre la respuesta y los logs
    Given path 'orders'
    When method get
    Then status 200
    And match karate.response.header('X-Request-Id') == '#uuid'

  Scenario: Un X-Request-Id seguro enviado por el cliente se devuelve igual
    Given path 'orders'
    And header X-Request-Id = 'karate-req-1'
    When method get
    Then status 200
    And match karate.response.header('X-Request-Id') == 'karate-req-1'

  Scenario: Un X-Request-Id inseguro se reemplaza por uno generado
    # Evita inyección en los logs
    Given path 'orders'
    And header X-Request-Id = 'id con espacios'
    When method get
    Then status 200
    And match karate.response.header('X-Request-Id') == '#uuid'
