import { existsSync, readdirSync, readFileSync, realpathSync } from 'node:fs';
import { isAbsolute, join, relative, resolve, sep } from 'node:path';
import { AccesoDenegadoError, ArchivoNoEncontradoError } from './errores.js';

/** Acceso de solo lectura a los artefactos de la API, siempre dentro de su raíz (ADR-007). */
export interface RepositorioApi {
	/** Contenido de un archivo, por su ruta relativa a la raíz. */
	leer(ruta: string): string;
	/** Rutas relativas (con `/`) de los archivos con esa extensión, recursivamente y ordenadas. */
	listar(directorio: string, extension: string): string[];
}

export function abrirRepositorio(raiz: string): RepositorioApi {
	if (!existsSync(raiz)) {
		throw new ArchivoNoEncontradoError(raiz);
	}
	const base = realpathSync(raiz);

	const resolver = (ruta: string): string => {
		if (isAbsolute(ruta)) {
			throw new AccesoDenegadoError(ruta);
		}
		const pedida = resolve(base, ruta);
		if (!estaDentro(base, pedida)) {
			throw new AccesoDenegadoError(ruta);
		}
		if (!existsSync(pedida)) {
			throw new ArchivoNoEncontradoError(ruta);
		}
		// Un enlace simbólico dentro del repositorio no puede apuntar fuera de él
		const real = realpathSync(pedida);
		if (!estaDentro(base, real)) {
			throw new AccesoDenegadoError(ruta);
		}
		return real;
	};

	return {
		leer: (ruta) => readFileSync(resolver(ruta), 'utf8'),
		listar: (directorio, extension) =>
			archivosDe(resolver(directorio))
				.filter((archivo) => archivo.endsWith(extension))
				.map((archivo) => relative(base, archivo).split(sep).join('/'))
				.sort(),
	};
}

function estaDentro(base: string, ruta: string): boolean {
	const desdeBase = relative(base, ruta);
	return desdeBase === '' || (desdeBase !== '..' && !desdeBase.startsWith(`..${sep}`) && !isAbsolute(desdeBase));
}

function archivosDe(directorio: string): string[] {
	return readdirSync(directorio, { withFileTypes: true }).flatMap((entrada) => {
		const ruta = join(directorio, entrada.name);
		if (entrada.isDirectory()) return archivosDe(ruta);
		return entrada.isFile() ? [ruta] : [];
	});
}
