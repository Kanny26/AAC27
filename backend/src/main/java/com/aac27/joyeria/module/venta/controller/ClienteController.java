package com.aac27.joyeria.module.venta.controller;

import com.aac27.joyeria.module.venta.dto.ClienteRequest;
import com.aac27.joyeria.module.venta.dto.ClienteResponse;
import com.aac27.joyeria.module.venta.service.ClienteService;
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

/**
 * Controlador REST para el módulo de Clientes (CRM Básico).
 *
 * <p>
 * Base URL: {@code /api/v1/clientes}
 * <br>
 * Referencia: RF-U02, RF-B01.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('" + RolConstants.VENDEDOR + "', '" + RolConstants.ADMINISTRADOR + "')")
public class ClienteController {

    private final ClienteService clienteService;

    /**
     * GET /api/v1/clientes — Lista todos los clientes paginados.
     * Si se incluye el param 'q', realiza búsqueda por nombre/doc/teléfono
     * (RF-B01).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ClienteResponse>>> listarClientes(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<ClienteResponse> resultado = (q != null && !q.isBlank())
                ? clienteService.buscarClientes(q, pageable)
                : clienteService.listarClientes(pageable);

        return ResponseEntity.ok(ApiResponse.ok(resultado, "Clientes obtenidos correctamente"));
    }

    /**
     * GET /api/v1/clientes/{id} — Obtiene el detalle de un cliente.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(clienteService.obtenerCliente(id), "Cliente encontrado"));
    }

    /**
     * POST /api/v1/clientes — Crea un nuevo cliente.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> crearCliente(
            @Valid @RequestBody ClienteRequest request) {

        ClienteResponse respuesta = clienteService.crearCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(respuesta, "Cliente registrado correctamente"));
    }

    /**
     * PUT /api/v1/clientes/{id} — Actualiza los datos de un cliente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {

        ClienteResponse respuesta = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(ApiResponse.ok(respuesta, "Cliente actualizado correctamente"));
    }
}
