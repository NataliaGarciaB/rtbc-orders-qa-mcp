import { describe, expect, it } from 'vitest';
import { FormatoInvalidoError } from '../../src/sources/errores.js';
import { leerReglasNegocio } from '../../src/sources/reglas-negocio.js';
import { leerRiesgos } from '../../src/sources/riesgos.js';
import { artefacto } from '../fixtures.js';

describe('Lector de la matriz de riesgos (docs/qa/estrategia-pruebas.md)', () => {
	const ARCHIVO = 'docs/qa/estrategia-pruebas.md';

	it('lee los 10 riesgos con su nivel, su puntaje y las reglas que afectan', () => {
		const riesgos = leerRiesgos(artefacto(ARCHIVO));

		expect(riesgos.map((r) => `${r.id} ${r.puntaje} ${r.nivel} [${r.reglas.join(',')}]`)).toEqual([
			'R01 9 Alto [RN-04,RN-05,RN-06]',
			'R02 6 Alto [RN-03]',
			'R03 6 Alto [RN-01,RN-02]',
			'R04 4 Medio []',
			'R05 4 Medio []',
			'R06 3 Medio []',
			'R07 4 Medio []',
			'R08 2 Bajo [RN-06]',
			'R09 6 Alto []',
			'R10 2 Bajo []',
		]);
	});

	it('conserva la descripción del riesgo', () => {
		const r01 = leerRiesgos(artefacto(ARCHIVO))[0];

		expect(r01?.descripcion).toBe(
			'Se permite una transición de estado inválida, por ejemplo cancelar una orden entregada o enviar sin pagar',
		);
	});

	describe('rechaza una matriz con formato inesperado (Q03)', () => {
		const original = artefacto(ARCHIVO);
		const esperado = new FormatoInvalidoError(
			ARCHIVO,
			'la tabla "Matriz de riesgos" con las columnas ID, Riesgo, RN y Nivel, y un nivel "Alto", "Medio" o "Bajo" con su puntaje',
		);

		it('sin la sección', () => {
			expect(() => leerRiesgos(original.replace('## 2. Matriz de riesgos', '## Riesgos'))).toThrow(esperado);
		});

		it('sin la columna Nivel', () => {
			expect(() => leerRiesgos(original.replace('| P | I | Nivel |', '| P | I | Severidad |'))).toThrow(esperado);
		});

		it('con un nivel desconocido', () => {
			expect(() => leerRiesgos(original.replace('**9 Alto**', '**9 Crítico**'))).toThrow(esperado);
		});

		it('con un nivel sin puntaje', () => {
			expect(() => leerRiesgos(original.replace('**9 Alto**', '**Alto**'))).toThrow(esperado);
		});
	});
});

describe('Lector de las reglas de negocio (README.md)', () => {
	const ARCHIVO = 'README.md';

	it('lee las 6 reglas con su descripción', () => {
		expect(leerReglasNegocio(artefacto(ARCHIVO))).toEqual([
			{ id: 'RN-01', descripcion: 'Una orden debe contener al menos un producto' },
			{ id: 'RN-02', descripcion: 'La cantidad de cada producto debe ser mayor que cero' },
			{ id: 'RN-03', descripcion: 'El total se calcula en el servidor; si el cliente envía un total, se ignora' },
			{ id: 'RN-04', descripcion: 'Una orden entregada no puede ser cancelada' },
			{ id: 'RN-05', descripcion: 'Una orden no puede ser enviada si no ha sido pagada' },
			{ id: 'RN-06', descripcion: 'No se permiten transiciones de estado inválidas' },
		]);
	});

	describe('rechaza una tabla con formato inesperado (Q03)', () => {
		const original = artefacto(ARCHIVO);
		const esperado = new FormatoInvalidoError(
			ARCHIVO,
			'la tabla "Reglas de negocio" con las columnas Regla y Descripción, y al menos una regla RN-xx',
		);

		it('sin la sección', () => {
			expect(() => leerReglasNegocio(original.replace('### Reglas de negocio', '### Reglas'))).toThrow(esperado);
		});

		it('con un identificador que no es RN-xx', () => {
			expect(() => leerReglasNegocio(original.replace('| RN-01 |', '| Regla 1 |'))).toThrow(esperado);
		});
	});
});
