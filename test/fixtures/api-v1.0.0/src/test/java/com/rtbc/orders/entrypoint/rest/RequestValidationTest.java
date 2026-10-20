package com.rtbc.orders.entrypoint.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Validación de formato en el borde HTTP (ADR-008). Un request inválido nunca llega a los casos de uso.
 */
@DisplayName("Validación de requests")
class RequestValidationTest extends OrderControllerTestSupport {

	@AfterEach
	void noLlegaALosCasosDeUso() {
		verifyNoInteractions(createOrder, getOrder, listOrders, changeOrderStatus, cancelOrder);
	}

	@Nested
	@DisplayName("POST /orders")
	class Crear {

		@Test
		@Tag("RN-01")
		@DisplayName("RN-01: sin productos responde 400 con el campo productos")
		void sinProductos() throws Exception {
			postOrder("""
					{ "cliente": "Juan Pérez", "productos": [] }
					""")
					.andExpect(jsonPath("$.errores[*].campo").value(hasItem("productos")));
		}

		@Test
		@Tag("RN-02")
		@DisplayName("RN-02: cantidad 0 responde 400 con el campo productos[0].cantidad")
		void cantidadCero() throws Exception {
			postOrder(order("\"Juan Pérez\"", "\"P001\"", "0", "100"))
					.andExpect(jsonPath("$.errores[0].campo").value("productos[0].cantidad"))
					.andExpect(jsonPath("$.errores[0].mensaje").value("debe ser mayor que cero"));
		}

		@ParameterizedTest(name = "{0} → {3}")
		@Tag("validaciones")
		@CsvSource(delimiter = '|', value = {
				"cliente ausente           | null                 | \"P001\"   | cliente",
				"cliente vacío             | \"   \"             | \"P001\"   | cliente",
				"id de producto ausente    | \"Juan Pérez\"       | null       | productos[0].id",
				"id de producto vacío      | \"Juan Pérez\"       | \"\"       | productos[0].id"
		})
		void camposObligatorios(String caso, String cliente, String productId, String campo) throws Exception {
			postOrder(order(cliente, productId, "1", "100"))
					.andExpect(jsonPath("$.errores[*].campo").value(hasItem(campo)));
		}

		@Test
		@Tag("validaciones")
		@DisplayName("cliente de 121 caracteres e id de producto de 51 responden 400")
		void longitudesMaximas() throws Exception {
			postOrder(order("\"" + "a".repeat(121) + "\"", "\"" + "P".repeat(51) + "\"", "1", "100"))
					.andExpect(jsonPath("$.errores[*].campo").value(containsInAnyOrder("cliente", "productos[0].id")));
		}

		@ParameterizedTest(name = "valorUnitario {0} responde 400")
		@Tag("validaciones")
		@CsvSource({ "0", "-1", "10.555", "100000000000000000" })
		void valorUnitarioInvalido(String valor) throws Exception {
			postOrder(order("\"Juan Pérez\"", "\"P001\"", "1", valor))
					.andExpect(jsonPath("$.errores[0].campo").value("productos[0].valorUnitario"));
		}

		@Test
		@Tag("validaciones")
		@DisplayName("reporta todos los campos inválidos a la vez")
		void variosErrores() throws Exception {
			postOrder(order("null", "\"P001\"", "0", "10.555"))
					.andExpect(jsonPath("$.errores[*].campo").value(
							containsInAnyOrder("cliente", "productos[0].cantidad", "productos[0].valorUnitario")));
		}

		@Test
		@Tag("validaciones")
		@DisplayName("un producto nulo en la lista responde 400")
		void productoNulo() throws Exception {
			postOrder("""
					{ "cliente": "Juan Pérez", "productos": [ null ] }
					""")
					.andExpect(jsonPath("$.errores[0].campo").value("productos[0]"));
		}

		@Test
		@Tag("validaciones")
		@DisplayName("una cantidad decimal responde 400 (no se trunca)")
		void cantidadDecimal() throws Exception {
			postOrder(order("\"Juan Pérez\"", "\"P001\"", "1.5", "100"));
		}

		@Test
		@DisplayName("un JSON mal formado responde 400")
		void jsonMalFormado() throws Exception {
			postOrder("{ \"cliente\": ");
		}

		@Test
		@DisplayName("un request sin body responde 400")
		void sinBody() throws Exception {
			postOrder("");
		}

		private ResultActions postOrder(String json) throws Exception {
			return expectValidationError(
					mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(json)), "/orders");
		}

		private String order(String cliente, String productId, String cantidad, String valorUnitario) {
			return """
					{ "cliente": %s, "productos": [ { "id": %s, "cantidad": %s, "valorUnitario": %s } ] }
					""".formatted(cliente, productId, cantidad, valorUnitario);
		}
	}

	@Nested
	@DisplayName("PATCH /orders/{id}/status")
	class ActualizarEstado {

		private final UUID id = UUID.randomUUID();

		@Test
		@DisplayName("un estado desconocido responde 400 con el campo estado")
		void estadoDesconocido() throws Exception {
			patchStatus("{ \"estado\": \"PERDIDA\" }")
					.andExpect(jsonPath("$.errores[0].campo").value("estado"));
		}

		@Test
		@DisplayName("un estado ausente responde 400 con el campo estado")
		void estadoAusente() throws Exception {
			patchStatus("{ }")
					.andExpect(jsonPath("$.errores[0].campo").value("estado"));
		}

		private ResultActions patchStatus(String json) throws Exception {
			return expectValidationError(
					mvc.perform(patch("/orders/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content(json)),
					"/orders/" + id + "/status");
		}
	}

	@Test
	@DisplayName("un id que no es UUID responde 400 con el campo id")
	void idMalFormado() throws Exception {
		expectValidationError(mvc.perform(get("/orders/{id}", "abc")), "/orders/abc")
				.andExpect(jsonPath("$.errores[0].campo").value("id"));
	}

	private static ResultActions expectValidationError(ResultActions result, String instance) throws Exception {
		return result
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.instance").value(instance));
	}
}
