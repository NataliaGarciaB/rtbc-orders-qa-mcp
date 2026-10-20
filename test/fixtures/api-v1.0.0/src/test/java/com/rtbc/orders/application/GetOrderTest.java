package com.rtbc.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rtbc.orders.domain.ErrorCode;
import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Caso de uso: consultar orden")
class GetOrderTest {

	@Mock
	private OrderRepository repository;

	@InjectMocks
	private GetOrder getOrder;

	@Test
	@DisplayName("devuelve la orden existente")
	void devuelveOrdenExistente() {
		Order order = OrderFixtures.validOrder();
		when(repository.findById(order.id())).thenReturn(Optional.of(order));

		assertThat(getOrder.execute(order.id())).isSameAs(order);
	}

	@Test
	@DisplayName("lanza ORDEN_NO_ENCONTRADA si la orden no existe")
	void ordenInexistente() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> getOrder.execute(id))
				.isInstanceOfSatisfying(OrderNotFoundException.class,
						e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDEN_NO_ENCONTRADA))
				.hasMessage("No existe la orden " + id);
	}
}
