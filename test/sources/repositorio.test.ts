import { mkdtempSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { AccesoDenegadoError, ArchivoNoEncontradoError } from '../../src/sources/errores.js';
import { abrirRepositorio } from '../../src/sources/repositorio.js';
import { RAIZ_API } from '../fixtures.js';

// El repositorio se abre dentro de cada prueba: así cada mutante de Stryker se evalúa en la prueba que lo ejecuta
describe('Repositorio de la API (Q04: solo lectura y dentro del repositorio)', () => {
	it('lee un archivo por su ruta relativa', () => {
		expect(abrirRepositorio(RAIZ_API).leer('docs/api/openapi.yaml')).toContain('openapi: 3.0.3');
	});

	it('lista los archivos de un directorio con una extensión, recursivamente y en orden estable', () => {
		expect(abrirRepositorio(RAIZ_API).listar('src/test/resources/acceptance', '.feature')).toEqual([
			'src/test/resources/acceptance/cancelar.feature',
			'src/test/resources/acceptance/consultar.feature',
			'src/test/resources/acceptance/crear.feature',
			'src/test/resources/acceptance/helpers/cambiar-estado.feature',
			'src/test/resources/acceptance/helpers/crear-orden.feature',
			'src/test/resources/acceptance/helpers/orden-en-estado.feature',
			'src/test/resources/acceptance/operacion.feature',
			'src/test/resources/acceptance/transiciones.feature',
			'src/test/resources/acceptance/validaciones.feature',
		]);
	});

	it.each(['../package.json', 'docs/../../package.json', '..'])('rechaza la ruta "%s" que sale del repositorio', (ruta) => {
		expect(() => abrirRepositorio(RAIZ_API).leer(ruta)).toThrow(AccesoDenegadoError);
	});

	it('rechaza una ruta absoluta', () => {
		expect(() => abrirRepositorio(RAIZ_API).leer(join(RAIZ_API, 'README.md'))).toThrow(AccesoDenegadoError);
	});

	it('rechaza listar fuera del repositorio', () => {
		expect(() => abrirRepositorio(RAIZ_API).listar('..', '.ts')).toThrow(AccesoDenegadoError);
	});

	it('informa la ruta de un archivo inexistente', () => {
		expect(() => abrirRepositorio(RAIZ_API).leer('docs/no-existe.md')).toThrow(new ArchivoNoEncontradoError('docs/no-existe.md'));
	});

	it('rechaza abrir un repositorio que no existe', () => {
		expect(() => abrirRepositorio(join(tmpdir(), 'no-existe-rtbc'))).toThrow(ArchivoNoEncontradoError);
	});

	it('rechaza un enlace simbólico que apunta fuera del repositorio', (ctx) => {
		const raiz = mkdtempSync(join(tmpdir(), 'rtbc-repo-'));
		const afuera = mkdtempSync(join(tmpdir(), 'rtbc-afuera-'));
		writeFileSync(join(afuera, 'secreto.txt'), 'no debe leerse');
		try {
			symlinkSync(join(afuera, 'secreto.txt'), join(raiz, 'enlace.txt'));
		} catch {
			ctx.skip(); // crear enlaces simbólicos puede requerir permisos en Windows
		}

		expect(() => abrirRepositorio(raiz).leer('enlace.txt')).toThrow(AccesoDenegadoError);
	});
});
