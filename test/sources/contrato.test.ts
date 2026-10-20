import { describe, expect, it } from 'vitest';
import { leerContrato } from '../../src/sources/contrato.js';
import { FormatoInvalidoError } from '../../src/sources/errores.js';
import { artefacto } from '../fixtures.js';

const ARCHIVO = 'docs/api/openapi.yaml';

describe('Lector del contrato OpenAPI (docs/api/openapi.yaml)', () => {
	it('lee las 14 respuestas declaradas, ordenadas por ruta, método y código', () => {
		const { respuestas } = leerContrato(artefacto(ARCHIVO));

		expect(respuestas.map((r) => `${r.metodo} ${r.ruta} ${r.http}`)).toEqual([
			'GET /orders 200',
			'POST /orders 201',
			'POST /orders 400',
			'GET /orders/{id} 200',
			'GET /orders/{id} 400',
			'GET /orders/{id} 404',
			'DELETE /orders/{id} 200',
			'DELETE /orders/{id} 400',
			'DELETE /orders/{id} 404',
			'DELETE /orders/{id} 409',
			'PATCH /orders/{id}/status 200',
			'PATCH /orders/{id}/status 400',
			'PATCH /orders/{id}/status 404',
			'PATCH /orders/{id}/status 409',
		]);
	});

	it('lee las restricciones de cada campo de los requests, incluidos los de objetos anidados', () => {
		const { esquemasRequest } = leerContrato(artefacto(ARCHIVO));

		expect(esquemasRequest).toEqual([
			{
				nombre: 'CreateOrderRequest',
				campos: [
					{ campo: 'cliente', tipo: 'string', obligatorio: true, minLength: 1, maxLength: 120 },
					{ campo: 'productos', tipo: 'array', obligatorio: true, minItems: 1 },
					{ campo: 'productos[].id', tipo: 'string', obligatorio: true, minLength: 1, maxLength: 50 },
					{ campo: 'productos[].cantidad', tipo: 'integer', obligatorio: true, minimum: 1 },
					{ campo: 'productos[].valorUnitario', tipo: 'number', obligatorio: true, minimum: 0.01, multipleOf: 0.01 },
				],
			},
			{
				nombre: 'UpdateStatusRequest',
				campos: [
					{
						campo: 'estado',
						tipo: 'string',
						obligatorio: true,
						enum: ['CREADA', 'PAGADA', 'ENVIADA', 'ENTREGADA', 'CANCELADA'],
					},
				],
			},
		]);
	});

	it('ordena las respuestas aunque el contrato las declare desordenadas e ignora "default"', () => {
		const contrato = [
			'openapi: 3.0.3',
			'paths:',
			'  /z:',
			'    delete:',
			'      responses: { "404": {}, "200": {} }',
			'  /a:',
			'    post:',
			'      responses: { "201": {}, default: {} }',
			'    get:',
			'      responses: { "200": {} }',
		].join('\n');

		expect(leerContrato(contrato).respuestas.map((r) => `${r.metodo} ${r.ruta} ${r.http}`)).toEqual([
			'GET /a 200',
			'POST /a 201',
			'DELETE /z 200',
			'DELETE /z 404',
		]);
	});

	it('distingue los campos opcionales', () => {
		const contrato = artefacto(ARCHIVO).replace('required: [cliente, productos]', 'required: [productos]');

		const cliente = leerContrato(contrato).esquemasRequest[0]?.campos.find((c) => c.campo === 'cliente');

		expect(cliente?.obligatorio).toBe(false);
	});

	describe('rechaza un documento con formato inesperado (Q03)', () => {
		const esperado = new FormatoInvalidoError(ARCHIVO, 'un documento OpenAPI 3 en YAML con las secciones "openapi" y "paths"');

		it('YAML inválido', () => {
			expect(() => leerContrato('openapi: [3.0\n  paths: :')).toThrow(esperado);
		});

		it('sin la sección paths', () => {
			expect(() => leerContrato('openapi: 3.0.3\ninfo:\n  title: x\n')).toThrow(esperado);
		});

		it('sin la versión de OpenAPI', () => {
			expect(() => leerContrato('paths: {}\n')).toThrow(esperado);
		});

		it('una referencia a un esquema inexistente', () => {
			const roto = artefacto(ARCHIVO).replace("$ref: '#/components/schemas/ProductLine'", "$ref: '#/components/schemas/NoExiste'");

			expect(() => leerContrato(roto)).toThrow(
				new FormatoInvalidoError(ARCHIVO, 'que la referencia "#/components/schemas/NoExiste" exista en el documento'),
			);
		});
	});
});
