@ignore
Feature: Helper: crear una orden y llevarla a un estado por el camino válido

  # Uso: call read('classpath:acceptance/helpers/orden-en-estado.feature') { estado: 'ENTREGADA' }
  # Devuelve: orderId

  Scenario:
    * def caminos =
      """
      {
        CREADA: [],
        PAGADA: ['PAGADA'],
        ENVIADA: ['PAGADA', 'ENVIADA'],
        ENTREGADA: ['PAGADA', 'ENVIADA', 'ENTREGADA'],
        CANCELADA: ['CANCELADA']
      }
      """
    * def creada = call read('classpath:acceptance/helpers/crear-orden.feature')
    * def orderId = creada.orderId
    * def avanzar =
      """
      function(destino) {
        var r = karate.call('classpath:acceptance/helpers/cambiar-estado.feature', { orderId: orderId, estado: destino });
        if (r.responseStatus != 200) karate.fail('no se pudo llevar la orden a ' + destino + ': ' + r.responseStatus);
      }
      """
    * karate.forEach(caminos[estado], avanzar)
