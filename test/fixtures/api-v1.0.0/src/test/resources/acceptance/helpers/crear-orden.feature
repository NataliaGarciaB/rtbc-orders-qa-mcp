@ignore
Feature: Helper: crear una orden

  # Uso: call read('classpath:acceptance/helpers/crear-orden.feature') { body: <opcional> }
  # Devuelve: order (respuesta) y orderId

  Scenario:
    * def payload = karate.get('body', read('classpath:acceptance/data/orden-valida.json'))
    Given url baseUrl
    And path 'orders'
    And request payload
    When method post
    Then status 201
    * def order = response
    * def orderId = response.id
