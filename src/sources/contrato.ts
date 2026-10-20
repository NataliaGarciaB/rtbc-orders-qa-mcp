import { parse } from 'yaml';
import {
	METODOS_HTTP,
	type CampoContrato,
	type Contrato,
	type EsquemaRequest,
	type MetodoHttp,
	type RespuestaContrato,
} from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';

const ESPERADO = 'un documento OpenAPI 3 en YAML con las secciones "openapi" y "paths"';
const RESTRICCIONES = ['minLength', 'maxLength', 'minimum', 'multipleOf', 'minItems'] as const;

type Nodo = Record<string, unknown>;

/** Lee del contrato OpenAPI las respuestas declaradas y las restricciones de los campos de cada request. */
export function leerContrato(yaml: string, archivo = 'docs/api/openapi.yaml'): Contrato {
	const documento = interpretar(yaml, archivo);
	const resolver = (nodo: Nodo): Nodo => resolverReferencia(documento, nodo, archivo);

	const respuestas: RespuestaContrato[] = [];
	const esquemas = new Map<string, EsquemaRequest>();

	for (const [ruta, operaciones] of Object.entries(objeto(documento['paths']))) {
		for (const metodo of METODOS_HTTP) {
			const operacion = objeto(objeto(operaciones)[metodo.toLowerCase()]);
			for (const codigo of Object.keys(objeto(operacion['responses']))) {
				if (/^\d{3}$/.test(codigo)) respuestas.push({ metodo, ruta, http: Number(codigo) });
			}
			const esquema = objeto(objeto(objeto(objeto(operacion['requestBody'])['content'])['application/json'])['schema']);
			const nombre = nombreDeReferencia(esquema);
			if (nombre && !esquemas.has(nombre)) {
				esquemas.set(nombre, { nombre, campos: camposDe(resolver(esquema), '', resolver) });
			}
		}
	}

	return {
		respuestas: respuestas.sort(
			(a, b) => a.ruta.localeCompare(b.ruta) || orden(a.metodo) - orden(b.metodo) || a.http - b.http,
		),
		esquemasRequest: [...esquemas.values()],
	};
}

function interpretar(yaml: string, archivo: string): Nodo {
	let documento: unknown;
	try {
		documento = parse(yaml);
	} catch {
		throw new FormatoInvalidoError(archivo, ESPERADO);
	}
	const raiz = objeto(documento);
	if (typeof raiz['openapi'] !== 'string' || typeof raiz['paths'] !== 'object' || raiz['paths'] === null) {
		throw new FormatoInvalidoError(archivo, ESPERADO);
	}
	return raiz;
}

/** Campos de un esquema de objeto; las listas de objetos se recorren con el prefijo `campo[].`. */
function camposDe(esquema: Nodo, prefijo: string, resolver: (nodo: Nodo) => Nodo): CampoContrato[] {
	const obligatorios = Array.isArray(esquema['required']) ? (esquema['required'] as string[]) : [];

	return Object.entries(objeto(esquema['properties'])).flatMap(([nombre, valor]) => {
		const propiedad = resolver(objeto(valor));
		const campo: CampoContrato = {
			campo: prefijo + nombre,
			tipo: String(propiedad['type']),
			obligatorio: obligatorios.includes(nombre),
			...restricciones(propiedad),
		};
		const items = objeto(propiedad['items']);
		const anidados =
			propiedad['type'] === 'array' && Object.keys(items).length > 0
				? camposDe(resolver(items), `${prefijo}${nombre}[].`, resolver)
				: [];
		return [campo, ...anidados];
	});
}

function restricciones(propiedad: Nodo): Partial<CampoContrato> {
	const resultado: Partial<CampoContrato> = {};
	for (const clave of RESTRICCIONES) {
		if (typeof propiedad[clave] === 'number') resultado[clave] = propiedad[clave];
	}
	if (Array.isArray(propiedad['enum'])) resultado.enum = propiedad['enum'].map(String);
	return resultado;
}

function resolverReferencia(documento: Nodo, nodo: Nodo, archivo: string): Nodo {
	const referencia = nodo['$ref'];
	if (typeof referencia !== 'string') return nodo;
	const destino = referencia
		.replace(/^#\//, '')
		.split('/')
		.reduce<unknown>((actual, parte) => objeto(actual)[parte], documento);
	if (typeof destino !== 'object' || destino === null) {
		throw new FormatoInvalidoError(archivo, `que la referencia "${referencia}" exista en el documento`);
	}
	return destino as Nodo;
}

function nombreDeReferencia(esquema: Nodo): string | undefined {
	const referencia = esquema['$ref'];
	return typeof referencia === 'string' ? referencia.split('/').at(-1) : undefined;
}

function objeto(valor: unknown): Nodo {
	return typeof valor === 'object' && valor !== null ? (valor as Nodo) : {};
}

function orden(metodo: MetodoHttp): number {
	return METODOS_HTTP.indexOf(metodo);
}
