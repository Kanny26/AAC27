package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol.NombreRol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.entity.Cliente;
import com.aac27.joyeria.module.venta.entity.Venta;
import com.aac27.joyeria.module.venta.repository.ClienteRepository;
import com.aac27.joyeria.module.venta.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D1 / RF17 / RF18: el historial de ventas por cliente respeta el alcance del vendedor.
 *
 * <p>Vendedor: solo ve las ventas que él registró. Administrador: ve todas.
 * Si el cliente solo tiene ventas de otros vendedores, la respuesta es 200 con página vacía
 * (no 404, para no revelar que existen ventas ajenas).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentaClienteAlcanceTest extends AbstractAuthTest {

    private static final AtomicLong SEQ = new AtomicLong(1);

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VentaRepository ventaRepository;

    private Cliente crearCliente() {
        long n = SEQ.getAndIncrement();
        return clienteRepository.saveAndFlush(Cliente.builder()
                .nombre("Cliente D1 " + n)
                .numeroDocumento("8" + String.format("%09d", n))
                .build());
    }

    private Venta crearVenta(Cliente cliente, Usuario vendedor, String factura) {
        return ventaRepository.saveAndFlush(Venta.builder()
                .numeroFactura(factura)
                .cliente(cliente)
                .usuario(vendedor)
                .modalidadPago(Venta.ModalidadPago.contado)
                .build());
    }

    @Test
    void vendedor_veSoloSusVentas_delCliente() throws Exception {
        Usuario a = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Usuario b = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Cliente cliente = crearCliente();
        crearVenta(cliente, a, "D1-A-" + SEQ.getAndIncrement());
        crearVenta(cliente, b, "D1-B-" + SEQ.getAndIncrement());

        mockMvc.perform(get("/api/v1/ventas/cliente/" + cliente.getClienteId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generarAccessToken(a))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].numeroFactura").value(
                        ventaRepository.findByUsuarioUsuarioIdOrderByFechaVentaDesc(
                                        a.getUsuarioId(), org.springframework.data.domain.Pageable.unpaged())
                                .getContent().get(0).getNumeroFactura()));
    }

    @Test
    void vendedor_clienteSoloConVentasAjenas_recibePaginaVacia() throws Exception {
        Usuario a = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Usuario b = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Cliente cliente = crearCliente();
        crearVenta(cliente, b, "D1-X-" + SEQ.getAndIncrement());

        mockMvc.perform(get("/api/v1/ventas/cliente/" + cliente.getClienteId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generarAccessToken(a))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void administrador_veTodasLasVentas_delCliente() throws Exception {
        Usuario admin = crearUsuario(NombreRol.administrador, Usuario.EstadoUsuario.activo);
        Usuario a = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Usuario b = crearUsuario(NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        Cliente cliente = crearCliente();
        crearVenta(cliente, a, "D1-AD-A-" + SEQ.getAndIncrement());
        crearVenta(cliente, b, "D1-AD-B-" + SEQ.getAndIncrement());

        mockMvc.perform(get("/api/v1/ventas/cliente/" + cliente.getClienteId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generarAccessToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }
}