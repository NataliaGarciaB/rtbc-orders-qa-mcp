package com.rtbc.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Caso de uso: listar órdenes")
class ListOrdersTest {

	@Mock
	private OrderRepository repository;

	@InjectMocks
	private ListOrders listOrders;

	@Test
	@DisplayName("devuelve todas las órdenes del repositorio")
	void devuelveTodas() {
		List<Order> orders = List.of(OrderFixtures.validOrder(), OrderFixtures.validOrder());
		when(repository.findAll()).thenReturn(orders);

		assertThat(listOrders.execute()).isEqualTo(orders);
	}

	@Test
	@DisplayName("devuelve una lista vacía si no hay órdenes")
	void listaVacia() {
		when(repository.findAll()).thenReturn(List.of());

		assertThat(listOrders.execute()).isEmpty();
	}
}
