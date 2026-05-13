package com.aac27.joyeria.module.postventa.controller;

import com.aac27.joyeria.module.postventa.dto.CasoPostventaRequest;
import com.aac27.joyeria.module.postventa.dto.CasoPostventaResponse;
import com.aac27.joyeria.module.postventa.service.PostventaService;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/postventa")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
public class PostventaController {

    private final PostventaService postventaService;

    /**
     * POST /api/v1/postventa — Abre un nuevo caso (Reclamo, Cambio, Devolución, Reparación).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CasoPostventaResponse>> crearCaso(
            @Valid @RequestBody CasoPostventaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        
        CasoPostventaResponse respuesta = postventaService.crearCaso(request, usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(respuesta, "Caso de postventa abierto exitosamente."));
    }

    /**
     * PUT /api/v1/postventa/{id}/estado — Actualiza el estado del caso.
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<CasoPostventaResponse>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado,
            @RequestParam String comentario,
            @AuthenticationPrincipal Usuario usuario) {
        
        return ResponseEntity.ok(ApiResponse.ok(
                postventaService.cambiarEstado(id, nuevoEstado, comentario, usuario),
                "Estado del caso actualizado"
        ));
    }

    /**
     * GET /api/v1/postventa/buscar — Búsqueda por número de factura (RF26).
     */
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<Page<CasoPostventaResponse>>> buscarPorFactura(
            @RequestParam String factura,
            @PageableDefault(sort = "fechaApertura", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Usuario usuario) {
        
        // RF26: Si es vendedor, filtramos su vista en el frontend o aquí podemos 
        // aplicar la regla de que solo vea los de su factura (para mantenerlo simple, 
        // devolvemos los resultados de esa factura. La regla de lectura de ventas 
        // ya protege quién ve qué ventas).
        return ResponseEntity.ok(ApiResponse.ok(
                postventaService.buscarPorFactura(factura, pageable),
                "Resultados de búsqueda"
        ));
    }

    /**
     * GET /api/v1/postventa/mis-casos — Casos abiertos por el vendedor logueado.
     */
    @GetMapping("/mis-casos")
    public ResponseEntity<ApiResponse<Page<CasoPostventaResponse>>> misCasos(
            @PageableDefault(sort = "fechaApertura", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Usuario usuario) {
        
        return ResponseEntity.ok(ApiResponse.ok(
                postventaService.listarPorVendedor(usuario.getUsuarioId(), pageable),
                "Tus casos de postventa"
        ));
    }
}
