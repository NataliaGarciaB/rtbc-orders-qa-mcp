package com.rtbc.orders.entrypoint.rest;

import com.rtbc.orders.application.CancelOrder;
import com.rtbc.orders.application.ChangeOrderStatus;
import com.rtbc.orders.application.CreateOrder;
import com.rtbc.orders.application.GetOrder;
import com.rtbc.orders.application.ListOrders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de la capa REST: solo carga la capa web; los casos de uso se simulan.
 */
@WebMvcTest(OrderController.class)
abstract class OrderControllerTestSupport {

	@Autowired
	protected MockMvc mvc;

	@MockitoBean
	protected CreateOrder createOrder;

	@MockitoBean
	protected GetOrder getOrder;

	@MockitoBean
	protected ListOrders listOrders;

	@MockitoBean
	protected ChangeOrderStatus changeOrderStatus;

	@MockitoBean
	protected CancelOrder cancelOrder;

	protected static final String VALID_ORDER_JSON = """
			{
			  "cliente": "Juan Pérez",
			  "productos": [ { "id": "P001", "cantidad": 2, "valorUnitario": 100000 } ]
			}
			""";
}
