package com.rtbc.orders.entrypoint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El contenedor necesita saber si la aplicación está lista (healthcheck). Solo se expone ese endpoint, sin detalles
 * internos (ADR-021).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Endpoint de salud")
class HealthEndpointTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("GET /actuator/health responde UP sin detalles de componentes")
	void saludSinDetalles() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
	}

	@ParameterizedTest(name = "{0} no está expuesto")
	@ValueSource(strings = { "/actuator", "/actuator/env", "/actuator/beans", "/actuator/info", "/actuator/heapdump" })
	void otrosEndpointsNoExpuestos(String path) throws Exception {
		mvc.perform(get(path)).andExpect(status().isNotFound());
	}
}
