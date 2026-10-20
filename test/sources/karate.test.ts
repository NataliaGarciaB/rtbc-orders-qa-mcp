import { describe, expect, it } from 'vitest';
import type { EscenarioKarate } from '../../src/core/modelos.js';
import { FormatoInvalidoError } from '../../src/sources/errores.js';
import { leerFeatureKarate } from '../../src/sources/karate.js';
import { artefacto } from '../fixtures.js';

const ACEPTACION = 'src/test/resources/acceptance';

function leer(nombre: string): EscenarioKarate[] {
	const archivo = `${ACEPTACION}/${nombre}`;
	return leerFeatureKarate(artefacto(archivo), archivo);
}

/** Forma compacta para comparar: "escenario | etiquetas | MÉTODO ruta origen→destino http" por petición. */
function resumen(escenarios: EscenarioKarate[]): string[] {
	return escenarios.flatMap((e) =>
		e.peticiones.map(
			(p) => `${e.nombre} | ${e.etiquetas.join(' ')} | ${p.metodo} ${p.ruta} ${p.origen ?? '-'}→${p.destino ?? '-'} ${p.http ?? '?'}`,
		),
	);
}

describe('Lector de escenarios Karate (Q01, Q02)', () => {
	it('transiciones.feature: interpreta estado de origen, destino y código, y expande el Scenario Outline', () => {
		expect(resumen(leer('transiciones.feature'))).toEqual([
			'Intentar enviar una orden no pagada | @transiciones @critico @RN-05 | PATCH /orders/{id}/status CREADA→ENVIADA 400',
			'Intentar cancelar una orden entregada | @transiciones @critico @RN-04 | DELETE /orders/{id} ENTREGADA→- 409',
			'Intentar cancelar una orden entregada | @transiciones @critico @RN-04 | GET /orders/{id} ENTREGADA→- 200',
			'Transición CREADA → PAGADA responde 200 | @transiciones @RN-06 | PATCH /orders/{id}/status CREADA→PAGADA 200',
			'Transición PAGADA → ENVIADA responde 200 | @transiciones @RN-06 | PATCH /orders/{id}/status PAGADA→ENVIADA 200',
			'Transición ENVIADA → ENTREGADA responde 200 | @transiciones @RN-06 | PATCH /orders/{id}/status ENVIADA→ENTREGADA 200',
			'Transición PAGADA → CANCELADA responde 200 | @transiciones @RN-06 | PATCH /orders/{id}/status PAGADA→CANCELADA 200',
			'Transición CREADA → ENTREGADA responde 409 | @transiciones @RN-06 | PATCH /orders/{id}/status CREADA→ENTREGADA 409',
			'Transición PAGADA → PAGADA responde 409 | @transiciones @RN-06 | PATCH /orders/{id}/status PAGADA→PAGADA 409',
			'Transición ENVIADA → PAGADA responde 409 | @transiciones @RN-06 | PATCH /orders/{id}/status ENVIADA→PAGADA 409',
			'Transición CANCELADA → PAGADA responde 409 | @transiciones @RN-06 | PATCH /orders/{id}/status CANCELADA→PAGADA 409',
			// Limitación conocida (ADR-006): las transiciones dentro de karate.map no se detectan
			'Flujo completo CREADA → PAGADA → ENVIADA → ENTREGADA | @transiciones @RN-06 | GET /orders/{id} CREADA→- 200',
		]);
	});

	it('cancelar.feature: un DELETE con 200 deja la orden CANCELADA para las peticiones siguientes', () => {
		expect(resumen(leer('cancelar.feature'))).toEqual([
			'Cancelar una orden creada responde 200 y la orden sigue consultable | @cancelar | DELETE /orders/{id} CREADA→- 200',
			'Cancelar una orden creada responde 200 y la orden sigue consultable | @cancelar | GET /orders/{id} CANCELADA→- 200',
			'Cancelar una orden ya cancelada responde 409 | @cancelar @RN-06 | DELETE /orders/{id} CANCELADA→- 409',
		]);
	});

	it('validaciones.feature: etiquetas por bloque Examples, rutas con valores literales y estados desconocidos', () => {
		expect(resumen(leer('validaciones.feature'))).toEqual([
			'Crear una orden inválida responde 400 con el campo inválido: sin productos | @validaciones @RN-01 | POST /orders -→- 400',
			'Crear una orden inválida responde 400 con el campo inválido: cantidad 0 | @validaciones @RN-02 | POST /orders -→- 400',
			'Crear una orden inválida responde 400 con el campo inválido: cliente vacío | @validaciones | POST /orders -→- 400',
			'Crear una orden inválida responde 400 con el campo inválido: valor unitario 0 | @validaciones | POST /orders -→- 400',
			'Crear una orden inválida responde 400 con el campo inválido: valor con 3 decimales | @validaciones | POST /orders -→- 400',
			'Actualizar a un estado desconocido responde 400 con el campo estado | @validaciones | PATCH /orders/{id}/status CREADA→PERDIDA 400',
			'Un id que no es UUID responde 400 | @validaciones | GET /orders/abc -→- 400',
		]);
	});

	it('crear.feature y consultar.feature: varias peticiones por escenario y rutas sin orden previa', () => {
		expect(resumen(leer('crear.feature'))).toEqual([
			'Crear una orden válida | @crear @critico | POST /orders -→- 201',
			'El total se calcula en el servidor e ignora total y estado enviados por el cliente | @crear @RN-03 | POST /orders -→- 201',
			'El total de varias líneas con decimales es exacto | @crear @RN-03 | POST /orders -→- 201',
			'El total de varias líneas con decimales es exacto | @crear @RN-03 | GET /orders/{id} -→- 200',
		]);
		expect(resumen(leer('consultar.feature'))).toEqual([
			'Consultar una orden existente | @consultar @critico | GET /orders/{id} CREADA→- 200',
			'Consultar una orden inexistente responde 404 | @consultar | GET /orders/00000000-0000-0000-0000-000000000000 -→- 404',
			'Listar órdenes incluye la orden creada | @consultar @listar | GET /orders CREADA→- 200',
		]);
	});

	it('ignora los helpers marcados con @ignore', () => {
		expect(leer('helpers/orden-en-estado.feature')).toEqual([]);
		expect(leer('helpers/crear-orden.feature')).toEqual([]);
	});

	it('conserva el archivo de origen de cada escenario', () => {
		expect(leer('cancelar.feature').map((e) => e.archivo)).toEqual([
			`${ACEPTACION}/cancelar.feature`,
			`${ACEPTACION}/cancelar.feature`,
		]);
	});

	it('devuelve el mismo resultado en lecturas sucesivas (Q07)', () => {
		expect(leer('transiciones.feature')).toEqual(leer('transiciones.feature'));
	});

	it('rechaza un archivo que no es Gherkin válido (Q03)', () => {
		// Dos Feature en un mismo archivo no es Gherkin válido
		expect(() => leerFeatureKarate('Feature: x\n  Scenario: y\n    Given algo\nFeature: otra\n', 'roto.feature')).toThrow(
			new FormatoInvalidoError('roto.feature', 'un archivo .feature con sintaxis Gherkin válida'),
		);
	});
});
