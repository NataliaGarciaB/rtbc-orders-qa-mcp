package com.rtbc.orders.entrypoint;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger UI publica el contrato de diseño (docs/api/openapi.yaml) tal cual: una sola fuente de verdad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Documentación de la API")
class ApiDocumentationTest {

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("publica docs/api/openapi.yaml sin modificaciones")
	void publicaContratoDeDiseno() throws Exception {
		byte[] contract = Files.readAllBytes(Path.of("docs/api/openapi.yaml"));

		mvc.perform(get("/openapi.yaml"))
				.andExpect(status().isOk())
				.andExpect(content().bytes(contract));
	}

	@Test
	@DisplayName("Swagger UI está disponible y apunta al contrato de diseño")
	void swaggerUiApuntaAlContrato() throws Exception {
		mvc.perform(get("/swagger-ui.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("url: '/openapi.yaml'")));
		mvc.perform(get("/webjars/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
		mvc.perform(get("/webjars/swagger-ui/swagger-ui.css")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("no publica una especificación generada que compita con el contrato")
	void sinEspecificacionGenerada() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
	}
}
