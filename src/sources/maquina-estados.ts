import { ESTADOS, esEstado, type Estado, type Transicion } from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';

const SECCION = '## Matriz completa de transiciones';
const ESPERADO =
	'la tabla "Matriz completa de transiciones" con los 5 estados como filas y columnas y un código HTTP en cada celda';

/**
 * Lee la matriz 5 × 5 de docs/maquina-estados.md. La tabla es la fuente de verdad: el resultado se ordena por
 * estado de origen y de destino, en el orden del ciclo de vida.
 */
export function leerMaquinaEstados(markdown: string, archivo = 'docs/maquina-estados.md'): Transicion[] {
	const formatoInvalido = () => new FormatoInvalidoError(archivo, ESPERADO);

	const filas = filasDeTabla(markdown, SECCION);
	const [encabezado, , ...cuerpo] = filas;
	if (!encabezado) throw formatoInvalido();

	const destinos = encabezado.slice(1);
	if (!sonLosCincoEstados(destinos) || cuerpo.length !== ESTADOS.length) throw formatoInvalido();

	const transiciones = cuerpo.flatMap((celdas) => {
		const origen = sinFormato(celdas[0] ?? '');
		if (!esEstado(origen) || celdas.length !== destinos.length + 1) throw formatoInvalido();

		return destinos.map((destino, i) => {
			const celda = celdas[i + 1] ?? '';
			const http = celda.match(/\b(\d{3})\b/)?.[1];
			if (!http) throw formatoInvalido();
			const regla = celda.match(/\bRN-\d{2}\b/)?.[0];
			const transicion: Transicion = { origen, destino, http: Number(http), valida: http.startsWith('2') };
			return regla ? { ...transicion, regla } : transicion;
		});
	});

	if (!sonLosCincoEstados(cuerpo.map((celdas) => sinFormato(celdas[0] ?? '')))) throw formatoInvalido();
	return transiciones.sort((a, b) => indice(a.origen) - indice(b.origen) || indice(a.destino) - indice(b.destino));
}

/** Filas de la primera tabla Markdown que aparece después del título indicado, como listas de celdas. */
function filasDeTabla(markdown: string, titulo: string): string[][] {
	const lineas = markdown.split(/\r?\n/);
	const inicio = lineas.findIndex((linea) => linea.trim() === titulo);
	if (inicio < 0) return [];

	const tabla: string[][] = [];
	for (const linea of lineas.slice(inicio + 1)) {
		const texto = linea.trim();
		if (texto.startsWith('|')) {
			tabla.push(texto.slice(1, texto.endsWith('|') ? -1 : undefined).split('|').map((celda) => celda.trim()));
		} else if (tabla.length > 0) {
			break;
		}
	}
	return tabla;
}

function sinFormato(celda: string): string {
	return celda.replaceAll('*', '').trim();
}

function sonLosCincoEstados(valores: string[]): valores is Estado[] {
	return valores.length === ESTADOS.length && ESTADOS.every((estado) => valores.includes(estado));
}

function indice(estado: Estado): number {
	return ESTADOS.indexOf(estado);
}
