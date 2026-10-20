package com.rtbc.orders.entrypoint.rest;

import static com.rtbc.orders.domain.OrderFixtures.CUSTOMER;
import static com.rtbc.orders.domain.OrderFixtures.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rtbc.orders.domain.Order;
import com.rtbc.orders.domain.OrderFixtures;
import com.rtbc.orders.domain.OrderStatus;
import com.rtbc.orders.domain.ProductLine;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

@DisplayName("API REST de órdenes")
class OrderControllerTest extends OrderControllerTestSupport {

	@Nested
	@DisplayName("POST /orders")
	class Crear {

		@Test
		@DisplayName("responde 201 con Location y la orden creada")
		void creaOrden() throws Exception {
			Order order = Order.create(CUSTOMER, List.of(line("P001", 2, "100000")));
			when(createOrder.execute(anyString(), anyList())).thenReturn(order);

			mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_JSON))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", endsWith("/orders/" + order.id())))
					.andExpect(jsonPath("$.id").value(order.id().toString()))
					.andExpect(jsonPath("$.cliente").value(CUSTOMER))
					.andExpect(jsonPath("$.productos[0].id").value("P001"))
					.andExpect(jsonPath("$.productos[0].cantidad").value(2))
					.andExpect(jsonPath("$.productos[0].valorUnitario").value(100000))
					.andExpect(jsonPath("$.total").value(200000))
					.andExpect(jsonPath("$.estado").value("CREADA"));
		}

		@Test
		@DisplayName("traduce el request a cliente y líneas de producto del dominio")
		void mapeaRequest() throws Exception {
			when(createOrder.execute(anyString(), anyList())).thenReturn(OrderFixtures.validOrder());

			mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_JSON))
					.andExpect(status().isCreated());

			@SuppressWarnings("unchecked")
			ArgumentCaptor<List<ProductLine>> products = ArgumentCaptor.forClass(List.class);
			verify(createOrder).execute(eq(CUSTOMER), products.capture());
			assertThat(products.getValue()).containsExactly(line("P001", 2, "100000"));
		}

		@Test
		@Tag("RN-03")
		@DisplayName("RN-03: ignora total y estado enviados por el cliente")
		void ignoraTotalYEstado() throws Exception {
			when(createOrder.execute(anyString(), anyList()))
					.thenReturn(Order.create(CUSTOMER, List.of(line("P001", 2, "100000"))));

			mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "cliente": "Juan Pérez",
					  "productos": [ { "id": "P001", "cantidad": 2, "valorUnitario": 100000 } ],
					  "total": 1,
					  "estado": "ENTREGADA"
					}
					"""))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.total").value(200000))
					.andExpect(jsonPath("$.estado").value("CREADA"));
		}

		@Test
		@DisplayName("escribe los decimales sin notación científica")
		void decimalesSinNotacionCientifica() throws Exception {
			when(createOrder.execute(anyString(), anyList()))
					.thenReturn(Order.create(CUSTOMER, List.of(line("P001", 2, "1E+5"))));

			mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER_JSON))
					.andExpect(status().isCreated())
					.andExpect(content().string(containsString("\"valorUnitario\":100000")))
					.andExpect(content().string(containsString("\"total\":200000")));
		}
	}

	@Nested
	@DisplayName("GET /orders/{id}")
	class Consultar {

		@Test
		@DisplayName("responde 200 con la orden")
		void consultaOrden() throws Exception {
			Order order = OrderFixtures.validOrder();
			when(getOrder.execute(order.id())).thenReturn(order);

			mvc.perform(get("/orders/{id}", order.id()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(order.id().toString()))
					.andExpect(jsonPath("$.estado").value("CREADA"));
		}
	}

	@Nested
	@DisplayName("PATCH /orders/{id}/status")
	class ActualizarEstado {

		@Test
		@DisplayName("responde 200 con la orden en el nuevo estado")
		void actualizaEstado() throws Exception {
			Order order = OrderFixtures.orderIn(OrderStatus.PAGADA);
			when(changeOrderStatus.execute(order.id(), OrderStatus.PAGADA)).thenReturn(order);

			mvc.perform(patch("/orders/{id}/status", order.id())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{ "estado": "PAGADA" }
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(order.id().toString()))
					.andExpect(jsonPath("$.estado").value("PAGADA"));
			verify(changeOrderStatus).execute(order.id(), OrderStatus.PAGADA);
		}
	}

	@Nested
	@DisplayName("DELETE /orders/{id}")
	class Cancelar {

		@Test
		@DisplayName("ADR-006: cancela lógicamente y responde 200 con la orden CANCELADA")
		void cancelaOrden() throws Exception {
			Order order = OrderFixtures.orderIn(OrderStatus.CANCELADA);
			when(cancelOrder.execute(order.id())).thenReturn(order);

			mvc.perform(delete("/orders/{id}", order.id()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.estado").value("CANCELADA"));
			verify(cancelOrder).execute(order.id());
		}
	}

	@Nested
	@DisplayName("GET /orders")
	class Listar {

		@Test
		@DisplayName("responde 200 con todas las órdenes")
		void listaOrdenes() throws Exception {
			when(listOrders.execute()).thenReturn(List.of(OrderFixtures.validOrder(), OrderFixtures.validOrder()));

			mvc.perform(get("/orders"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)));
		}

		@Test
		@DisplayName("responde 200 con una lista vacía si no hay órdenes")
		void listaVacia() throws Exception {
			when(listOrders.execute()).thenReturn(List.of());

			mvc.perform(get("/orders"))
					.andExpect(status().isOk())
					.andExpect(content().json("[]"));
		}
	}
}
