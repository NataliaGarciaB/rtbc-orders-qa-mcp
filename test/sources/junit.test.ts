import { describe, expect, it } from 'vitest';
import type { PruebaJUnit } from '../../src/core/modelos.js';
import { leerPruebasJUnit } from '../../src/sources/junit.js';
import { artefacto } from '../fixtures.js';

const PRUEBAS = 'src/test/java/com/rtbc/orders';

function leer(ruta: string): PruebaJUnit[] {
	const archivo = `${PRUEBAS}/${ruta}`;
	return leerPruebasJUnit(artefacto(archivo), archivo);
}

function resumen(pruebas: PruebaJUnit[]): string[] {
	return pruebas.map((p) => `${p.clase}.${p.metodo} [${p.etiquetas.join(',')}]`);
}

describe('Lector de pruebas JUnit (trazabilidad por @Tag)', () => {
	it('combina las etiquetas de la clase con las del método', () => {
		expect(resumen(leer('domain/OrderStatusTransitionTest.java'))).toEqual([
			'OrderStatusTransitionTest.matrizDeTransiciones [RN-06]',
			'OrderStatusTransitionTest.ordenEntregadaNoSeCancela [RN-06,RN-04]',
			'OrderStatusTransitionTest.ordenNoPagadaNoSeEnvia [RN-06,RN-05]',
			'OrderStatusTransitionTest.flujoCompleto [RN-06]',
			'OrderStatusTransitionTest.rechazaEstadoNulo [RN-06]',
		]);
	});

	it('hereda la etiqueta de una clase @Nested a todos sus métodos', () => {
		const alMenosUnProducto = resumen(leer('domain/OrderTest.java')).filter((p) => p.startsWith('OrderTest.AlMenosUnProducto.'));

		expect(alMenosUnProducto).toEqual([
			'OrderTest.AlMenosUnProducto.rechazaListaVacia [RN-01]',
			'OrderTest.AlMenosUnProducto.rechazaListaNula [RN-01]',
			'OrderTest.AlMenosUnProducto.creaOrdenConUnProducto [RN-01]',
			'OrderTest.AlMenosUnProducto.generaIdUnico [RN-01]',
			'OrderTest.AlMenosUnProducto.rechazaProductoNulo [RN-01]',
			'OrderTest.AlMenosUnProducto.copiaDefensivaDeProductos [RN-01]',
		]);
	});

	it('no se confunde con text blocks que contienen llaves', () => {
		const pruebas = resumen(leer('entrypoint/rest/RequestValidationTest.java'));

		expect(pruebas).toContain('RequestValidationTest.Crear.sinProductos [RN-01]');
		expect(pruebas).toContain('RequestValidationTest.Crear.cantidadCero [RN-02]');
		expect(pruebas).toContain('RequestValidationTest.ActualizarEstado.estadoDesconocido []');
		expect(pruebas).toContain('RequestValidationTest.idMalFormado []');
	});

	it('un archivo sin métodos de prueba no produce pruebas', () => {
		expect(leer('entrypoint/rest/OrderControllerTestSupport.java')).toEqual([]);
	});

	it('conserva el archivo de origen', () => {
		expect(leer('domain/OrderStatusTransitionTest.java')[0]?.archivo).toBe(`${PRUEBAS}/domain/OrderStatusTransitionTest.java`);
	});

	it('ignora comentarios y textos, y reconoce los distintos tipos de método de prueba', () => {
		const fuente = `
			package x;
			// @Tag("RN-09") comentado { no cuenta
			/* @Test
			   void comentado() { } */
			@Tag("RN-01")
			class Ejemplo {
				private final String texto = "} { @Tag(\\"RN-08\\") void falso() {";
				private final char llave = '}';

				@Test
				@DisplayName("con (paréntesis) y { llaves }")
				void simple() { }

				@ParameterizedTest(name = "caso {0}")
				@Tag("RN-02")
				@ValueSource(ints = { 1, 2 })
				void parametrizada(int n) { if (n > 0) { } }

				@RepeatedTest(2)
				java.util.List<String> repetida() { return java.util.List.of("{"); }

				@TestFactory
				Stream<DynamicTest> fabrica() { return Stream.empty(); }

				@BeforeEach
				void preparar() { }

				void auxiliar() { }
			}`;

		expect(resumen(leerPruebasJUnit(fuente, 'Ejemplo.java'))).toEqual([
			'Ejemplo.simple [RN-01]',
			'Ejemplo.parametrizada [RN-01,RN-02]',
			'Ejemplo.repetida [RN-01]',
			'Ejemplo.fabrica [RN-01]',
		]);
	});
});
