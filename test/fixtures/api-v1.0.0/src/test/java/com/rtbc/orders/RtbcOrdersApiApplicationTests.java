package com.rtbc.orders;

import static org.assertj.core.api.Assertions.assertThat;

import com.rtbc.orders.application.CancelOrder;
import com.rtbc.orders.application.ChangeOrderStatus;
import com.rtbc.orders.application.CreateOrder;
import com.rtbc.orders.application.GetOrder;
import com.rtbc.orders.application.ListOrders;
import com.rtbc.orders.application.OrderRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
@DisplayName("Arranque de la aplicación")
class RtbcOrdersApiApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("conecta el puerto de persistencia con su adaptador")
	void conectaRepositorio() {
		assertThat(context.getBean(OrderRepository.class)).isNotNull();
	}

	@Test
	@DisplayName("registra los casos de uso como servicios transaccionales")
	void registraCasosDeUso() {
		Stream.of(CreateOrder.class, GetOrder.class, ListOrders.class, ChangeOrderStatus.class, CancelOrder.class)
				.map(context::getBean)
				.forEach(useCase -> assertThat(AopUtils.isAopProxy(useCase))
						.as("%s debe ejecutarse dentro de una transacción", useCase.getClass().getSimpleName())
						.isTrue());
	}
}
