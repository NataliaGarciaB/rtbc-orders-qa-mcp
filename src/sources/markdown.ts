/** Filas de la primera tabla Markdown que aparece después del título indicado, como listas de celdas. */
export function filasDeTabla(markdown: string, titulo: string): string[][] {
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
