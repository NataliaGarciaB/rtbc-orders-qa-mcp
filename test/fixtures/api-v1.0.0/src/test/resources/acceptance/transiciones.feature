@transiciones
Feature: Transiciones de estado (PATCH /orders/{id}/status y DELETE /orders/{id})

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  @critico @RN-05
  Scenario: Intentar enviar una orden no pagada
    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'CREADA' }
    And path 'orders', orden.orderId, 'status'
    And request { estado: 'ENVIADA' }
    When method patch
    Then status 400
    And match response == schemas.problema
    And match response.codigo == 'ORDEN_NO_PAGADA'

  @critico @RN-04
  Scenario: Intentar cancelar una orden entregada
    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'ENTREGADA' }
    And path 'orders', orden.orderId
    When method delete
    Then status 409
    And match response == schemas.problema
    And match response.codigo == 'TRANSICION_INVALIDA'
    # La orden no cambia
    Given path 'orders', orden.orderId
    When method get
    Then status 200
    And match response.estado == 'ENTREGADA'

  @RN-06
  Scenario Outline: Transición <origen> → <destino> responde <codigoHttp>
    # Riesgo R01: muestra representativa de la matriz 5×5 (docs/maquina-estados.md).
    # Las 25 combinaciones se prueban completas en OrderStatusTransitionTest.
    Given def orden = call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: '<origen>' }
    And path 'orders', orden.orderId, 'status'
    And request { estado: '<destino>' }
    When method patch
    Then status <codigoHttp>
    And match response.estado == <estadoEsperado>
    And match response.codigo == <codigoError>

    Examples: transiciones válidas
      | origen  | destino   | codigoHttp | estadoEsperado | codigoError |
      | CREADA  | PAGADA    | 200        | 'PAGADA'       | '#notpresent' |
      | PAGADA  | ENVIADA   | 200        | 'ENVIADA'      | '#notpresent' |
      | ENVIADA | ENTREGADA | 200        | 'ENTREGADA'    | '#notpresent' |
      | PAGADA  | CANCELADA | 200        | 'CANCELADA'    | '#notpresent' |

    Examples: transiciones inválidas
      | origen    | destino   | codigoHttp | estadoEsperado | codigoError           |
      | CREADA    | ENTREGADA | 409        | '#notpresent'  | 'TRANSICION_INVALIDA' |
      | PAGADA    | PAGADA    | 409        | '#notpresent'  | 'TRANSICION_INVALIDA' |
      | ENVIADA   | PAGADA    | 409        | '#notpresent'  | 'TRANSICION_INVALIDA' |
      | CANCELADA | PAGADA    | 409        | '#notpresent'  | 'TRANSICION_INVALIDA' |

  @RN-06
  Scenario: Flujo completo CREADA → PAGADA → ENVIADA → ENTREGADA
    Given def creada = call read('classpath:acceptance/helpers/crear-orden.feature')
    * def orderId = creada.orderId
    * def estados = ['PAGADA', 'ENVIADA', 'ENTREGADA']
    * def resultados = karate.map(estados, function(e){ return karate.call('classpath:acceptance/helpers/cambiar-estado.feature', { orderId: orderId, estado: e }).responseStatus })
    Then match resultados == [200, 200, 200]
    Given path 'orders', orderId
    When method get
    Then status 200
    And match response.estado == 'ENTREGADA'
