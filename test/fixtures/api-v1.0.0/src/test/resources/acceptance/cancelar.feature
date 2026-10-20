@cancelar
Feature: Cancelar orden (DELETE /orders/{id})

  # ADR-006: cancelación lógica; misma regla que PATCH a CANCELADA

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  Scenario: Cancelar una orden creada responde 200 y la orden sigue consultable
    Given def orden = call read('classpath:acceptance/helpers/crear-orden.feature')
    And path 'orders', orden.orderId
    When method delete
    Then status 200
    And match response.estado == 'CANCELADA'
    # No se borra: la orden sigue existiendo en estado CANCELADA
    Given path 'orders', orden.orderId
    When method get
    Then status 200
    And match response.estado == 'CANCELADA'

  @RN-06
  Scenario: Cancelar una orden ya cancelada responde 409
    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'CANCELADA' }
    And path 'orders', orden.orderId
    When method delete
    Then status 409
    And match response.codigo == 'TRANSICION_INVALIDA'
