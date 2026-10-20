import { readFileSync } from 'node:fs';
import { join } from 'node:path';

/** Raíz de la copia de los artefactos de la API v1.0.0 (ver test/fixtures/README.md). */
export const RAIZ_API = join(import.meta.dirname, 'fixtures', 'api-v1.0.0');

/** Lee un artefacto de la copia de la API, para probar los lectores con su contenido real. */
export function artefacto(ruta: string): string {
	return readFileSync(join(RAIZ_API, ruta), 'utf8');
}
