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
import com.rtbc.orders.domain.OrderFixtures;
import com.rtbc.orders.domain.OrderNotPaidException;
import com.rtbc.orders.domain.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("RN-06")
@DisplayName("Caso de uso: actualizar estado")
class ChangeOrderStatusTest {

	@Mock
	private OrderRepository repository;

	@InjectMocks
	private ChangeOrderStatus changeOrderStatus;

	@Test
	@DisplayName("aplica una transición válida y guarda la orden")
	void transicionValida() {
		Order order = orderIn(OrderStatus.CREADA);
		Order persisted = OrderFixtures.validOrder();
		when(repository.findById(order.id())).thenReturn(Optional.of(order));
		when(repository.save(order)).thenReturn(persisted);

		Order result = changeOrderStatus.execute(order.id(), OrderStatus.PAGADA);

		assertThat(order.status()).isEqualTo(OrderStatus.PAGADA);
		verify(repository).save(order);
		assertThat(result).as("devuelve lo que persiste el repositorio").isSameAs(persisted);
	}

	@Test
	@DisplayName("lanza ORDEN_NO_ENCONTRADA y no guarda si la orden no existe")
	void ordenInexistente() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> changeOrderStatus.execute(id, OrderStatus.PAGADA))
				.isInstanceOf(OrderNotFoundException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@Tag("RN-05")
	@DisplayName("RN-05: no guarda si se intenta enviar una orden no pagada")
	void enviarSinPagar() {
		Order order = orderIn(OrderStatus.CREADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> changeOrderStatus.execute(order.id(), OrderStatus.ENVIADA))
				.isInstanceOf(OrderNotPaidException.class);
		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("no guarda si la transición es inválida")
	void transicionInvalida() {
		Order order = orderIn(OrderStatus.CANCELADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> changeOrderStatus.execute(order.id(), OrderStatus.PAGADA))
				.isInstanceOf(InvalidTransitionException.class);
		verify(repository, never()).save(any());
	}
}
