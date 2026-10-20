import { describe, expect, it } from 'vitest';
import { AccesoDenegadoError, ArchivoNoEncontradoError, FormatoInvalidoError } from '../../src/sources/errores.js';

/**
 * Los mensajes de error llegan a la persona a través de Claude Code: deben decir qué archivo falló y por qué
 * (riesgo Q03).
 */
describe('Errores de lectura', () => {
	it('acceso denegado indica la ruta rechazada', () => {
		const error = new AccesoDenegadoError('../secreto');

		expect(error.name).toBe('AccesoDenegadoError');
		expect(error.ruta).toBe('../secreto');
		expect(error.message).toBe('Acceso denegado: "../secreto" está fuera del repositorio de la API');
	});

	it('archivo no encontrado indica la ruta buscada', () => {
		const error = new ArchivoNoEncontradoError('docs/no-existe.md');

		expect(error.name).toBe('ArchivoNoEncontradoError');
		expect(error.message).toBe('No se encontró "docs/no-existe.md" en el repositorio de la API');
	});

	it('formato inválido indica el archivo y la estructura esperada', () => {
		const error = new FormatoInvalidoError('docs/maquina-estados.md', 'una tabla de 5 × 5 estados');

		expect(error.name).toBe('FormatoInvalidoError');
		expect(error.archivo).toBe('docs/maquina-estados.md');
		expect(error.esperado).toBe('una tabla de 5 × 5 estados');
		expect(error.message).toBe(
			'docs/maquina-estados.md: formato no reconocido. Se esperaba una tabla de 5 × 5 estados',
		);
	});
});
