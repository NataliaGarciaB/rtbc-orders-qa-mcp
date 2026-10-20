@ignore
Feature: Helper: cambiar el estado de una orden

  # Uso: call read('classpath:acceptance/helpers/cambiar-estado.feature') { orderId: '...', estado: 'PAGADA' }
  # No valida el status: lo decide quien llama (responseStatus y response quedan disponibles)

  Scenario:
    Given url baseUrl
    And path 'orders', orderId, 'status'
    And request { estado: '#(estado)' }
    When method patch
