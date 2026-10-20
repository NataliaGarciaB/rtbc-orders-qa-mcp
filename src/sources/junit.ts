import type { PruebaJUnit } from '../core/modelos.js';

const ANOTACIONES_DE_PRUEBA = new Set(['Test', 'ParameterizedTest', 'RepeatedTest', 'TestFactory', 'TestTemplate']);
const NO_SON_METODOS = new Set(['if', 'for', 'while', 'switch', 'catch', 'synchronized', 'return', 'new', 'super', 'this']);

/**
 * Elementos que importan para la trazabilidad, en orden: anotaciones (con su texto si lo tienen), declaraciones de
 * clase, llaves y nombres seguidos de paréntesis (declaraciones de método o llamadas).
 */
const ELEMENTO = /@(\w+)(?:\s*\(\s*"§(\d+)§"\s*\))?|\b(?:class|interface|enum|record)\s+(\w+)|([{}])|\b(\w+)\s*\(/g;

interface Ambito {
	clase?: string;
	etiquetas: string[];
}

/**
 * Lee de un archivo de pruebas JUnit cada método de prueba con sus etiquetas `@Tag`, sumando las de las clases que lo
 * contienen (incluidas las clases `@Nested`). Comentarios, textos y text blocks no se interpretan como código.
 */
export function leerPruebasJUnit(fuente: string, archivo: string): PruebaJUnit[] {
	const { codigo, textos } = limpiar(fuente);
	const pruebas: PruebaJUnit[] = [];
	const ambitos: Ambito[] = [];
	let etiquetasPendientes: string[] = [];
	let esPrueba = false;
	let clasePendiente: string | undefined;

	for (const [, anotacion, indiceTexto, clase, llave, nombre] of codigo.matchAll(ELEMENTO)) {
		if (anotacion === 'Tag' && indiceTexto !== undefined) {
			etiquetasPendientes.push(textos[Number(indiceTexto)] ?? '');
		} else if (anotacion) {
			esPrueba ||= ANOTACIONES_DE_PRUEBA.has(anotacion);
		} else if (clase) {
			clasePendiente = clase;
		} else if (llave === '{') {
			ambitos.push(clasePendiente ? { clase: clasePendiente, etiquetas: etiquetasPendientes } : { etiquetas: [] });
			if (clasePendiente) etiquetasPendientes = [];
			clasePendiente = undefined;
		} else if (llave === '}') {
			ambitos.pop();
		} else if (nombre && !NO_SON_METODOS.has(nombre)) {
			if (esPrueba) {
				const clases = ambitos.filter((ambito) => ambito.clase !== undefined);
				pruebas.push({
					archivo,
					clase: clases.map((ambito) => ambito.clase).join('.'),
					metodo: nombre,
					etiquetas: [...clases.flatMap((ambito) => ambito.etiquetas), ...etiquetasPendientes],
				});
			}
			etiquetasPendientes = [];
			esPrueba = false;
		}
	}
	return pruebas;
}

/**
 * Quita comentarios, vacía los text blocks y reemplaza cada texto por un marcador `"§n§"`, guardando su valor. Así
 * las llaves y paréntesis dentro de textos no alteran la estructura.
 */
function limpiar(fuente: string): { codigo: string; textos: string[] } {
	const textos: string[] = [];
	let codigo = '';
	let i = 0;

	const hasta = (fin: string, desde: number): number => {
		const posicion = fuente.indexOf(fin, desde);
		return posicion < 0 ? fuente.length : posicion + fin.length;
	};
	const literalHasta = (comilla: string, desde: number): number => {
		let j = desde;
		while (j < fuente.length && fuente[j] !== comilla) j += fuente[j] === '\\' ? 2 : 1;
		return j + 1;
	};

	while (i < fuente.length) {
		if (fuente.startsWith('//', i)) {
			i = hasta('\n', i) - 1;
		} else if (fuente.startsWith('/*', i)) {
			i = hasta('*/', i);
		} else if (fuente.startsWith('"""', i)) {
			i = hasta('"""', i + 3);
			codigo += '""';
		} else if (fuente[i] === '"') {
			const fin = literalHasta('"', i + 1);
			textos.push(fuente.slice(i + 1, fin - 1).replaceAll('\\"', '"'));
			codigo += `"§${textos.length - 1}§"`;
			i = fin;
		} else if (fuente[i] === "'") {
			i = literalHasta("'", i + 1);
			codigo += "' '";
		} else {
			codigo += fuente[i];
			i += 1;
		}
	}
	return { codigo, textos };
}
