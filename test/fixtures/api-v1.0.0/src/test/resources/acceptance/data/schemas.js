function fn() {
  // Estructura del contrato (docs/api/openapi.yaml) con marcadores de Karate: se valida la forma, no valores frágiles
  return {
    orden: {
      id: '#uuid',
      cliente: '#string',
      productos: '#[_ > 0]',
      total: '#number',
      estado: '#regex (CREADA|PAGADA|ENVIADA|ENTREGADA|CANCELADA)'
    },
    producto: {
      id: '#string',
      cantidad: '#number',
      valorUnitario: '#number'
    },
    problema: {
      type: '##string',
      title: '#string',
      status: '#number',
      detail: '#string',
      instance: '#string',
      codigo: '#string',
      errores: '##[]'
    }
  };
}
