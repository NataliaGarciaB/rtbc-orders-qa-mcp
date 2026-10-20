@validaciones
Feature: Validaciones de entrada en la API desplegada

  # Riesgo R03: una orden inválida no debe crearse en el artefacto real.
  # Las combinaciones completas y los valores límite están en RequestValidationTest y en el dominio;
  # aquí se confirma una muestra representativa por regla en el sistema integrado.

  Background:
    * url baseUrl
    * def schemas = call read('classpath:acceptance/data/schemas.js')

  Scenario Outline: Crear una orden inválida responde 400 con el campo inválido: <caso>
    Given path 'orders'
    And request <body>
    When method post
    Then status 400
    And match response == schemas.problema
    And match response.codigo == 'VALIDACION'
    And match response.errores[*].campo contains '<campo>'

    @RN-01
    Examples:
      | caso         | body                                                                                     | campo     |
      | sin productos | { cliente: 'Juan Pérez', productos: [] }                                                | productos |

    @RN-02
    Examples:
      | caso       | body                                                                                        | campo                 |
      | cantidad 0 | { cliente: 'Juan Pérez', productos: [ { id: 'P001', cantidad: 0, valorUnitario: 100 } ] }   | productos[0].cantidad |

    Examples:
      | caso                    | body                                                                                         | campo                       |
      | cliente vacío           | { cliente: '  ', productos: [ { id: 'P001', cantidad: 1, valorUnitario: 100 } ] }            | cliente                     |
      | valor unitario 0        | { cliente: 'Juan Pérez', productos: [ { id: 'P001', cantidad: 1, valorUnitario: 0 } ] }      | productos[0].valorUnitario  |
      | valor con 3 decimales   | { cliente: 'Juan Pérez', productos: [ { id: 'P001', cantidad: 1, valorUnitario: 10.555 } ] } | productos[0].valorUnitario  |

  Scenario: Actualizar a un estado desconocido responde 400 con el campo estado
    Given def orden = call read('classpath:acceptance/helpers/crear-orden.feature')
    And path 'orders', orden.orderId, 'status'
    And request { estado: 'PERDIDA' }
    When method patch
    Then status 400
    And match response.codigo == 'VALIDACION'
    And match response.errores[*].campo contains 'estado'

  Scenario: Un id que no es UUID responde 400
    Given path 'orders', 'abc'
    When method get
    Then status 400
    And match response.codigo == 'VALIDACION'
