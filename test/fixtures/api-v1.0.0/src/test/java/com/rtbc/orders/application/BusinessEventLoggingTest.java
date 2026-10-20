package com.rtbc.orders.application;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static com.rtbc.orders.domain.OrderFixtures.orderIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderNotPaidException;
import com.rtbc.orders.domain.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Observabilidad: cada evento de negocio deja un log con formato clave=valor, sin datos personales.
 */
@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
@DisplayName("Logs de eventos de negocio")
class BusinessEventLoggingTest {

	@Mock
	private OrderRepository repository;

	@Test
	@DisplayName("registra la creación de la orden con id, total y cantidad de productos, sin el cliente")
	void registraCreacion(CapturedOutput output) {
		when(repository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Order order = new CreateOrder(repository).execute(CUSTOMER, List.of(line("P001", 2, "100000")));

		assertThat(output.getOut())
				.contains("INFO")
				.contains("Orden creada: id=" + order.id() + " total=200000.00 productos=1")
				.doesNotContain(CUSTOMER);
	}

	@Test
	@DisplayName("registra el cambio de estado con el estado de origen y de destino")
	void registraCambioDeEstado(CapturedOutput output) {
		Order order = orderIn(OrderStatus.CREADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));
		when(repository.save(order)).thenReturn(order);

		new ChangeOrderStatus(repository).execute(order.id(), OrderStatus.PAGADA);

		assertThat(output.getOut())
				.contains("INFO")
				.contains("Estado cambiado: id=" + order.id() + " de=CREADA a=PAGADA");
	}

	@Test
	@DisplayName("registra como advertencia una transición rechazada con su código")
	void registraRechazo(CapturedOutput output) {
		Order order = orderIn(OrderStatus.CREADA);
		when(repository.findById(order.id())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> new ChangeOrderStatus(repository).execute(order.id(), OrderStatus.ENVIADA))
				.isInstanceOf(OrderNotPaidException.class);

		assertThat(output.getOut())
				.contains("WARN")
				.contains("Transición rechazada: id=" + order.id() + " de=CREADA a=ENVIADA codigo=ORDEN_NO_PAGADA");
	}
}
