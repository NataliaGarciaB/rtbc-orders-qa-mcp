package com.rtbc.orders.domain;

import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Línea de producto")
class ProductLineTest {

	@Nested
	@Tag("RN-02")
	@DisplayName("RN-02: la cantidad debe ser mayor que cero")
	class CantidadMayorQueCero {

		@ParameterizedTest(name = "rechaza cantidad {0}")
		@ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
		void rechazaCantidadNoPositiva(int quantity) {
			assertThatThrownBy(() -> line("P001", quantity, "100"))
					.isInstanceOfSatisfying(ValidationException.class, e -> {
						assertThat(e.code()).isEqualTo(ErrorCode.VALIDACION);
						assertThat(e.field()).isEqualTo("productos.cantidad");
					});
		}

		@Test
		@DisplayName("acepta cantidad 1 (límite inferior válido)")
		void aceptaCantidadUno() {
			assertThat(line("P001", 1, "100").quantity()).isEqualTo(1);
		}
	}

	@Nested
	@Tag("validaciones")
	@DisplayName("ADR-008: id de producto obligatorio, máximo 50 caracteres")
	class IdDeProducto {

		@ParameterizedTest(name = "rechaza id \"{0}\"")
		@NullSource
		@ValueSource(strings = { "", "   " })
		void rechazaIdVacio(String productId) {
			assertInvalid(() -> line(productId, 1, "100"), "productos.id");
		}

		@Test
		@DisplayName("rechaza id de 51 caracteres")
		void rechazaIdDemasiadoLargo() {
			assertInvalid(() -> line("P".repeat(51), 1, "100"), "productos.id");
		}

		@ParameterizedTest(name = "acepta id de {0} caracteres")
		@ValueSource(ints = { 1, 50 })
		void aceptaLimitesValidos(int length) {
			assertThat(line("P".repeat(length), 1, "100").productId()).hasSize(length);
		}
	}

	@Nested
	@Tag("validaciones")
	@DisplayName("ADR-008: valor unitario mayor que cero, máximo 2 decimales")
	class ValorUnitario {

		@ParameterizedTest(name = "{0} se normaliza a {1}")
		@CsvSource({ "100000, 100000.00", "35000.5, 35000.50", "10.500, 10.50", "1E+5, 100000.00" })
		@DisplayName("normaliza el valor unitario a 2 decimales para que el contrato sea consistente")
		void normalizaADosDecimales(String recibido, String esperado) {
			assertThat(line("P001", 1, recibido).unitPrice()).isEqualTo(new BigDecimal(esperado));
		}

		@Test
		@DisplayName("rechaza valor unitario ausente")
		void rechazaValorNulo() {
			assertInvalid(() -> new ProductLine("P001", 1, null), "productos.valorUnitario");
		}

		@ParameterizedTest(name = "rechaza valor {0}")
		@ValueSource(strings = { "-1", "0", "0.00", "0.001", "10.555" })
		void rechazaValorInvalido(String unitPrice) {
			assertInvalid(() -> line("P001", 1, unitPrice), "productos.valorUnitario");
		}

		@ParameterizedTest(name = "acepta valor {0}")
		@ValueSource(strings = { "0.01", "10.50", "10.500", "100000" })
		void aceptaValorValido(String unitPrice) {
			assertThat(line("P001", 1, unitPrice).unitPrice()).isEqualByComparingTo(unitPrice);
		}
	}

	private static void assertInvalid(ThrowingCallable creation, String field) {
		assertThatThrownBy(creation)
				.isInstanceOfSatisfying(ValidationException.class, e -> assertThat(e.field()).isEqualTo(field));
	}
}
