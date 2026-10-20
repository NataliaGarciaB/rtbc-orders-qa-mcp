package com.rtbc.orders.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

/**
 * R06: un error inesperado no expone detalles internos al cliente, pero queda completo en el log.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("R06: errores inesperados")
class UnexpectedErrorTest extends OrderControllerTestSupport {

	private static final String INTERNAL_DETAIL = "detalle interno: SELECT * FROM orders";

	@Test
	@DisplayName("responde 500 genérico sin mensaje, clase ni traza de la excepción")
	void noExponeDetalles() throws Exception {
		when(listOrders.execute()).thenThrow(new IllegalStateException(INTERNAL_DETAIL));

		mvc.perform(get("/orders"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
				.andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado; intente más tarde"))
				.andExpect(jsonPath("$.trace").doesNotExist())
				.andExpect(jsonPath("$.exception").doesNotExist())
				.andExpect(content().string(not(containsString(INTERNAL_DETAIL))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	@Test
	@DisplayName("registra la excepción completa en el log para diagnosticarla")
	void registraEnLog(CapturedOutput output) throws Exception {
		when(listOrders.execute()).thenThrow(new IllegalStateException(INTERNAL_DETAIL));

		mvc.perform(get("/orders")).andExpect(status().isInternalServerError());

		assertThat(output.getOut())
				.contains("Error inesperado en GET /orders")
				.contains("java.lang.IllegalStateException: " + INTERNAL_DETAIL);
	}
}
