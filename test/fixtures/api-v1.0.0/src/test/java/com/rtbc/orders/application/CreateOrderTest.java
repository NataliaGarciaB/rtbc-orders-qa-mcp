package com.rtbc.orders.application;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import com.rtbc.orders.domain.OrderStatus;
import com.rtbc.orders.domain.ValidationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Caso de uso: crear orden")
class CreateOrderTest {

	@Mock
	private OrderRepository repository;

	@InjectMocks
	private CreateOrder createOrder;

	@Test
	@DisplayName("guarda la orden en estado CREADA con el total calculado")
	void guardaOrdenCreada() {
		Order persisted = OrderFixtures.validOrder();
		when(repository.save(any(Order.class))).thenReturn(persisted);

		Order result = createOrder.execute(CUSTOMER, List.of(line("P001", 2, "100000")));

		ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().status()).isEqualTo(OrderStatus.CREADA);
		assertThat(saved.getValue().customer()).isEqualTo(CUSTOMER);
		assertThat(saved.getValue().total()).isEqualByComparingTo("200000");
		assertThat(result).as("devuelve lo que persiste el repositorio").isSameAs(persisted);
	}

	@Test
	@DisplayName("no guarda nada si los datos son inválidos")
	void noGuardaOrdenInvalida() {
		assertThatThrownBy(() -> createOrder.execute(CUSTOMER, List.of()))
				.isInstanceOf(ValidationException.class);

		verifyNoInteractions(repository);
	}
}
