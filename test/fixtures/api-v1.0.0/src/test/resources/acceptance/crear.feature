@crear
Feature: Crear orden (POST /orders)

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @critico
  Scenario: Crear una orden válida
    Given path 'orders'
    And request read('classpath:acceptance/data/orden-valida.json')
    When method post
    Then status 201
    And match response == schemas.orden
    And match each response.productos == schemas.producto
    And match response.estado == 'CREADA'
    And match response.total == 200000
    And match karate.response.header('Location') contains '/orders/' + response.id

  @RN-03
  Scenario: El total se calcula en el servidor e ignora total y estado enviados por el cliente
    # Riesgo R02: el cliente no puede manipular el total ni el estado inicial
    Given path 'orders'
    And request
      """
      {
        "cliente": "Juan Pérez",
        "productos": [ { "id": "P001", "cantidad": 2, "valorUnitario": 100000 } ],
        "total": 1,
        "estado": "ENTREGADA"
      }
      """
    When method post
    Then status 201
    And match response.total == 200000
    And match response.estado == 'CREADA'

  @RN-03
  Scenario: El total de varias líneas con decimales es exacto
    # Riesgo R02: sin errores de redondeo (BigDecimal de punta a punta, incluida la base de datos)
    Given path 'orders'
    And request
      """
      {
        "cliente": "Juan Pérez",
        "productos": [
          { "id": "P001", "cantidad": 3, "valorUnitario": 10.50 },
          { "id": "P002", "cantidad": 1, "valorUnitario": 0.99 }
        ]
      }
      """
    When method post
    Then status 201
    And match response.total == 32.49
    # El valor persistido es el mismo
    Given path 'orders', response.id
    When method get
    Then status 200
    And match response.total == 32.49
