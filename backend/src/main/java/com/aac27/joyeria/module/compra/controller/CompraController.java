package com.aac27.joyeria.module.compra.controller;

import com.aac27.joyeria.module.compra.dto.CompraRequest;
import com.aac27.joyeria.module.compra.dto.CompraResponse;
import com.aac27.joyeria.module.compra.service.CompraService;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.shared.constant.RolConstants;
import com.aac27.joyeria.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para el módulo de Compras.
 *
 * <p>Base URL: {@code /api/v1/compras}
 * <br>Solo el {@code administrador} puede registrar y consultar compras (RF11).
 *
 * <p>Referencia: RF11, RF-C01, Sección 4.1 y 4.3.
 */
@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
public class CompraController {

    private final CompraService compraService;

    /**
     * POST /api/v1/compras — Registra una nueva orden de compra.
     * Actualiza stock y crea crédito si aplica en la misma transacción.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CompraResponse>> registrarCompra(
            @Valid @RequestBody CompraRequest request,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        CompraResponse respuesta = compraService.registrarCompra(
                request, usuarioAutenticado.getUsuarioId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(respuesta, "Compra registrada exitosamente"));
    }

    /**
     * GET /api/v1/compras/{id} — Obtiene una compra con sus detalles y crédito.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompraResponse>> obtenerCompra(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(compraService.obtenerCompra(id), "Compra obtenida exitosamente"));
    }

    /**
     * GET /api/v1/compras — Lista paginada de compras con filtro opcional por proveedor.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompraResponse>>> listarCompras(
            @RequestParam(required = false) Long proveedorId,
            @PageableDefault(size = 20, sort = "compraId") Pageable pageable) {

        Page<CompraResponse> compras = compraService.listarCompras(proveedorId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(compras, "Compras obtenidas exitosamente"));
    }
}
