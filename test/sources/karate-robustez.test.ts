import { describe, expect, it } from 'vitest';
import type { EscenarioKarate } from '../../src/core/modelos.js';
import { leerFeatureKarate } from '../../src/sources/karate.js';

function resumen(escenarios: EscenarioKarate[]): string[] {
	return escenarios.flatMap((e) =>
		e.peticiones.map((p) => `${e.nombre} | ${p.metodo} ${p.ruta} ${p.origen ?? '-'}→${p.destino ?? '-'} ${p.http ?? '?'}`),
	);
}

function leer(...lineas: string[]): string[] {
	return resumen(leerFeatureKarate(['Feature: robustez', ...lineas].join('\n'), 'robustez.feature'));
}

const HELPER = "call read('classpath:acceptance/helpers/orden-en-estado.feature')";

/**
 * Los .feature de la API usan un estilo uniforme; estas pruebas fijan cómo se interpretan otros estilos válidos
 * para que el lector no reporte falsos vacíos ni omita escenarios (Q01, Q02).
 */
describe('Lector de escenarios Karate: robustez del intérprete de pasos', () => {
	it('tolera espacios variables, mayúsculas y campos adicionales en el body', () => {
		expect(
			leer(
				'  Scenario: espacios',
				`    Given def orden = ${HELPER}   {estado:'PAGADA'}`,
				"    And path   'orders' ,  orden.orderId,'status'",
				"    And request   { motivo: 'cliente', estado:'ENVIADA' }",
				'    When method   PATCH',
				'    Then status   200',
			),
		).toEqual(['espacios | PATCH /orders/{id}/status PAGADA→ENVIADA 200']);
	});

	it('no confunde pasos que solo mencionan path, request, method o status', () => {
		expect(
			leer(
				'  Scenario: menciones',
				"    Given path 'orders'",
				"    * def path = 'otra'",
				"    * def request = { estado: 'PAGADA' }",
				'    * def method = "delete"',
				'    When method get',
				'    Then status 200',
				'    And match response.status == 404',
				'    And match responseStatus == 500',
			),
		).toEqual(['menciones | GET /orders -→- 200']);
	});

	it('ignora métodos HTTP no soportados, estados inválidos en el helper y status sin petición', () => {
		expect(
			leer(
				'  Scenario: ignorados',
				'    Then status 200',
				`    Given def orden = ${HELPER} { estado: 'PERDIDA' }`,
				"    And path 'orders'",
				'    When method head',
				"    And path 'orders', orden.orderId",
				'    When method delete',
				'    Then status 200',
			),
		).toEqual(['ignorados | DELETE /orders/{id} -→- 200']);
	});

	it('usa / como ruta cuando no hay path antes de method', () => {
		expect(leer('  Scenario: sin path', '    When method get', '    Then status 200')).toEqual(['sin path | GET / -→- 200']);
	});

	it.each([
		[199, 'PAGADA'],
		[200, 'CANCELADA'],
		[299, 'CANCELADA'],
		[300, 'PAGADA'],
	])('solo un código 2xx cambia el estado de la orden (DELETE con %i → luego %s)', (codigo, estadoSiguiente) => {
		expect(
			leer(
				'  Scenario: límites',
				`    Given def orden = ${HELPER} { estado: 'PAGADA' }`,
				"    And path 'orders', orden.orderId",
				'    When method delete',
				`    Then status ${codigo}`,
				"    And path 'orders', orden.orderId",
				'    When method get',
				'    Then status 200',
			)[1],
		).toBe(`límites | GET /orders/{id} ${estadoSiguiente}→- 200`);
	});

	it('un PATCH exitoso hacia un estado inválido no cambia el estado de la orden', () => {
		expect(
			leer(
				'  Scenario: estado inválido',
				`    Given def orden = ${HELPER} { estado: 'CREADA' }`,
				"    And path 'orders', orden.orderId, 'status'",
				"    And request { estado: 'PERDIDA' }",
				'    When method patch',
				'    Then status 200',
				"    And path 'orders', orden.orderId",
				'    When method get',
				'    Then status 200',
			)[1],
		).toBe('estado inválido | GET /orders/{id} CREADA→- 200');
	});

	it('conserva las etiquetas del escenario cuando la feature no tiene etiquetas', () => {
		const [escenario] = leerFeatureKarate(
			['Feature: sin etiquetas', '  @RN-01', '  Scenario: uno', "    Given path 'orders'"].join('\n'),
			'x.feature',
		);
		expect(escenario?.etiquetas).toEqual(['@RN-01']);
	});

	it('mantiene el marcador si una columna no existe en los ejemplos', () => {
		expect(
			leer(
				'  Scenario Outline: <caso> <inexistente>',
				"    Given path 'orders'",
				'    When method get',
				'    Then status 200',
				'    Examples:',
				'      | caso |',
				'      | uno  |',
			),
		).toEqual(['uno <inexistente> | GET /orders -→- 200']);
	});
});
