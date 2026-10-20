import { ESTADOS, type Estado, type Transicion } from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';
import { filasDeTabla } from './markdown.js';

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

	// Filas y columnas deben ser exactamente los 5 estados: cubre filas faltantes, repetidas o desconocidas
	const destinos = encabezado.slice(1);
	const origenes = cuerpo.map((celdas) => sinFormato(celdas[0] ?? ''));
	if (!sonLosCincoEstados(destinos) || !sonLosCincoEstados(origenes)) throw formatoInvalido();

	const transiciones = cuerpo.flatMap((celdas, fila) =>
		destinos.map((destino, columna) => {
			const celda = celdas[columna + 1] ?? '';
			const http = celda.match(/\b(\d{3})\b/)?.[1];
			if (!http) throw formatoInvalido();
			const regla = celda.match(/\bRN-\d{2}\b/)?.[0];
			const transicion: Transicion = { origen: origenes[fila] as Estado, destino, http: Number(http), valida: http.startsWith('2') };
			return regla ? { ...transicion, regla } : transicion;
		}),
	);

	return transiciones.sort((a, b) => indice(a.origen) - indice(b.origen) || indice(a.destino) - indice(b.destino));
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
