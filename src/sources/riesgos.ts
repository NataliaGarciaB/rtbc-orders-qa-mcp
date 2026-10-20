import type { NivelRiesgo, Riesgo } from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';
import { filasDeTabla } from './markdown.js';

const SECCION = '## 2. Matriz de riesgos';
const ESPERADO =
	'la tabla "Matriz de riesgos" con las columnas ID, Riesgo, RN y Nivel, y un nivel "Alto", "Medio" o "Bajo" con su puntaje';

/** Lee la matriz de riesgos de docs/qa/estrategia-pruebas.md. Las columnas se ubican por su título. */
export function leerRiesgos(markdown: string, archivo = 'docs/qa/estrategia-pruebas.md'): Riesgo[] {
	const formatoInvalido = () => new FormatoInvalidoError(archivo, ESPERADO);

	const [encabezado = [], , ...cuerpo] = filasDeTabla(markdown, SECCION);
	const columna = {
		id: encabezado.indexOf('ID'),
		descripcion: encabezado.indexOf('Riesgo'),
		reglas: encabezado.indexOf('RN'),
		nivel: encabezado.indexOf('Nivel'),
	};
	if (Object.values(columna).some((indice) => indice < 0) || cuerpo.length === 0) throw formatoInvalido();

	return cuerpo.map((celdas) => {
		const nivel = (celdas[columna.nivel] ?? '').replaceAll('*', '').match(/^(\d+)\s+(Alto|Medio|Bajo)$/);
		if (!nivel) throw formatoInvalido();
		return {
			id: celdas[columna.id] ?? '',
			descripcion: celdas[columna.descripcion] ?? '',
			reglas: (celdas[columna.reglas] ?? '').match(/RN-\d{2}/g) ?? [],
			nivel: nivel[2] as NivelRiesgo,
			puntaje: Number(nivel[1]),
		};
	});
}
