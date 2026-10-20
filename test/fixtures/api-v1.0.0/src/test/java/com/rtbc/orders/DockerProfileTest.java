package com.rtbc.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Perfil {@code docker} (lo activa la imagen): logs JSON para que una herramienta los procese (ADR-019, ADR-021).
 * <p>
 * Se verifica la configuración sin levantar un contexto con el perfil: hacerlo reconfiguraría el logging global de
 * la JVM y afectaría a otras pruebas según el orden de ejecución. El JSON real con {@code requestId} se verifica
 * contra el contenedor (scripts/verificar-docker.ps1).
 */
@DisplayName("Perfil docker")
class DockerProfileTest {

	@Test
	@DisplayName("emite los logs de consola en formato JSON (ECS)")
	void logsJson() throws IOException {
		Properties docker = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/application-docker.properties")) {
			assertThat(in).as("existe application-docker.properties").isNotNull();
			docker.load(in);
		}

		assertThat(docker.getProperty("logging.structured.format.console")).isEqualTo("ecs");
	}
}
