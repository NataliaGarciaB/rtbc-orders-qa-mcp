@consultar
Feature: Consultar órdenes (GET /orders/{id})

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @critico
  Scenario: Consultar una orden existente
    Given def existente = call read('classpath:acceptance/helpers/crear-orden.feature')
    And path 'orders', existente.orderId
    When method get
    Then status 200
    And match response == schemas.orden
    And match response == existente.order

  Scenario: Consultar una orden inexistente responde 404
    # Riesgo R05: un id válido que no existe no debe provocar un 500
    Given path 'orders', '00000000-0000-0000-0000-000000000000'
    When method get
    Then status 404
    And match response == schemas.problema
    And match response.codigo == 'ORDEN_NO_ENCONTRADA'

  @listar
  Scenario: Listar órdenes incluye la orden creada
    Given def creada = call read('classpath:acceptance/helpers/crear-orden.feature')
    And path 'orders'
    When method get
    Then status 200
    And match each response == schemas.orden
    And match response[*].id contains creada.orderId
