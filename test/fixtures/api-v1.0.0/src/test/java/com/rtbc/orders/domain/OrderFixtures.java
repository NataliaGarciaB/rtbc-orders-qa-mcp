package com.rtbc.orders.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Datos de prueba legibles: cada prueba parte de una orden válida y cambia solo lo que prueba.
 */
public final class OrderFixtures {

	public static final String CUSTOMER = "Juan Pérez";

	private OrderFixtures() {
	}

	public static ProductLine line() {
		return line("P001", 2, "100000");
	}

	public static ProductLine line(String productId, int quantity, String unitPrice) {
		return new ProductLine(productId, quantity, new BigDecimal(unitPrice));
	}

	public static Order validOrder() {
		return Order.create(CUSTOMER, List.of(line()));
	}

	/**
	 * Orden llevada al estado pedido por el camino válido de la máquina de estados.
	 */
	public static Order orderIn(OrderStatus status) {
		Order order = validOrder();
		PATHS.get(status).forEach(order::changeStatus);
		return order;
	}

	private static final Map<OrderStatus, List<OrderStatus>> PATHS = Map.of(
			OrderStatus.CREADA, List.of(),
			OrderStatus.PAGADA, List.of(OrderStatus.PAGADA),
			OrderStatus.ENVIADA, List.of(OrderStatus.PAGADA, OrderStatus.ENVIADA),
			OrderStatus.ENTREGADA, List.of(OrderStatus.PAGADA, OrderStatus.ENVIADA, OrderStatus.ENTREGADA),
			OrderStatus.CANCELADA, List.of(OrderStatus.CANCELADA));
}
