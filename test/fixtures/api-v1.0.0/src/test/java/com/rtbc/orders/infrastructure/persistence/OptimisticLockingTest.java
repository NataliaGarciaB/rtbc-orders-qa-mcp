package com.rtbc.orders.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import com.rtbc.orders.domain.OrderStatus;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * R08 / ADR-016: dos cambios de estado concurrentes sobre la misma orden. Se simula con dos transacciones
 * reales (sin hilos ni esperas) para que la prueba sea determinista.
 */
@DataJpaTest
@Import(JpaOrderRepository.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("R08: bloqueo optimista en cambios de estado concurrentes")
class OptimisticLockingTest {

	@Autowired
	private JpaOrderRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private SpringDataOrderRepository jpa;

	/**
	 * Esta prueba confirma transacciones reales: limpia lo que guarda para no afectar a otras pruebas
	 * que comparten la misma base H2 en memoria.
	 */
	@AfterEach
	void limpiarDatosConfirmados() {
		new TransactionTemplate(transactionManager).executeWithoutResult(tx -> jpa.deleteAll());
	}

	@Test
	@DisplayName("rechaza el segundo cambio si la orden fue modificada después de leerla")
	void rechazaActualizacionConcurrente() {
		UUID id = repository.save(OrderFixtures.validOrder()).id();
		TransactionTemplate first = new TransactionTemplate(transactionManager);
		TransactionTemplate second = new TransactionTemplate(transactionManager);
		second.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		assertThatThrownBy(() -> first.executeWithoutResult(tx -> {
			Order readByFirst = repository.findById(id).orElseThrow();

			second.executeWithoutResult(tx2 -> {
				Order readBySecond = repository.findById(id).orElseThrow();
				readBySecond.changeStatus(OrderStatus.PAGADA);
				repository.save(readBySecond);
			});

			readByFirst.changeStatus(OrderStatus.CANCELADA);
			repository.save(readByFirst);
		})).isInstanceOf(ObjectOptimisticLockingFailureException.class);

		assertThat(repository.findById(id).orElseThrow().status())
				.as("prevalece el cambio que se confirmó primero")
				.isEqualTo(OrderStatus.PAGADA);
	}
}
