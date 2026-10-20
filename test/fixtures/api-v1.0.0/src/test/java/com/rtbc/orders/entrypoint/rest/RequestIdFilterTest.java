package com.rtbc.orders.entrypoint.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Observabilidad: cada request lleva un id de correlación que aparece en sus logs y en la respuesta.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("Id de correlación X-Request-Id")
class RequestIdFilterTest extends OrderControllerTestSupport {

	private static final String HEADER = "X-Request-Id";

	@Test
	@DisplayName("genera un UUID si el request no trae el header")
	void generaId() throws Exception {
		when(listOrders.execute()).thenReturn(List.of());

		String id = mvc.perform(get("/orders"))
				.andReturn().getResponse().getHeader(HEADER);

		assertThat(UUID.fromString(id)).isNotNull();
	}

	@Test
	@DisplayName("reutiliza un id válido enviado por el cliente")
	void reutilizaId() throws Exception {
		when(listOrders.execute()).thenReturn(List.of());

		mvc.perform(get("/orders").header(HEADER, "abc-123_x.y"))
				.andExpect(header().string(HEADER, "abc-123_x.y"));
	}

	@ParameterizedTest(name = "reemplaza el id inválido \"{0}\"")
	@ValueSource(strings = { "abc\nINFO falso", "con espacio", "" })
	void reemplazaIdInvalido(String invalid) throws Exception {
		when(listOrders.execute()).thenReturn(List.of());

		String id = mvc.perform(get("/orders").header(HEADER, invalid))
				.andReturn().getResponse().getHeader(HEADER);

		assertThat(UUID.fromString(id)).isNotNull();
	}

	@Test
	@DisplayName("reemplaza un id de más de 64 caracteres")
	void reemplazaIdLargo() throws Exception {
		when(listOrders.execute()).thenReturn(List.of());
		String tooLong = "a".repeat(65);

		String id = mvc.perform(get("/orders").header(HEADER, tooLong))
				.andReturn().getResponse().getHeader(HEADER);

		assertThat(id).isNotEqualTo(tooLong);
		assertThat(UUID.fromString(id)).isNotNull();
	}

	@Test
	@DisplayName("incluye el id en los logs del request y lo limpia al terminar")
	void incluyeIdEnLogs(CapturedOutput output) throws Exception {
		when(listOrders.execute()).thenThrow(new IllegalStateException("falla"));

		mvc.perform(get("/orders").header(HEADER, "req-42"));

		assertThat(output.getOut()).contains("[req-42] ").contains("Error inesperado en GET /orders");
		assertThat(MDC.get("requestId")).isNull();
	}
}
