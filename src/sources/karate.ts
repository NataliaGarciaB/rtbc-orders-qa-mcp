import { AstBuilder, GherkinClassicTokenMatcher, Parser } from '@cucumber/gherkin';
import { IdGenerator, type GherkinDocument, type Scenario } from '@cucumber/messages';
import {
	METODOS_HTTP,
	esEstado,
	type Estado,
	type EscenarioKarate,
	type MetodoHttp,
	type PeticionKarate,
} from '../core/modelos.js';
import { FormatoInvalidoError } from './errores.js';

const ESPERADO = 'un archivo .feature con sintaxis Gherkin válida';

interface EscenarioExpandido {
	nombre: string;
	etiquetas: string[];
	pasos: string[];
}

/**
 * Lee un archivo .feature de Karate con el parser oficial de Gherkin (ADR-006) e interpreta, paso a paso, las
 * peticiones que hace cada escenario. Los Scenario Outline se expanden fila por fila; los helpers (@ignore) se omiten.
 */
export function leerFeatureKarate(contenido: string, archivo: string): EscenarioKarate[] {
	const feature = interpretar(contenido, archivo).feature;
	const etiquetasFeature = feature?.tags.map((etiqueta) => etiqueta.name) ?? [];
	if (!feature || etiquetasFeature.includes('@ignore')) return [];

	return feature.children
		.flatMap((hijo) => (hijo.scenario ? expandir(hijo.scenario, etiquetasFeature) : []))
		.map(({ nombre, etiquetas, pasos }) => ({ archivo, nombre, etiquetas, peticiones: peticionesDe(pasos) }));
}

function interpretar(contenido: string, archivo: string): GherkinDocument {
	try {
		return new Parser(new AstBuilder(IdGenerator.incrementing()), new GherkinClassicTokenMatcher()).parse(contenido);
	} catch {
		throw new FormatoInvalidoError(archivo, ESPERADO);
	}
}

/** Un escenario simple produce un escenario; un Scenario Outline, uno por cada fila de sus ejemplos. */
function expandir(escenario: Scenario, etiquetasFeature: string[]): EscenarioExpandido[] {
	const etiquetas = [...etiquetasFeature, ...escenario.tags.map((etiqueta) => etiqueta.name)];
	const pasos = escenario.steps.map((paso) => paso.text);
	if (escenario.examples.length === 0) {
		return [{ nombre: escenario.name, etiquetas, pasos }];
	}

	return escenario.examples.flatMap((ejemplos) => {
		const columnas = ejemplos.tableHeader?.cells.map((celda) => celda.value) ?? [];
		return ejemplos.tableBody.map((fila) => {
			const valores = new Map(columnas.map((columna, i) => [columna, fila.cells[i]?.value ?? '']));
			const sustituir = (texto: string) =>
				texto.replace(/<([^>]+)>/g, (marcador, columna: string) => valores.get(columna) ?? marcador);
			return {
				nombre: sustituir(escenario.name),
				etiquetas: [...etiquetas, ...ejemplos.tags.map((etiqueta) => etiqueta.name)],
				pasos: pasos.map(sustituir),
			};
		});
	});
}

/**
 * Recorre los pasos como lo haría Karate: `path` y `request` preparan la petición, `method` la envía (y las
 * reinicia) y `status` registra el código esperado. El estado de la orden lo fijan los helpers de la API y
 * cambia tras una transición exitosa.
 */
function peticionesDe(pasos: string[]): PeticionKarate[] {
	const peticiones: PeticionKarate[] = [];
	let ruta: string | undefined;
	let destino: string | undefined;
	let origen: Estado | undefined;

	for (const paso of pasos) {
		origen = estadoFijadoPorHelper(paso) ?? origen;

		const camino = paso.match(/^path\s+(.+)$/)?.[1];
		const estadoPedido = paso.match(/^request\s+\{.*\bestado:\s*'([^']+)'/)?.[1];
		const metodo = paso.match(/^method\s+(\w+)/)?.[1]?.toUpperCase();
		const codigo = paso.match(/^status\s+(\d{3})\b/)?.[1];

		if (camino) ruta = rutaDe(camino);
		if (estadoPedido) destino = estadoPedido;
		if (metodo && esMetodo(metodo)) {
			peticiones.push({
				metodo,
				ruta: ruta ?? '/',
				...(origen ? { origen } : {}),
				...(destino ? { destino } : {}),
			});
			ruta = undefined;
			destino = undefined;
		}
		const ultima = peticiones.at(-1);
		if (codigo && ultima) {
			ultima.http = Number(codigo);
			origen = estadoDespuesDe(ultima) ?? origen;
		}
	}
	return peticiones;
}

function estadoFijadoPorHelper(paso: string): Estado | undefined {
	const estado = paso.match(/helpers\/orden-en-estado\.feature'\)\s*\{\s*estado:\s*'([^']+)'/)?.[1];
	if (estado && esEstado(estado)) return estado;
	return /helpers\/crear-orden\.feature/.test(paso) ? 'CREADA' : undefined;
}

/** Estado de la orden después de una transición exitosa; undefined si la petición no la cambia. */
function estadoDespuesDe(peticion: PeticionKarate): Estado | undefined {
	const exitosa = peticion.http !== undefined && peticion.http >= 200 && peticion.http < 300;
	if (!exitosa || !peticion.origen) return undefined;
	if (peticion.metodo === 'DELETE') return 'CANCELADA';
	return peticion.metodo === 'PATCH' && peticion.destino && esEstado(peticion.destino) ? peticion.destino : undefined;
}

/** `'orders', orden.orderId, 'status'` → `/orders/{id}/status`: los literales se conservan y las variables son {id}. */
function rutaDe(argumentos: string): string {
	const segmentos = argumentos.split(',').map((argumento) => {
		const literal = argumento.trim().match(/^['"](.*)['"]$/)?.[1];
		return literal ?? '{id}';
	});
	return `/${segmentos.join('/')}`;
}

function esMetodo(valor: string): valor is MetodoHttp {
	return (METODOS_HTTP as readonly string[]).includes(valor);
}
