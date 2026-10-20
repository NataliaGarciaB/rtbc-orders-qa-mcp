/** Estados de una orden, en el orden del ciclo de vida. */
export const ESTADOS = ['CREADA', 'PAGADA', 'ENVIADA', 'ENTREGADA', 'CANCELADA'] as const;
export type Estado = (typeof ESTADOS)[number];

export function esEstado(valor: string): valor is Estado {
	return (ESTADOS as readonly string[]).includes(valor);
}

/** Comportamiento esperado al pasar de un estado a otro (una celda de la matriz 5 × 5). */
export interface Transicion {
	origen: Estado;
	destino: Estado;
	http: number;
	valida: boolean;
	/** Regla de negocio que explica el rechazo, si la celda la menciona (por ejemplo RN-05). */
	regla?: string;
}

export type NivelRiesgo = 'Alto' | 'Medio' | 'Bajo';

/** Riesgo de la matriz del proyecto (probabilidad × impacto = puntaje). */
export interface Riesgo {
	id: string;
	descripcion: string;
	/** Reglas de negocio que afecta (vacío si es transversal). */
	reglas: string[];
	nivel: NivelRiesgo;
	puntaje: number;
}

export interface ReglaNegocio {
	id: string;
	descripcion: string;
}

export const METODOS_HTTP = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;
export type MetodoHttp = (typeof METODOS_HTTP)[number];

/** Una respuesta declarada en el contrato: método, ruta (con plantillas como {id}) y código HTTP. */
export interface RespuestaContrato {
	metodo: MetodoHttp;
	ruta: string;
	http: number;
}

/** Restricciones de un campo de request. Los campos de listas de objetos usan la forma `productos[].id`. */
export interface CampoContrato {
	campo: string;
	tipo: string;
	obligatorio: boolean;
	minLength?: number;
	maxLength?: number;
	minimum?: number;
	multipleOf?: number;
	minItems?: number;
	enum?: string[];
}

export interface EsquemaRequest {
	nombre: string;
	campos: CampoContrato[];
}

export interface Contrato {
	respuestas: RespuestaContrato[];
	esquemasRequest: EsquemaRequest[];
}

/** Una petición HTTP dentro de un escenario de aceptación, con el estado de la orden en ese momento. */
export interface PeticionKarate {
	metodo: MetodoHttp;
	/** Ruta con `{id}` donde el escenario usa una variable; los valores literales se conservan. */
	ruta: string;
	http?: number;
	/** Estado de la orden antes de la petición, si el escenario lo establece. */
	origen?: Estado;
	/** Estado pedido en el body (`request { estado: '...' }`); puede ser un valor inválido a propósito. */
	destino?: string;
}

export interface EscenarioKarate {
	archivo: string;
	nombre: string;
	etiquetas: string[];
	peticiones: PeticionKarate[];
}
