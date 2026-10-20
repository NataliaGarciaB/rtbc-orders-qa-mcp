package com.rtbc.orders.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de aceptación sobre la API desplegada (Karate). Se ejecutan con el perfil Maven {@code acceptance}.
 * Cada escenario crea sus propios datos, por eso corren en paralelo.
 */
@DisplayName("Pruebas de aceptación (Karate)")
class OrdersKarateIT {

	private static final int THREADS = 4;
	private static final String IGNORE = "~@ignore";

	@Test
	@DisplayName("todos los escenarios de aceptación pasan")
	void escenariosDeAceptacion() {
		String tags = System.getProperty("karate.tags", "");
		// Los helpers (@ignore) solo se ejecutan cuando un escenario los llama
		Runner.Builder<?> runner = Runner.path("classpath:acceptance")
				.tags(tags.isBlank() ? List.of(IGNORE) : List.of(IGNORE, tags))
				.outputCucumberJson(false);

		Results results = runner.parallel(THREADS);

		assertThat(results.getScenariosTotal())
				.as("se ejecutó al menos un escenario (un filtro de tags mal escrito no debe dar falso verde)")
				.isPositive();
		assertThat(results.getFailCount()).as(results.getErrorMessages()).isZero();
	}
}
