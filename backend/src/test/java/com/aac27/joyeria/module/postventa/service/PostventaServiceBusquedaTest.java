package com.aac27.joyeria.module.postventa.service;

import com.aac27.joyeria.module.compra.repository.InventarioMovimientoRepository;
import com.aac27.joyeria.module.compra.repository.ProductoLockRepository;
import com.aac27.joyeria.module.postventa.entity.CasoPostventa;
import com.aac27.joyeria.module.postventa.repository.CasoPostventaRepository;
import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.repository.ClienteRepository;
import com.aac27.joyeria.module.venta.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RF26: el vendedor solo consulta los casos de SUS ventas; el administrador ve todos.
 *
 * <p>Prueba unitaria con Mockito (sin base de datos): lo que se verifica es QUÉ consulta del
 * repositorio elige el servicio según el rol, que es donde vive la regla.
 */
@ExtendWith(MockitoExtension.class)
class PostventaServiceBusquedaTest {

    private static final String FACTURA = "FAC-000123";
    private static final Pageable PAGINA = PageRequest.of(0, 20);

    @Mock private CasoPostventaRepository casoRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ProductoLockRepository productoLockRepository;
    @Mock private InventarioMovimientoRepository movimientoRepository;

    @InjectMocks
    private PostventaService postventaService;

    private static Usuario usuarioConRol(Long id, Rol.NombreRol nombreRol) {
        Rol rol = new Rol();
        rol.setNombre(nombreRol);
        return Usuario.builder().usuarioId(id).rol(rol).build();
    }

    @Test
    @DisplayName("El vendedor solo busca entre los casos de las ventas que él registró")
    void vendedorBuscaSoloEnSusVentas() {
        Usuario vendedor = usuarioConRol(7L, Rol.NombreRol.vendedor);
        when(casoRepository.findByVentaNumeroFacturaYVendedor(FACTURA, 7L, PAGINA))
                .thenReturn(Page.<CasoPostventa>empty());

        postventaService.buscarPorFactura(FACTURA, PAGINA, vendedor);

        verify(casoRepository).findByVentaNumeroFacturaYVendedor(FACTURA, 7L, PAGINA);
        // La consulta sin filtro nunca debe usarse con un vendedor
        verify(casoRepository, never()).findByVentaNumeroFactura(any(), any());
    }

    @Test
    @DisplayName("El administrador busca por factura sin limitarse a un vendedor")
    void administradorBuscaEnTodasLasVentas() {
        Usuario admin = usuarioConRol(1L, Rol.NombreRol.administrador);
        when(casoRepository.findByVentaNumeroFactura(FACTURA, PAGINA))
                .thenReturn(Page.<CasoPostventa>empty());

        postventaService.buscarPorFactura(FACTURA, PAGINA, admin);

        verify(casoRepository).findByVentaNumeroFactura(FACTURA, PAGINA);
        verify(casoRepository, never()).findByVentaNumeroFacturaYVendedor(any(), any(), any());
    }
}
