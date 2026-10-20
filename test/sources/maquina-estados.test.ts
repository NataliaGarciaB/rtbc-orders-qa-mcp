import { describe, expect, it } from 'vitest';
import { FormatoInvalidoError } from '../../src/sources/errores.js';
import { leerMaquinaEstados } from '../../src/sources/maquina-estados.js';
import { artefacto } from '../fixtures.js';

const ARCHIVO = 'docs/maquina-estados.md';

describe('Lector de la máquina de estados (docs/maquina-estados.md)', () => {
	it('lee las 25 transiciones en orden de estado de origen y de destino', () => {
		const transiciones = leerMaquinaEstados(artefacto(ARCHIVO));

		expect(transiciones).toHaveLength(25);
		expect(transiciones.slice(0, 5).map((t) => `${t.origen}→${t.destino}`)).toEqual([
			'CREADA→CREADA',
			'CREADA→PAGADA',
			'CREADA→ENVIADA',
			'CREADA→ENTREGADA',
			'CREADA→CANCELADA',
		]);
		expect(transiciones.at(-1)).toEqual({ origen: 'CANCELADA', destino: 'CANCELADA', http: 409, valida: false });
	});

	it('cuenta lo que dice la tabla: 6 válidas, 1 con 400 y 18 con 409', () => {
		const transiciones = leerMaquinaEstados(artefacto(ARCHIVO));

		expect(transiciones.filter((t) => t.valida)).toHaveLength(6);
		expect(transiciones.filter((t) => t.http === 400)).toHaveLength(1);
		expect(transiciones.filter((t) => t.http === 409)).toHaveLength(18);
	});

	it('identifica las transiciones válidas', () => {
		const validas = leerMaquinaEstados(artefacto(ARCHIVO))
			.filter((t) => t.valida)
			.map((t) => `${t.origen}→${t.destino}`);

		expect(validas).toEqual([
			'CREADA→PAGADA',
			'CREADA→CANCELADA',
			'PAGADA→ENVIADA',
			'PAGADA→CANCELADA',
			'ENVIADA→ENTREGADA',
			'ENVIADA→CANCELADA',
		]);
	});

	it('asocia la regla de negocio cuando la celda la menciona', () => {
		const transiciones = leerMaquinaEstados(artefacto(ARCHIVO));

		expect(transiciones).toContainEqual({ origen: 'CREADA', destino: 'ENVIADA', http: 400, valida: false, regla: 'RN-05' });
		expect(transiciones).toContainEqual({ origen: 'ENTREGADA', destino: 'CANCELADA', http: 409, valida: false, regla: 'RN-04' });
		expect(transiciones).toContainEqual({ origen: 'CREADA', destino: 'PAGADA', http: 200, valida: true });
	});

	it('ordena por ciclo de vida aunque las filas de la tabla estén en otro orden', () => {
		const original = artefacto(ARCHIVO);
		const lineas = original.split('\n');
		const posicionCancelada = lineas.findIndex((linea) => linea.startsWith('| **CANCELADA** |'));
		const posicionCreada = lineas.findIndex((linea) => linea.startsWith('| **CREADA** |'));
		const [filaCancelada = ''] = lineas.splice(posicionCancelada, 1);
		lineas.splice(posicionCreada, 0, filaCancelada);
		const desordenada = lineas.join('\n');

		expect(desordenada).not.toEqual(original);

		expect(leerMaquinaEstados(desordenada)).toEqual(leerMaquinaEstados(original));
	});

	it('devuelve el mismo resultado en lecturas sucesivas (Q07)', () => {
		expect(leerMaquinaEstados(artefacto(ARCHIVO))).toEqual(leerMaquinaEstados(artefacto(ARCHIVO)));
	});

	describe('rechaza una tabla con formato inesperado (Q03)', () => {
		const original = artefacto(ARCHIVO);
		const esperado = new FormatoInvalidoError(
			ARCHIVO,
			'la tabla "Matriz completa de transiciones" con los 5 estados como filas y columnas y un código HTTP en cada celda',
		);

		it('sin la sección de la matriz', () => {
			expect(() => leerMaquinaEstados(original.replace('## Matriz completa de transiciones', '## Otra sección'))).toThrow(esperado);
		});

		it('con una fila menos', () => {
			expect(() => leerMaquinaEstados(original.replace(/^\| \*\*CANCELADA\*\*.*$/m, ''))).toThrow(esperado);
		});

		it('con una celda sin código HTTP', () => {
			expect(() => leerMaquinaEstados(original.replace('| **PAGADA** | 409 |', '| **PAGADA** | — |'))).toThrow(esperado);
		});

		it('con un estado desconocido en las columnas', () => {
			expect(() => leerMaquinaEstados(original.replace('| CREADA | PAGADA |', '| CREADA | PERDIDA |'))).toThrow(esperado);
		});

		it('con un estado desconocido en las filas', () => {
			expect(() => leerMaquinaEstados(original.replace('| **ENVIADA** |', '| **PERDIDA** |'))).toThrow(esperado);
		});
	});
});
