package com.aac27.joyeria.module.venta.controller;

import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.dto.AbonoApartadoRequest;
import com.aac27.joyeria.module.venta.dto.ApartadoResponse;
import com.aac27.joyeria.module.venta.entity.Apartado;
import com.aac27.joyeria.module.venta.service.ApartadoService;
import com.aac27.joyeria.shared.constant.RolConstants;
import com.aac27.joyeria.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del ciclo de vida de Apartados (Layaway).
 * 
 * <p>Complementa a VentaController (donde se inicia el apartado).
 * Referencia: RF-V01, Sección 3.19.
 */
@RestController
@RequestMapping("/api/v1/apartados")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
public class ApartadoController {

    private final ApartadoService apartadoService;

    /**
     * GET /api/v1/apartados — Lista apartados activos (Panel de control).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApartadoResponse>>> listarActivos(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                apartadoService.listarActivos(pageable), "Apartados activos obtenidos"));
    }

    /**
     * GET /api/v1/apartados/cliente/{id} — Apartados de un cliente específico.
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ApiResponse<Page<ApartadoResponse>>> listarPorCliente(
            @PathVariable Long clienteId,
            @RequestParam(defaultValue = "activo") String estado,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Apartado.EstadoApartado estadoEnum = Apartado.EstadoApartado.valueOf(estado.toLowerCase());
        return ResponseEntity.ok(ApiResponse.ok(
                apartadoService.listarPorCliente(clienteId, estadoEnum, pageable), 
                "Apartados del cliente obtenidos"));
    }

    /**
     * GET /api/v1/apartados/{id} — Detalle de un apartado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApartadoResponse>> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                apartadoService.obtenerApartado(id), "Detalle del apartado obtenido"));
    }

    /**
     * POST /api/v1/apartados/{id}/abonos — Registrar un nuevo abono.
     * Si el saldo llega a 0, se completa automáticamente.
     */
    @PostMapping("/{id}/abonos")
    public ResponseEntity<ApiResponse<ApartadoResponse>> registrarAbono(
            @PathVariable Long id,
            @Valid @RequestBody AbonoApartadoRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        
        return ResponseEntity.ok(ApiResponse.ok(
                apartadoService.registrarAbono(id, request, usuario), 
                "Abono registrado correctamente"));
    }

    /**
     * POST /api/v1/apartados/{id}/cancelar — Cancelar apartado y liberar stock.
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<ApartadoResponse>> cancelar(
            @PathVariable Long id,
            @RequestParam String motivo,
            @AuthenticationPrincipal Usuario usuario) {
        
        return ResponseEntity.ok(ApiResponse.ok(
                apartadoService.cancelarApartado(id, usuario, motivo), 
                "Apartado cancelado y stock liberado"));
    }
}
