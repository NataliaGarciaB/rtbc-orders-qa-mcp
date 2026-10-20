package com.rtbc.orders.entrypoint.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rtbc.orders.application.OrderNotFoundException;
import com.rtbc.orders.domain.InvalidTransitionException;
import com.rtbc.orders.domain.OrderNotPaidException;
import com.rtbc.orders.domain.OrderStatus;
import com.rtbc.orders.domain.ValidationException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Traducción de errores de negocio a ProblemDetail (RFC 9457) con {@code codigo} estable (ADR-005, ADR-009).
 */
@DisplayName("Errores de negocio en la API")
class ErrorHandlingTest extends OrderControllerTestSupport {

	private final UUID id = UUID.randomUUID();

	@Test
	@Tag("RN-05")
	@DisplayName("RN-05: enviar una orden no pagada responde 400 ORDEN_NO_PAGADA")
	void ordenNoPagada() throws Exception {
		when(changeOrderStatus.execute(id, OrderStatus.ENVIADA)).thenThrow(new OrderNotPaidException());

		mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"estado\": \"ENVIADA\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.codigo").value("ORDEN_NO_PAGADA"))
				.andExpect(jsonPath("$.detail").value("La orden no puede ser enviada si no ha sido pagada"))
				.andExpect(jsonPath("$.instance").value("/orders/" + id + "/status"));
	}

	@Test
	@Tag("RN-04")
	@DisplayName("RN-04: cancelar una orden entregada responde 409 TRANSICION_INVALIDA")
	void cancelarEntregada() throws Exception {
		when(cancelOrder.execute(id))
				.thenThrow(new InvalidTransitionException(OrderStatus.ENTREGADA, OrderStatus.CANCELADA));

		mvc.perform(delete("/orders/{id}", id))
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"))
				.andExpect(jsonPath("$.detail").value("No se puede pasar de ENTREGADA a CANCELADA"));
	}

	@Test
	@Tag("RN-06")
	@DisplayName("RN-06: una transición inválida por PATCH responde 409 TRANSICION_INVALIDA")
	void transicionInvalida() throws Exception {
		when(changeOrderStatus.execute(id, OrderStatus.PAGADA))
				.thenThrow(new InvalidTransitionException(OrderStatus.CANCELADA, OrderStatus.PAGADA));

		mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"estado\": \"PAGADA\" }"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
	}

	@Test
	@DisplayName("una orden inexistente responde 404 ORDEN_NO_ENCONTRADA")
	void ordenInexistente() throws Exception {
		when(getOrder.execute(id)).thenThrow(new OrderNotFoundException(id));

		mvc.perform(get("/orders/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ORDEN_NO_ENCONTRADA"))
				.andExpect(jsonPath("$.detail").value("No existe la orden " + id));
	}

	@Test
	@DisplayName("una validación del dominio responde 400 VALIDACION con el campo en errores")
	void validacionDelDominio() throws Exception {
		when(createOrder.execute(anyString(), anyList()))
				.thenThrow(new ValidationException("productos", "la orden debe contener al menos un producto"));

		mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[0].campo").value("productos"))
				.andExpect(jsonPath("$.errores[0].mensaje").value("la orden debe contener al menos un producto"));
	}

	@Test
	@DisplayName("R08: una modificación concurrente responde 409 TRANSICION_INVALIDA")
	void modificacionConcurrente() throws Exception {
		when(changeOrderStatus.execute(any(), any()))
				.thenThrow(new ObjectOptimisticLockingFailureException("OrderEntity", id));

		mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"estado\": \"PAGADA\" }"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"))
				.andExpect(jsonPath("$.detail")
						.value("La orden fue modificada por otra solicitud; consulte su estado e intente de nuevo"));
	}
}
