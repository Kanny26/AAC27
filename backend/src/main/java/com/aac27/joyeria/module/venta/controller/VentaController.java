package com.aac27.joyeria.module.venta.controller;

import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.dto.VentaRequest;
import com.aac27.joyeria.module.venta.dto.VentaResponse;
import com.aac27.joyeria.module.venta.entity.Venta;
import com.aac27.joyeria.module.venta.repository.VentaRepository;
import com.aac27.joyeria.module.venta.service.VentaService;
import com.aac27.joyeria.shared.constant.RolConstants;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para el módulo de Ventas.
 *
 * <p>
 * Base URL: {@code /api/v1/ventas}
 * <br>
 * Roles permitidos: VENDEDOR y ADMINISTRADOR pueden registrar ventas (RF18).
 *
 * <p>
 * <strong>Endpoints implementados:</strong>
 * <ul>
 * <li>GET /api/v1/ventas — RF17 Listar ventas (Admin: todas; Vendedor:
 * propias)</li>
 * <li>GET /api/v1/ventas/{id} — RF18 Detalle de venta</li>
 * <li>GET /api/v1/ventas/cliente/{cid} — Ventas de un cliente específico</li>
 * <li>POST /api/v1/ventas — RF20 Registrar venta</li>
 * <li>POST /api/v1/ventas/apartados — RF-V01 Registrar apartado (layaway)</li>
 * </ul>
 *
 * <p>
 * Referencia: RF17-RF21, RF-V01 a RF-V04.
 */
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
public class VentaController {

        private final VentaService ventaService;
        private final VentaRepository ventaRepository;

        // ──────────────────────────────────────────────────────────
        // GET — LISTAR VENTAS (RF17)
        // ──────────────────────────────────────────────────────────

        /**
         * GET /api/v1/ventas — Lista ventas paginadas.
         *
         * <p>
         * RF17: Admin ve TODAS las ventas; Vendedor solo ve las suyas.
         * La lógica de filtro por rol está en el Service.
         *
         * @param q                  Búsqueda opcional por número de factura (parcial)
         * @param pageable           Paginación y orden
         * @param usuarioAutenticado Usuario autenticado (determina el alcance)
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<VentaResponse>>> listarVentas(
                        @RequestParam(required = false) String q,
                        @PageableDefault(size = 20, sort = "fechaVenta", direction = Sort.Direction.DESC) Pageable pageable,
                        @AuthenticationPrincipal Usuario usuarioAutenticado) {

                Page<Venta> ventasPagina;
                boolean esAdmin = usuarioAutenticado.getRol().getNombre().name()
                                .equalsIgnoreCase(RolConstants.ADMINISTRADOR);

                if (esAdmin) {
                        ventasPagina = ventaRepository.findAll(pageable);
                } else {
                        // Vendedor solo ve las suyas (RF17)
                        ventasPagina = ventaRepository.findByUsuarioUsuarioIdOrderByFechaVentaDesc(
                                        usuarioAutenticado.getUsuarioId(), pageable);
                }

                Page<VentaResponse> respuesta = ventasPagina.map(this::toVentaResponse);
                return ResponseEntity.ok(ApiResponse.ok(respuesta, "Ventas obtenidas correctamente"));
        }

        // ──────────────────────────────────────────────────────────
        // GET — DETALLE DE VENTA (RF18)
        // ──────────────────────────────────────────────────────────

        /**
         * GET /api/v1/ventas/{id} — Devuelve el detalle completo de una venta.
         *
         * <p>
         * RF18: Admin puede ver cualquier venta; Vendedor solo las suyas.
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<VentaResponse>> obtenerVenta(
                        @PathVariable Long id,
                        @AuthenticationPrincipal Usuario usuarioAutenticado) {

                Venta venta = ventaRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Venta", id));

                // RF18: Vendedor solo puede ver sus propias ventas
                boolean esAdmin = usuarioAutenticado.getRol().getNombre().name()
                                .equalsIgnoreCase(RolConstants.ADMINISTRADOR);
                if (!esAdmin && !venta.getUsuario().getUsuarioId()
                                .equals(usuarioAutenticado.getUsuarioId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.error("No tiene permiso para ver esta venta"));
                }

                return ResponseEntity.ok(ApiResponse.ok(toVentaResponse(venta), "Venta encontrada"));
        }

        // ──────────────────────────────────────────────────────────
        // GET — VENTAS DE UN CLIENTE
        // ──────────────────────────────────────────────────────────

        /**
         * GET /api/v1/ventas/cliente/{clienteId} — Historial de ventas de un cliente.
         * Útil para el perfil del cliente en el formulario de nueva venta.
         */
        @GetMapping("/cliente/{clienteId}")
        public ResponseEntity<ApiResponse<Page<VentaResponse>>> ventasPorCliente(
                        @PathVariable Long clienteId,
                        @PageableDefault(size = 10, sort = "fechaVenta", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<VentaResponse> respuesta = ventaRepository
                                .findByClienteClienteIdOrderByFechaVentaDesc(clienteId, pageable)
                                .map(this::toVentaResponse);

                return ResponseEntity.ok(
                                ApiResponse.ok(respuesta, "Historial de compras del cliente obtenido"));
        }

        // ──────────────────────────────────────────────────────────
        // POST — REGISTRAR VENTA (RF20)
        // ──────────────────────────────────────────────────────────

        /**
         * POST /api/v1/ventas — Registra una venta directa (contado, crédito,
         * anticipo).
         *
         * <p>
         * El precio de venta lo determina el SERVIDOR (producto.precioVenta),
         * no el cliente HTTP — esto previene manipulación de precios.
         */
        @PostMapping
        public ResponseEntity<ApiResponse<VentaResponse>> registrarVenta(
                        @Valid @RequestBody VentaRequest request,
                        @AuthenticationPrincipal Usuario usuarioAutenticado) {

                VentaResponse respuesta = ventaService.registrarVenta(request, usuarioAutenticado);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(respuesta, "Venta registrada exitosamente. Factura: "
                                                + respuesta.getNumeroFactura()));
        }

        // ──────────────────────────────────────────────────────────
        // POST — REGISTRAR APARTADO (RF-V01)
        // ──────────────────────────────────────────────────────────

        /**
         * POST /api/v1/ventas/apartados — Registra un apartado (layaway).
         *
         * <p>
         * Reserva el stock sin descontarlo. Requiere primer abono ≥ 20% del total.
         * El ciclo de vida completo (abonos, completar, cancelar) se gestiona
         * desde {@code /api/v1/apartados}.
         */
        @PostMapping("/apartados")
        public ResponseEntity<ApiResponse<VentaResponse>> registrarApartado(
                        @Valid @RequestBody VentaRequest request,
                        @AuthenticationPrincipal Usuario usuarioAutenticado) {

                VentaResponse respuesta = ventaService.registrarApartado(request, usuarioAutenticado);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(respuesta,
                                                "Apartado registrado exitosamente: " + respuesta.getNumeroFactura()));
        }

        // ──────────────────────────────────────────────────────────
        // MAPEO INTERNO
        // ──────────────────────────────────────────────────────────

        /**
         * Convierte una entidad Venta al DTO de respuesta.
         * Proyección simplificada — el cliente puede usar GET /{id} para el detalle
         * completo.
         */
        private VentaResponse toVentaResponse(Venta venta) {
                return VentaResponse.builder()
                                .ventaId(venta.getVentaId())
                                .numeroFactura(venta.getNumeroFactura())
                                .subtotal(venta.getSubtotal())
                                .descuentoTotal(venta.getDescuentoTotal())
                                .descuentoPuntos(venta.getDescuentoPuntos())
                                .total(venta.getTotal())
                                .puntosFidelidadUsados(venta.getPuntosFidelidadUsados())
                                .puntosGanados(venta.getPuntosGanados())
                                .estado(venta.getEstado().name())
                                .build();
        }
}
