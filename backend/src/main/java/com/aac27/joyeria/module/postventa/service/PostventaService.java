package com.aac27.joyeria.module.postventa.service;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.compra.entity.InventarioMovimiento;
import com.aac27.joyeria.module.compra.repository.InventarioMovimientoRepository;
import com.aac27.joyeria.module.compra.repository.ProductoLockRepository;
import com.aac27.joyeria.module.postventa.dto.CasoPostventaRequest;
import com.aac27.joyeria.module.postventa.dto.CasoPostventaResponse;
import com.aac27.joyeria.module.postventa.entity.CasoPostventa;
import com.aac27.joyeria.module.postventa.entity.DetalleCasoPostventa;
import com.aac27.joyeria.module.postventa.entity.HistorialCaso;
import com.aac27.joyeria.module.postventa.entity.Reparacion;
import com.aac27.joyeria.module.postventa.repository.CasoPostventaRepository;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.entity.Cliente;
import com.aac27.joyeria.module.venta.entity.DetalleVenta;
import com.aac27.joyeria.module.venta.entity.Venta;
import com.aac27.joyeria.module.venta.repository.ClienteRepository;
import com.aac27.joyeria.module.venta.repository.VentaRepository;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostventaService {

    private final CasoPostventaRepository casoRepository;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoLockRepository productoLockRepository;
    private final InventarioMovimientoRepository movimientoRepository;

    // ──────────────────────────────────────────────────────────
    // CREACIÓN DE CASOS
    // ──────────────────────────────────────────────────────────

    @Transactional
    public CasoPostventaResponse crearCaso(CasoPostventaRequest request, Usuario usuario) {
        
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", request.getClienteId()));

        Venta venta = null;
        if (request.getVentaId() != null) {
            venta = ventaRepository.findById(request.getVentaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Venta", request.getVentaId()));
            
            // Validar cantidades si hay ítems devueltos/cambiados (RF25)
            if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
                validarCantidades(venta, request);
            }
        }

        CasoPostventa.TipoCaso tipoCaso = CasoPostventa.TipoCaso.valueOf(request.getTipo().toLowerCase());

        // Polimorfismo: Dependiendo del tipo, instanciamos Reparacion o CasoPostventa
        CasoPostventa caso;
        if (tipoCaso == CasoPostventa.TipoCaso.reparacion) {
            caso = Reparacion.builder()
                    .venta(venta)
                    .cliente(cliente)
                    .usuario(usuario)
                    .tipo(tipoCaso)
                    .estado(CasoPostventa.EstadoCaso.abierto)
                    .descripcion(request.getDescripcion())
                    .descripcionTrabajo(request.getDescripcionTrabajo())
                    .presupuesto(request.getPresupuesto())
                    .fechaEntregaEstimada(request.getFechaEntregaEstimada())
                    .estadoReparacion(Reparacion.EstadoReparacion.recibido)
                    .build();
        } else {
            caso = CasoPostventa.builder()
                    .venta(venta)
                    .cliente(cliente)
                    .usuario(usuario)
                    .tipo(tipoCaso)
                    .estado(CasoPostventa.EstadoCaso.abierto)
                    .descripcion(request.getDescripcion())
                    .build();
        }

        // Agregar Detalles y procesar inventario si es Devolución o Cambio
        if (request.getDetalles() != null) {
            for (CasoPostventaRequest.DetalleCasoRequest detReq : request.getDetalles()) {
                Producto prod = productoLockRepository.findById(detReq.getProductoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Producto", detReq.getProductoId()));

                DetalleCasoPostventa detalle = DetalleCasoPostventa.builder()
                        .producto(prod)
                        .cantidad(detReq.getCantidad())
                        .motivo(detReq.getMotivo())
                        .estadoProducto(DetalleCasoPostventa.EstadoDevolucion.valueOf(detReq.getEstadoProducto().toLowerCase()))
                        .build();
                
                caso.agregarDetalle(detalle);

                // Generar movimiento de entrada si el producto está en buen estado
                if (detalle.getEstadoProducto() == DetalleCasoPostventa.EstadoDevolucion.bueno &&
                   (tipoCaso == CasoPostventa.TipoCaso.devolucion || tipoCaso == CasoPostventa.TipoCaso.cambio)) {
                    
                    int stockAntes = prod.getStock();
                    prod.incrementarStock(detalle.getCantidad());

                    InventarioMovimiento mov = InventarioMovimiento.builder()
                            .producto(prod)
                            .tipo(InventarioMovimiento.TipoMovimiento.entrada)
                            .cantidad(detalle.getCantidad())
                            .stockAntes(stockAntes)
                            .stockDespues(prod.getStock())
                            .referenciaTipo("postventa")
                            // No tenemos el ID del caso aún porque no se ha guardado, 
                            // lo actualizaremos después del save.
                            .usuario(usuario)
                            .motivo("Reingreso por caso postventa (" + tipoCaso.name() + ")")
                            .build();
                    movimientoRepository.save(mov);
                }
            }
        }

        // Agregar Historial inicial
        HistorialCaso historial = HistorialCaso.builder()
                .usuario(usuario)
                .estadoNuevo(CasoPostventa.EstadoCaso.abierto.name())
                .comentario("Apertura de caso")
                .build();
        caso.agregarHistorial(historial);

        // Guardar el caso (JPA hace el INSERT en caso_postventa y reparacion si aplica)
        CasoPostventa guardado = casoRepository.save(caso);
        
        // TODO (Fase 4): Hook para envío de correo (RF-PV02)
        log.info("Caso {} creado exitosamente (ID: {}).", tipoCaso.name(), guardado.getCasoId());

        return toResponse(guardado);
    }

    // ──────────────────────────────────────────────────────────
    // GESTIÓN DE ESTADOS
    // ──────────────────────────────────────────────────────────

    @Transactional
    public CasoPostventaResponse cambiarEstado(Long casoId, String nuevoEstadoStr, String comentario, Usuario usuario) {
        CasoPostventa caso = casoRepository.findById(casoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Caso Postventa", casoId));

        CasoPostventa.EstadoCaso nuevoEstado = CasoPostventa.EstadoCaso.valueOf(nuevoEstadoStr.toLowerCase());
        
        if (caso.getEstado() == nuevoEstado) {
            throw new ReglaNegocioException("El caso ya se encuentra en estado " + nuevoEstadoStr);
        }

        String estadoAnterior = caso.getEstado().name();
        caso.setEstado(nuevoEstado);
        
        if (nuevoEstado == CasoPostventa.EstadoCaso.cerrado) {
            caso.setFechaCierre(LocalDateTime.now());
        }

        HistorialCaso historial = HistorialCaso.builder()
                .usuario(usuario)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(nuevoEstado.name())
                .comentario(comentario)
                .build();
        caso.agregarHistorial(historial);

        return toResponse(casoRepository.save(caso));
    }

    // ──────────────────────────────────────────────────────────
    // CONSULTAS
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CasoPostventaResponse> buscarPorFactura(String numeroFactura, Pageable pageable) {
        return casoRepository.findByVentaNumeroFactura(numeroFactura, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CasoPostventaResponse> listarPorVendedor(Long usuarioId, Pageable pageable) {
        return casoRepository.findByUsuarioUsuarioId(usuarioId, pageable)
                .map(this::toResponse);
    }

    // ──────────────────────────────────────────────────────────
    // METODOS INTERNOS
    // ──────────────────────────────────────────────────────────

    /**
     * Valida que no se intente devolver más ítems de los que se compraron en la venta original.
     */
    private void validarCantidades(Venta venta, CasoPostventaRequest request) {
        for (CasoPostventaRequest.DetalleCasoRequest detReq : request.getDetalles()) {
            
            int cantComprada = venta.getDetalles().stream()
                    .filter(d -> d.getProducto().getProductoId().equals(detReq.getProductoId()))
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();
            
            if (cantComprada == 0) {
                throw new ReglaNegocioException("El producto ID " + detReq.getProductoId() + " no pertenece a la factura.");
            }
            if (detReq.getCantidad() > cantComprada) {
                throw new ReglaNegocioException("Intenta devolver " + detReq.getCantidad() + 
                    " unidades, pero solo compró " + cantComprada + " en la venta indicada.");
            }
        }
    }

    private CasoPostventaResponse toResponse(CasoPostventa c) {
        CasoPostventaResponse.CasoPostventaResponseBuilder b = CasoPostventaResponse.builder()
                .casoId(c.getCasoId())
                .numeroFactura(c.getVenta() != null ? c.getVenta().getNumeroFactura() : null)
                .nombreCliente(c.getCliente().getNombre())
                .tipo(c.getTipo().name())
                .estado(c.getEstado().name())
                .descripcion(c.getDescripcion())
                .respuestaAdmin(c.getRespuestaAdmin())
                .fechaApertura(c.getFechaApertura())
                .fechaCierre(c.getFechaCierre());

        if (c instanceof Reparacion) {
            Reparacion r = (Reparacion) c;
            b.descripcionTrabajo(r.getDescripcionTrabajo())
             .presupuesto(r.getPresupuesto())
             .costoReal(r.getCostoReal())
             .estadoReparacion(r.getEstadoReparacion().name())
             .fechaEntregaEstimada(r.getFechaEntregaEstimada());
        }

        b.detalles(c.getDetalles().stream().map(d -> 
            CasoPostventaResponse.DetalleCasoResponse.builder()
                .productoId(d.getProducto().getProductoId())
                .nombreProducto(d.getProducto().getNombre())
                .cantidad(d.getCantidad())
                .motivo(d.getMotivo())
                .estadoProducto(d.getEstadoProducto().name())
                .build()
        ).collect(Collectors.toList()));

        b.historial(c.getHistorial().stream().map(h ->
            CasoPostventaResponse.HistorialCasoResponse.builder()
                .usuario(h.getUsuario().getNombre())
                .estadoAnterior(h.getEstadoAnterior())
                .estadoNuevo(h.getEstadoNuevo())
                .comentario(h.getComentario())
                .fechaCambio(h.getFechaCambio())
                .build()
        ).collect(Collectors.toList()));

        return b.build();
    }
}
