package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.orderIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Matriz completa de docs/maquina-estados.md: 5 válidas, 1 → ORDEN_NO_PAGADA, 19 → TRANSICION_INVALIDA.
 */
@Tag("RN-06")
@DisplayName("RN-06: transiciones de estado de la orden")
class OrderStatusTransitionTest {

	private static final String OK = "OK";

	@ParameterizedTest(name = "{0} → {1}: {2}")
	@CsvSource({
			"CREADA,    CREADA,    TRANSICION_INVALIDA",
			"CREADA,    PAGADA,    OK",
			"CREADA,    ENVIADA,   ORDEN_NO_PAGADA",
			"CREADA,    ENTREGADA, TRANSICION_INVALIDA",
			"CREADA,    CANCELADA, OK",
			"PAGADA,    CREADA,    TRANSICION_INVALIDA",
			"PAGADA,    PAGADA,    TRANSICION_INVALIDA",
			"PAGADA,    ENVIADA,   OK",
			"PAGADA,    ENTREGADA, TRANSICION_INVALIDA",
			"PAGADA,    CANCELADA, OK",
			"ENVIADA,   CREADA,    TRANSICION_INVALIDA",
			"ENVIADA,   PAGADA,    TRANSICION_INVALIDA",
			"ENVIADA,   ENVIADA,   TRANSICION_INVALIDA",
			"ENVIADA,   ENTREGADA, OK",
			"ENVIADA,   CANCELADA, OK",
			"ENTREGADA, CREADA,    TRANSICION_INVALIDA",
			"ENTREGADA, PAGADA,    TRANSICION_INVALIDA",
			"ENTREGADA, ENVIADA,   TRANSICION_INVALIDA",
			"ENTREGADA, ENTREGADA, TRANSICION_INVALIDA",
			"ENTREGADA, CANCELADA, TRANSICION_INVALIDA",
			"CANCELADA, CREADA,    TRANSICION_INVALIDA",
			"CANCELADA, PAGADA,    TRANSICION_INVALIDA",
			"CANCELADA, ENVIADA,   TRANSICION_INVALIDA",
			"CANCELADA, ENTREGADA, TRANSICION_INVALIDA",
			"CANCELADA, CANCELADA, TRANSICION_INVALIDA"
	})
	void matrizDeTransiciones(OrderStatus from, OrderStatus to, String expected) {
		Order order = orderIn(from);

		if (OK.equals(expected)) {
			order.changeStatus(to);
			assertThat(order.status()).isEqualTo(to);
		} else {
			assertThatThrownBy(() -> order.changeStatus(to))
					.isInstanceOfSatisfying(DomainException.class,
							e -> assertThat(e.code()).isEqualTo(ErrorCode.valueOf(expected)));
			assertThat(order.status()).as("una transición rechazada no cambia el estado").isEqualTo(from);
		}
	}

	@Test
	@Tag("RN-04")
	@DisplayName("RN-04: una orden entregada no puede ser cancelada")
	void ordenEntregadaNoSeCancela() {
		Order order = orderIn(OrderStatus.ENTREGADA);

		assertThatThrownBy(() -> order.changeStatus(OrderStatus.CANCELADA))
				.isInstanceOf(InvalidTransitionException.class)
				.hasMessage("No se puede pasar de ENTREGADA a CANCELADA");
	}

	@Test
	@Tag("RN-05")
	@DisplayName("RN-05: una orden no puede ser enviada si no ha sido pagada")
	void ordenNoPagadaNoSeEnvia() {
		Order order = orderIn(OrderStatus.CREADA);

		assertThatThrownBy(() -> order.changeStatus(OrderStatus.ENVIADA))
				.isInstanceOf(OrderNotPaidException.class)
				.hasMessage("La orden no puede ser enviada si no ha sido pagada");
	}

	@Test
	@DisplayName("recorre el flujo completo CREADA → PAGADA → ENVIADA → ENTREGADA")
	void flujoCompleto() {
		Order order = OrderFixtures.validOrder();

		order.changeStatus(OrderStatus.PAGADA);
		order.changeStatus(OrderStatus.ENVIADA);
		order.changeStatus(OrderStatus.ENTREGADA);

		assertThat(order.status()).isEqualTo(OrderStatus.ENTREGADA);
	}

	@Test
	@DisplayName("rechaza un estado destino ausente")
	void rechazaEstadoNulo() {
		Order order = OrderFixtures.validOrder();

		assertThatThrownBy(() -> order.changeStatus(null))
				.isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.field()).isEqualTo("estado"));
		assertThat(order.status()).isEqualTo(OrderStatus.CREADA);
	}
}
