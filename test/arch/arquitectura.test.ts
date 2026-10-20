import { readdirSync, readFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * ADR-004: core contiene la lógica de análisis como funciones puras. No puede depender del protocolo MCP,
 * del sistema de archivos ni de las capas externas (sources, server). Riesgo Q08.
 */
const CORE_DIR = join(import.meta.dirname, '..', '..', 'src', 'core');

const FORBIDDEN = [
	{ pattern: /^@modelcontextprotocol\//, reason: 'el protocolo MCP pertenece a server' },
	{ pattern: /^(node:)?fs(\/promises)?$/, reason: 'leer archivos pertenece a sources' },
	{ pattern: /^\.\.\/(sources|server)(\/|$)/, reason: 'core no depende de las capas externas' },
];

function typescriptFiles(dir: string): string[] {
	return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
		const path = join(dir, entry.name);
		if (entry.isDirectory()) return typescriptFiles(path);
		return entry.name.endsWith('.ts') ? [path] : [];
	});
}

function importsOf(source: string): string[] {
	const specifiers = /(?:import|export)\s[^'"]*?from\s*['"]([^'"]+)['"]|import\s*\(\s*['"]([^'"]+)['"]\s*\)|import\s+['"]([^'"]+)['"]/g;
	return [...source.matchAll(specifiers)].map((m) => m[1] ?? m[2] ?? m[3] ?? '');
}

describe('Arquitectura (ADR-004)', () => {
	it('core no depende de MCP, del sistema de archivos ni de las capas externas', () => {
		const violations = typescriptFiles(CORE_DIR).flatMap((file) =>
			importsOf(readFileSync(file, 'utf8')).flatMap((specifier) =>
				FORBIDDEN.filter((rule) => rule.pattern.test(specifier)).map(
					(rule) => `${relative(CORE_DIR, file)} importa '${specifier}': ${rule.reason}`,
				),
			),
		);

		expect(violations).toEqual([]);
	});
});
