import type { ReglaNegocio } from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';
import { filasDeTabla } from './markdown.js';

const SECCION = '### Reglas de negocio';
const ESPERADO = 'la tabla "Reglas de negocio" con las columnas Regla y Descripción, y al menos una regla RN-xx';

/** Lee la tabla de reglas de negocio del README de la API. */
export function leerReglasNegocio(markdown: string, archivo = 'README.md'): ReglaNegocio[] {
	const [encabezado, , ...cuerpo] = filasDeTabla(markdown, SECCION);
	const reglas = cuerpo.map(([id = '', descripcion = '']) => ({ id, descripcion }));

	const formatoValido =
		encabezado?.[0] === 'Regla' &&
		encabezado[1] === 'Descripción' &&
		reglas.length > 0 &&
		reglas.every((regla) => /^RN-\d{2}$/.test(regla.id));
	if (!formatoValido) throw new FormatoInvalidoError(archivo, ESPERADO);
	return reglas;
}
