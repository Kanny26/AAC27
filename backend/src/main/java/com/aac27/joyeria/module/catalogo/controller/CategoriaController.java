package com.aac27.joyeria.module.catalogo.controller;

import com.aac27.joyeria.module.catalogo.dto.CategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.CategoriaResponse;
import com.aac27.joyeria.module.catalogo.service.CategoriaService;
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
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para categorías.
 * Referencia: RF12.
 */
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoriaResponse>>> listar(
            @PageableDefault(size = 10, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.listar(pageable), "Categorías obtenidas"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.obtenerPorId(id), "Categoría obtenida"));
    }

    @PostMapping
    @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoriaService.crear(request), "Categoría creada exitosamente"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.actualizar(id, request), "Categoría actualizada"));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
    public ResponseEntity<ApiResponse<Void>> cambiarEstado(
            @PathVariable Long id, @RequestParam String nuevoEstado) {
        categoriaService.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(ApiResponse.ok(null, "Estado de categoría actualizado"));
    }
}
