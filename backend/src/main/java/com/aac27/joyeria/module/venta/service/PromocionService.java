package com.aac27.joyeria.module.venta.service;

import com.aac27.joyeria.module.venta.dto.PromocionRequest;
import com.aac27.joyeria.module.venta.dto.PromocionResponse;
import com.aac27.joyeria.module.venta.entity.Promocion;
import com.aac27.joyeria.module.venta.repository.PromocionRepository;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de Promociones y Descuentos.
 *
 * <p>
 * <strong>Reglas de negocio (RF-V02):</strong>
 * <ul>
 * <li>El descuento NO puede resultar en precio negativo</li>
 * <li>Las promociones tienen fechaInicio y fechaFin de vigencia</li>
 * <li>Solo el Administrador puede crear/editar promociones</li>
 * <li>Si tipo = 'porcentaje', valor debe ser <= 100</li>
 * </ul>
 *
 * <p>
 * Referencia: RF-V02, Sección 3.28.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromocionService {

    private final PromocionRepository promocionRepository;

    // ──────────────────────────────────────────────────────────
    // CREAR PROMOCIÓN
    // ──────────────────────────────────────────────────────────

    /**
     * Crea una nueva promoción.
     *
     * @param request Datos de la promoción
     * @return DTO de la promoción creada
     */
    @Transactional
    public PromocionResponse crearPromocion(PromocionRequest request) {
        validarRequest(request, null);

        Promocion promocion = Promocion.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .tipo(Promocion.TipoDescuento.valueOf(request.getTipo()))
                .valor(request.getValor())
                .aplicaA(Promocion.AplicaA.valueOf(request.getAplicaA()))
                .entidadId(request.getEntidadId())
                .fechaInicio(request.getFechaInicio())
                .fechaFin(request.getFechaFin())
                .activa(request.isActiva())
                .build();

        Promocion guardada = promocionRepository.save(promocion);
        log.info("Promoción creada: ID={}, nombre='{}'", guardada.getPromocionId(), guardada.getNombre());
        return toResponse(guardada);
    }

    // ──────────────────────────────────────────────────────────
    // LISTAR PROMOCIONES
    // ──────────────────────────────────────────────────────────

    /**
     * Lista todas las promociones paginadas (panel de administración).
     */
    @Transactional(readOnly = true)
    public Page<PromocionResponse> listarTodas(Pageable pageable) {
        return promocionRepository.findAllByOrderByFechaFinDesc(pageable).map(this::toResponse);
    }

    /**
     * Lista solo las promociones activas con vigencia actual.
     * Usado por el formulario de venta para que el vendedor pueda aplicarlas.
     */
    @Transactional(readOnly = true)
    public List<PromocionResponse> listarVigentes() {
        return promocionRepository.findVigentes(LocalDate.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lista promociones vigentes aplicables a un producto o su categoría.
     * Usado al agregar un ítem al formulario de venta.
     *
     * @param productoId  ID del producto
     * @param categoriaId ID de la categoría del producto
     */
    @Transactional(readOnly = true)
    public List<PromocionResponse> listarVigentesParaProducto(Long productoId, Long categoriaId) {
        return promocionRepository
                .findVigentesParaProducto(productoId, categoriaId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────
    // OBTENER POR ID
    // ──────────────────────────────────────────────────────────

    /**
     * Obtiene el detalle de una promoción por ID.
     */
    @Transactional(readOnly = true)
    public PromocionResponse obtenerPromocion(Long promocionId) {
        return toResponse(buscarPromocion(promocionId));
    }

    // ──────────────────────────────────────────────────────────
    // ACTUALIZAR PROMOCIÓN
    // ──────────────────────────────────────────────────────────

    /**
     * Actualiza una promoción existente.
     *
     * <p>
     * NOTA: No se puede editar una promoción que ya tiene ventas aplicadas.
     * Para eso se debe desactivar y crear una nueva.
     *
     * @param promocionId ID de la promoción a actualizar
     * @param request     Nuevos datos
     * @return DTO actualizado
     */
    @Transactional
    public PromocionResponse actualizarPromocion(Long promocionId, PromocionRequest request) {
        Promocion promocion = buscarPromocion(promocionId);
        validarRequest(request, promocionId);

        promocion.setNombre(request.getNombre());
        promocion.setDescripcion(request.getDescripcion());
        promocion.setTipo(Promocion.TipoDescuento.valueOf(request.getTipo()));
        promocion.setValor(request.getValor());
        promocion.setAplicaA(Promocion.AplicaA.valueOf(request.getAplicaA()));
        promocion.setEntidadId(request.getEntidadId());
        promocion.setFechaInicio(request.getFechaInicio());
        promocion.setFechaFin(request.getFechaFin());
        promocion.setActiva(request.isActiva());

        Promocion actualizada = promocionRepository.save(promocion);
        log.info("Promoción actualizada: ID={}", promocionId);
        return toResponse(actualizada);
    }

    // ──────────────────────────────────────────────────────────
    // ACTIVAR / DESACTIVAR
    // ──────────────────────────────────────────────────────────

    /**
     * Activa o desactiva una promoción sin eliminarla.
     *
     * @param promocionId ID de la promoción
     * @param activa      true para activar, false para desactivar
     * @return DTO actualizado
     */
    @Transactional
    public PromocionResponse cambiarEstado(Long promocionId, boolean activa) {
        Promocion promocion = buscarPromocion(promocionId);
        promocion.setActiva(activa);
        Promocion guardada = promocionRepository.save(promocion);
        log.info("Promoción #{} {} manualmente.", promocionId, activa ? "activada" : "desactivada");
        return toResponse(guardada);
    }

    // ──────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS
    // ──────────────────────────────────────────────────────────

    /**
     * Valida las reglas de negocio de la petición.
     */
    private void validarRequest(PromocionRequest request, Long excludeId) {
        // Fechas coherentes
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new ReglaNegocioException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio.");
        }
        // Porcentaje no puede ser > 100
        if ("porcentaje".equals(request.getTipo())) {
            if (request.getValor().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new ReglaNegocioException(
                        "El porcentaje de descuento no puede ser mayor a 100%.");
            }
        }
        // Si aplica a categoría o producto, el ID es obligatorio
        if (!"venta".equals(request.getAplicaA()) && request.getEntidadId() == null) {
            throw new ReglaNegocioException(
                    "Se requiere el ID de la entidad cuando 'aplica_a' es '"
                            + request.getAplicaA() + "'.");
        }
    }

    /** Busca una promoción por ID o lanza 404. */
    private Promocion buscarPromocion(Long promocionId) {
        return promocionRepository.findById(promocionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Promoción", promocionId));
    }

    /** Mapeo de entidad a DTO de respuesta. */
    private PromocionResponse toResponse(Promocion p) {
        return PromocionResponse.builder()
                .promocionId(p.getPromocionId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .tipo(p.getTipo().name())
                .valor(p.getValor())
                .aplicaA(p.getAplicaA().name())
                .entidadId(p.getEntidadId())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .activa(p.isActiva())
                .vigente(p.estaVigente(LocalDate.now()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
