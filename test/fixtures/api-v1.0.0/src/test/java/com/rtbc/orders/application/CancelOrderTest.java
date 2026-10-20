package com.rtbc.orders.application;

import static com.rtbc.orders.domain.OrderFixtures.orderIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rtbc.orders.domain.InvalidTransitionException;
import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cancelar reutiliza la misma regla que PATCH a CANCELADA (ADR-006): se prueba con el caso de uso real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Caso de uso: cancelar orden")
class CancelOrderTest {

	@Mock
	private OrderRepository repository;

	private CancelOrder cancelOrder;

	@BeforeEach
	void setUp() {
		cancelOrder = new CancelOrder(new ChangeOrderStatus(repository));
	}

	@Test
	@DisplayName("cancela una orden creada y la guarda")
	void cancelaOrdenCreada() {
		Order order = orderIn(OrderStatus.CREADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));
		when(repository.save(order)).thenReturn(order);

		Order result = cancelOrder.execute(order.id());

		assertThat(result.status()).isEqualTo(OrderStatus.CANCELADA);
		verify(repository).save(order);
	}

	@Test
	@Tag("RN-04")
	@DisplayName("RN-04: no cancela ni guarda una orden entregada")
	void noCancelaOrdenEntregada() {
		Order order = orderIn(OrderStatus.ENTREGADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> cancelOrder.execute(order.id()))
				.isInstanceOf(InvalidTransitionException.class);
		assertThat(order.status()).isEqualTo(OrderStatus.ENTREGADA);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("lanza ORDEN_NO_ENCONTRADA si la orden no existe")
	void ordenInexistente() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cancelOrder.execute(id)).isInstanceOf(OrderNotFoundException.class);
	}
}
