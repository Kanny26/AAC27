package com.aac27.joyeria.module.venta.controller;

import com.aac27.joyeria.module.venta.dto.PromocionRequest;
import com.aac27.joyeria.module.venta.dto.PromocionResponse;
import com.aac27.joyeria.module.venta.service.PromocionService;
import com.aac27.joyeria.shared.constant.RolConstants;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el módulo de Descuentos y Promociones.
 *
 * <p>
 * Base URL: {@code /api/v1/promociones}
 * <br>
 * Referencia: RF-V02, Sección 3.28.
 *
 * <p>
 * <strong>Permisos:</strong>
 * <ul>
 * <li>Admin: CRUD completo</li>
 * <li>Vendedor: solo puede listar las promociones vigentes para aplicarlas</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/promociones")
@RequiredArgsConstructor
public class PromocionController {

        private final PromocionService promocionService;

        /**
         * GET /api/v1/promociones — Lista todas las promociones (paginadas).
         * Solo Administrador ve el listado completo.
         */
        @GetMapping
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<Page<PromocionResponse>>> listarTodas(
                        @PageableDefault(size = 20, sort = "fechaFin", direction = Sort.Direction.DESC) Pageable pageable) {

                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.listarTodas(pageable),
                                                "Promociones obtenidas correctamente"));
        }

        /**
         * GET /api/v1/promociones/vigentes — Lista las promociones activas y vigentes
         * hoy.
         * Disponible para Vendedor (para aplicarlas en ventas).
         */
        @GetMapping("/vigentes")
        @PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<List<PromocionResponse>>> listarVigentes() {
                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.listarVigentes(),
                                                "Promociones vigentes obtenidas"));
        }

        /**
         * GET /api/v1/promociones/producto/{productoId}?categoriaId={catId}
         * Lista promociones vigentes que aplican a un producto específico o su
         * categoría.
         * Usado al seleccionar un producto en el formulario de venta.
         */
        @GetMapping("/producto/{productoId}")
        @PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<List<PromocionResponse>>> vigentesParaProducto(
                        @PathVariable Long productoId,
                        @RequestParam Long categoriaId) {

                return ResponseEntity.ok(
                                ApiResponse.ok(
                                                promocionService.listarVigentesParaProducto(productoId, categoriaId),
                                                "Promociones aplicables al producto obtenidas"));
        }

        /**
         * GET /api/v1/promociones/{id} — Detalle de una promoción.
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<PromocionResponse>> obtenerPromocion(@PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.obtenerPromocion(id), "Promoción encontrada"));
        }

        /**
         * POST /api/v1/promociones — Crea una nueva promoción.
         * Solo Administrador.
         */
        @PostMapping
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<PromocionResponse>> crearPromocion(
                        @Valid @RequestBody PromocionRequest request) {

                PromocionResponse respuesta = promocionService.crearPromocion(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.ok(respuesta, "Promoción creada correctamente"));
        }

        /**
         * PUT /api/v1/promociones/{id} — Actualiza una promoción.
         * Solo Administrador.
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<PromocionResponse>> actualizarPromocion(
                        @PathVariable Long id,
                        @Valid @RequestBody PromocionRequest request) {

                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.actualizarPromocion(id, request),
                                                "Promoción actualizada correctamente"));
        }

        /**
         * PATCH /api/v1/promociones/{id}/activar — Activa una promoción desactivada.
         */
        @PatchMapping("/{id}/activar")
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<PromocionResponse>> activar(@PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.cambiarEstado(id, true),
                                                "Promoción activada"));
        }

        /**
         * PATCH /api/v1/promociones/{id}/desactivar — Desactiva una promoción sin
         * eliminarla.
         */
        @PatchMapping("/{id}/desactivar")
        @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
        public ResponseEntity<ApiResponse<PromocionResponse>> desactivar(@PathVariable Long id) {
                return ResponseEntity.ok(
                                ApiResponse.ok(promocionService.cambiarEstado(id, false),
                                                "Promoción desactivada"));
        }
}
