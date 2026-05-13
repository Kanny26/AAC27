package com.aac27.joyeria.module.venta.service;

import com.aac27.joyeria.module.venta.dto.ClienteRequest;
import com.aac27.joyeria.module.venta.dto.ClienteResponse;
import com.aac27.joyeria.module.venta.entity.Cliente;
import com.aac27.joyeria.module.venta.repository.ClienteRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de Clientes (CRM Básico).
 *
 * <p>Implementa RF-U02: registro, edición, consulta y búsqueda de clientes.
 * Los clientes no se eliminan físicamente si tienen ventas o apartados activos
 * (borrado lógico no implementado en esta tabla — pendiente Fase 4).
 *
 * <p>Referencia: RF-U02, RF-B01, Sección 3.3.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    // ──────────────────────────────────────────────────────────
    // CREAR CLIENTE
    // ──────────────────────────────────────────────────────────

    /**
     * Registra un nuevo cliente.
     * El número de documento y el correo deben ser únicos (si se informan).
     *
     * @param request Datos del nuevo cliente
     * @return DTO de respuesta con el cliente creado
     */
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        validarUnicidad(request.getNumeroDocumento(), request.getCorreo(), null);

        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .numeroDocumento(request.getNumeroDocumento())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .direccion(request.getDireccion())
                .fechaNacimiento(request.getFechaNacimiento())
                .notas(request.getNotas())
                .build();

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente creado: ID={}, nombre='{}'", guardado.getClienteId(), guardado.getNombre());
        return toResponse(guardado);
    }

    // ──────────────────────────────────────────────────────────
    // LISTAR Y BUSCAR
    // ──────────────────────────────────────────────────────────

    /**
     * Lista todos los clientes paginados.
     *
     * @param pageable Configuración de paginación y orden
     * @return Página de clientes
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarClientes(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Obtiene el detalle completo de un cliente por su ID.
     *
     * @param clienteId ID del cliente
     * @return DTO de respuesta con el cliente encontrado
     * @throws RecursoNoEncontradoException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));
        return toResponse(cliente);
    }

    /**
     * Busca clientes por nombre, número de documento o teléfono.
     * Búsqueda case-insensitive y parcial. (RF-B01)
     *
     * @param termino Texto a buscar
     * @param pageable Configuración de paginación
     * @return Página de clientes que coinciden
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> buscarClientes(String termino, Pageable pageable) {
        if (termino == null || termino.isBlank()) {
            return listarClientes(pageable);
        }
        return clienteRepository.buscar(termino.trim(), pageable).map(this::toResponse);
    }

    // ──────────────────────────────────────────────────────────
    // ACTUALIZAR CLIENTE
    // ──────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param clienteId ID del cliente a actualizar
     * @param request   Nuevos datos del cliente
     * @return DTO de respuesta con el cliente actualizado
     */
    @Transactional
    public ClienteResponse actualizarCliente(Long clienteId, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", clienteId));

        // Validar unicidad solo si los campos cambiaron
        boolean documentoCambio = !equals(cliente.getNumeroDocumento(), request.getNumeroDocumento());
        boolean correoCambio    = !equals(cliente.getCorreo(), request.getCorreo());

        if (documentoCambio || correoCambio) {
            String docAValidar = documentoCambio ? request.getNumeroDocumento() : null;
            String correoAValidar = correoCambio ? request.getCorreo() : null;
            validarUnicidad(docAValidar, correoAValidar, clienteId);
        }

        cliente.setNombre(request.getNombre());
        cliente.setNumeroDocumento(request.getNumeroDocumento());
        cliente.setTelefono(request.getTelefono());
        cliente.setCorreo(request.getCorreo());
        cliente.setDireccion(request.getDireccion());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setNotas(request.getNotas());

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente actualizado: ID={}", clienteId);
        return toResponse(actualizado);
    }

    // ──────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ──────────────────────────────────────────────────────────

    /**
     * Valida que documento y correo no estén duplicados.
     *
     * @param documento   Número de documento a verificar (puede ser null)
     * @param correo      Correo a verificar (puede ser null)
     * @param excludeId   ID del cliente a excluir (null si es creación)
     */
    private void validarUnicidad(String documento, String correo, Long excludeId) {
        if (documento != null && !documento.isBlank()) {
            boolean existe = excludeId == null
                    ? clienteRepository.existsByNumeroDocumento(documento)
                    : clienteRepository.existsByNumeroDocumentoAndClienteIdNot(documento, excludeId);
            if (existe) {
                throw new ConflictoDatosException(
                        "Ya existe un cliente con el número de documento: " + documento);
            }
        }
        if (correo != null && !correo.isBlank()) {
            boolean existe = excludeId == null
                    ? clienteRepository.existsByCorreo(correo)
                    : clienteRepository.existsByCorreoAndClienteIdNot(correo, excludeId);
            if (existe) {
                throw new ConflictoDatosException(
                        "Ya existe un cliente con el correo: " + correo);
            }
        }
    }

    /** Mapeo de entidad a DTO de respuesta. */
    private ClienteResponse toResponse(Cliente c) {
        int puntos = c.getPuntosFidelidad() != null ? c.getPuntosFidelidad() : 0;
        return ClienteResponse.builder()
                .clienteId(c.getClienteId())
                .nombre(c.getNombre())
                .numeroDocumento(c.getNumeroDocumento())
                .telefono(c.getTelefono())
                .correo(c.getCorreo())
                .direccion(c.getDireccion())
                .fechaNacimiento(c.getFechaNacimiento())
                .puntosFidelidad(puntos)
                .descuentoDisponibleCOP(puntos * 100) // 1 punto = 100 COP
                .notas(c.getNotas())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /** Comparación null-safe de strings. */
    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
