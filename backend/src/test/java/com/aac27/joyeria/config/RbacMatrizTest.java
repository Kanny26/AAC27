package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol.NombreRol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.aac27.joyeria.module.seguridad.entity.Rol.NombreRol.administrador;
import static com.aac27.joyeria.module.seguridad.entity.Rol.NombreRol.superadministrador;
import static com.aac27.joyeria.module.seguridad.entity.Rol.NombreRol.vendedor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * T-08: matriz de autorización por rol (RBAC, RNF-SEG02).
 *
 * <p>Recorre una tabla "endpoint → roles permitidos" y comprueba, para cada endpoint:
 * <ul>
 *   <li>sin token → 401</li>
 *   <li>rol NO permitido → 403</li>
 *   <li>rol permitido → ni 401 ni 403 (el resultado exacto, 200/404/..., depende de los datos
 *       y no es lo que se prueba aquí)</li>
 * </ul>
 *
 * <p>Se usan endpoints SIN cuerpo a propósito: Spring valida el cuerpo ({@code @Valid}) antes de
 * ejecutar {@code @PreAuthorize}, así que un cuerpo inválido daría 400 y ocultaría el 403 que
 * queremos comprobar. (El alta de usuarios con vendedor ya se prueba en {@code AuthFlujoTest}.)
 *
 * <p>El superadministrador es un rol de soporte técnico: NO hereda los permisos del
 * administrador, por eso aparece como no permitido en todos los endpoints de negocio.
 *
 * <p>Los tokens se generan directamente con {@link JwtUtil}: el login ya se prueba en otras
 * clases y así se evita repetir BCrypt (cost 12) por cada fila de la matriz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacMatrizTest extends AbstractAuthTest {

    /** Una fila de la matriz: método, ruta y los roles que SÍ pueden llamarla. */
    private record Regla(HttpMethod metodo, String ruta, Set<NombreRol> permitidos) {
        @Override
        public String toString() {
            return metodo + " " + ruta;
        }
    }

    private static Regla regla(HttpMethod metodo, String ruta, NombreRol... permitidos) {
        Set<NombreRol> roles = permitidos.length == 0
                ? EnumSet.noneOf(NombreRol.class)
                : EnumSet.of(permitidos[0], permitidos);
        return new Regla(metodo, ruta, roles);
    }

    private static final List<Regla> MATRIZ = List.of(
            // ── Solo Administrador ────────────────────────────────────────────
            regla(HttpMethod.GET, "/api/v1/compras", administrador),
            regla(HttpMethod.GET, "/api/v1/promociones", administrador),
            regla(HttpMethod.PATCH, "/api/v1/promociones/999999/desactivar", administrador),
            regla(HttpMethod.PATCH, "/api/v1/categorias/999999/estado?nuevoEstado=inactivo", administrador),
            // RF25: gestionar casos de postventa es del Administrador (el vendedor solo consulta)
            regla(HttpMethod.PUT, "/api/v1/postventa/999999/estado?nuevoEstado=cerrado&comentario=prueba",
                    administrador),

            // ── Administrador y Vendedor ──────────────────────────────────────
            regla(HttpMethod.GET, "/api/v1/clientes", administrador, vendedor),
            regla(HttpMethod.GET, "/api/v1/ventas", administrador, vendedor),
            regla(HttpMethod.GET, "/api/v1/apartados", administrador, vendedor),
            regla(HttpMethod.GET, "/api/v1/promociones/vigentes", administrador, vendedor),
            regla(HttpMethod.GET, "/api/v1/postventa/mis-casos", administrador, vendedor),
            regla(HttpMethod.GET, "/api/v1/postventa/buscar?factura=NO-EXISTE", administrador, vendedor),

            // ── Cualquier usuario autenticado ─────────────────────────────────
            // Lectura de categorías abierta (la necesita el POS del vendedor)
            regla(HttpMethod.GET, "/api/v1/categorias", administrador, vendedor, superadministrador),
            regla(HttpMethod.GET, "/api/v1/auth/me", administrador, vendedor, superadministrador)
    );

    @Test
    void cadaEndpointRespetaLaMatrizDeRoles() throws Exception {
        Usuario admin = crearUsuario(administrador, Usuario.EstadoUsuario.activo);
        Usuario vend = crearUsuario(vendedor, Usuario.EstadoUsuario.activo);
        Usuario soporte = crearUsuario(superadministrador, Usuario.EstadoUsuario.activo);

        List<String> violaciones = new ArrayList<>();

        for (Regla regla : MATRIZ) {
            // Sin token siempre 401
            int sinToken = mockMvc.perform(request(regla.metodo(), regla.ruta()))
                    .andReturn().getResponse().getStatus();
            if (sinToken != 401) {
                violaciones.add(regla + " sin token: se esperaba 401 y fue " + sinToken);
            }

            comprobarRol(regla, administrador, admin, violaciones);
            comprobarRol(regla, vendedor, vend, violaciones);
            comprobarRol(regla, superadministrador, soporte, violaciones);
        }

        // Una sola aserción al final para listar TODAS las violaciones, no solo la primera
        assertThat(violaciones).as("Violaciones de la matriz RBAC").isEmpty();
    }

    private void comprobarRol(Regla regla, NombreRol rol, Usuario usuario, List<String> violaciones)
            throws Exception {
        String token = jwtUtil.generarAccessToken(usuario);
        int estado = mockMvc.perform(request(regla.metodo(), regla.ruta())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getStatus();

        if (regla.permitidos().contains(rol)) {
            if (estado == 401 || estado == 403) {
                violaciones.add(regla + " como " + rol + ": debía poder entrar y recibió " + estado);
            }
        } else if (estado != 403) {
            violaciones.add(regla + " como " + rol + ": se esperaba 403 y fue " + estado);
        }
    }
}
