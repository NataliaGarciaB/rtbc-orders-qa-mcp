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
