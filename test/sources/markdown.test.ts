import { describe, expect, it } from 'vitest';
import { filasDeTabla } from '../../src/sources/markdown.js';

describe('Lectura de tablas Markdown', () => {
	it('lee la primera tabla después del título, celda por celda y sin espacios sobrantes', () => {
		const markdown = ['# Documento', '', '## Datos  ', '', '|  A | B  |', '|---|---|', '| 1 |  2 |', '', '| X | Y |'].join('\n');

		expect(filasDeTabla(markdown, '## Datos')).toEqual([
			['A', 'B'],
			['---', '---'],
			['1', '2'],
		]);
	});

	it('acepta el título en la primera línea, filas con sangría y sin barra final', () => {
		const markdown = ['## Datos', '  | A | B', '  | 1 | 2 |'].join('\n');

		expect(filasDeTabla(markdown, '## Datos')).toEqual([
			['A', 'B'],
			['1', '2'],
		]);
	});

	it('acepta saltos de línea de Windows', () => {
		expect(filasDeTabla('## Datos\r\n| A |\r\n| 1 |\r\n', '## Datos')).toEqual([['A'], ['1']]);
	});

	it('termina la tabla en la primera línea que no es fila', () => {
		const markdown = ['## Datos', '| A |', 'texto', '| B |'].join('\n');

		expect(filasDeTabla(markdown, '## Datos')).toEqual([['A']]);
	});

	it('ignora las tablas anteriores al título', () => {
		const markdown = ['| antes |', '## Datos', '| después |'].join('\n');

		expect(filasDeTabla(markdown, '## Datos')).toEqual([['después']]);
	});

	it('devuelve una lista vacía si el título no existe o no tiene tabla', () => {
		expect(filasDeTabla('## Otro\n| A |', '## Datos')).toEqual([]);
		expect(filasDeTabla('## Datos\n\nsolo texto', '## Datos')).toEqual([]);
	});
});
