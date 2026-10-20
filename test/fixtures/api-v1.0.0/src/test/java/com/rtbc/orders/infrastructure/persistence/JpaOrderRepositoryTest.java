package com.rtbc.orders.infrastructure.persistence;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import com.rtbc.orders.domain.OrderStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaOrderRepository.class)
@DisplayName("Adaptador JPA de órdenes (H2)")
class JpaOrderRepositoryTest {

	@Autowired
	private JpaOrderRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("guarda y recupera la orden con sus líneas, total y estado")
	void guardaYRecupera() {
		Order order = Order.create(CUSTOMER, List.of(
				line("P001", 2, "100000"),
				line("P002", 3, "10.50")));

		repository.save(order);
		Order found = reload(order.id());

		assertThat(found.id()).isEqualTo(order.id());
		assertThat(found.customer()).isEqualTo(CUSTOMER);
		assertThat(found.status()).isEqualTo(OrderStatus.CREADA);
		assertThat(found.products()).hasSize(2);
		assertThat(found.products().get(0).productId()).isEqualTo("P001");
		assertThat(found.products().get(1).unitPrice()).isEqualByComparingTo("10.50");
		assertThat(found.total()).isEqualByComparingTo("200031.50");
	}

	@Test
	@DisplayName("devuelve la orden tal como queda persistida")
	void devuelveLoPersistido() {
		Order order = OrderFixtures.validOrder();

		Order saved = repository.save(order);

		assertThat(saved.id()).isEqualTo(order.id());
		assertThat(saved.status()).isEqualTo(order.status());
		assertThat(saved.total()).isEqualByComparingTo(order.total());
	}

	@Test
	@DisplayName("actualiza el estado de una orden existente sin duplicar sus líneas")
	void actualizaEstado() {
		Order order = OrderFixtures.validOrder();
		repository.save(order);
		entityManager.flush();

		order.changeStatus(OrderStatus.PAGADA);
		repository.save(order);
		Order found = reload(order.id());

		assertThat(found.status()).isEqualTo(OrderStatus.PAGADA);
		assertThat(found.products()).hasSize(1);
	}

	@Test
	@DisplayName("devuelve vacío si la orden no existe")
	void ordenInexistente() {
		assertThat(repository.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	@DisplayName("lista todas las órdenes guardadas")
	void listaTodas() {
		Order first = repository.save(OrderFixtures.validOrder());
		Order second = repository.save(OrderFixtures.validOrder());
		entityManager.flush();
		entityManager.clear();

		assertThat(repository.findAll()).extracting(Order::id).containsExactlyInAnyOrder(first.id(), second.id());
	}

	private Order reload(UUID id) {
		entityManager.flush();
		entityManager.clear();
		return repository.findById(id).orElseThrow();
	}
}
