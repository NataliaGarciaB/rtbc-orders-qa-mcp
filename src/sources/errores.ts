/** Una ruta pedida queda fuera del repositorio de la API (ADR-007). */
export class AccesoDenegadoError extends Error {
	constructor(readonly ruta: string) {
		super(`Acceso denegado: "${ruta}" está fuera del repositorio de la API`);
		this.name = 'AccesoDenegadoError';
	}
}

/** El archivo o el repositorio pedido no existe. */
export class ArchivoNoEncontradoError extends Error {
	constructor(readonly ruta: string) {
		super(`No se encontró "${ruta}" en el repositorio de la API`);
		this.name = 'ArchivoNoEncontradoError';
	}
}

/**
 * Un artefacto no tiene la estructura esperada. Se informa el archivo y lo que se esperaba para no devolver
 * un resultado silenciosamente incompleto (riesgo Q03).
 */
export class FormatoInvalidoError extends Error {
	constructor(
		readonly archivo: string,
		readonly esperado: string,
	) {
		super(`${archivo}: formato no reconocido. Se esperaba ${esperado}`);
		this.name = 'FormatoInvalidoError';
	}
}
