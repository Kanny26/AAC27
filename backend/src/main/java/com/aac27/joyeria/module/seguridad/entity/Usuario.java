package com.aac27.joyeria.module.seguridad.entity;

import com.aac27.joyeria.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa la tabla {@code usuario} de la base de datos.
 *
 * <p>Implementa {@link UserDetails} de Spring Security para integrarse
 * directamente con el sistema de autenticación. Esto permite que Spring
 * Security cargue un {@code Usuario} desde la BD y lo use en el contexto
 * de seguridad sin clases intermedias.
 *
 * <p>Hereda de {@link BaseAuditEntity} para los campos {@code created_at}
 * y {@code updated_at} que se gestionan automáticamente con JPA Auditing.
 *
 * <p>Referencia: Sección 3.1 del documento de arquitectura.
 * <br>RF02: "Contraseñas con BCrypt (cost≥12). Token JWT (exp: 8h). Refresh token (exp: 7d)."
 * <br>RF05: "Bloqueo de cuenta: usuarios Inactivos no se autentican."
 */
@Entity
@Table(
        name = "usuario",
        // Índices de rendimiento recomendados en Sección 4.4
        // Aunque las UQ ya crean índices, definirlos explícitamente
        // hace la intención clara para los desarrolladores.
        indexes = {
                @Index(name = "idx_usuario_correo", columnList = "correo"),
                @Index(name = "idx_usuario_estado", columnList = "estado")
        }
)
// Lombok: genera getters, setters, constructor vacío y con todos los campos
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseAuditEntity implements UserDetails {

    // ============================================================
    // CLAVE PRIMARIA
    // ============================================================

    /**
     * Identificador único autoincremental.
     * Equivale a BIGINT UNSIGNED AUTO_INCREMENT en MySQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    // ============================================================
    // DATOS PERSONALES
    // ============================================================

    /**
     * Nombre completo del usuario (empleado o administrador).
     * Equivale a VARCHAR(150) NOT NULL.
     */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /**
     * Número de documento de identidad (cédula/NIT).
     * ÚNICO e INMUTABLE tras la creación del usuario (RF08).
     * {@code updatable = false}: JPA lanzará error si se intenta modificar.
     */
    @Column(name = "numero_documento", nullable = false, unique = true,
            length = 20, updatable = false)
    private String numeroDocumento;

    /**
     * Correo electrónico del usuario.
     * ÚNICO e INMUTABLE tras la creación (RF08).
     * Se usa como nombre de usuario para el login (UserDetails.getUsername()).
     */
    @Column(name = "correo", nullable = false, unique = true,
            length = 255, updatable = false)
    private String correo;

    /**
     * Teléfono de contacto. Opcional.
     */
    @Column(name = "telefono", length = 20)
    private String telefono;

    // ============================================================
    // SEGURIDAD Y AUTENTICACIÓN
    // ============================================================

    /**
     * Contraseña cifrada con BCrypt (cost factor ≥ 12).
     * NUNCA se almacena en texto plano. La conversión a BCrypt se hace
     * en {@code AuthService} antes de guardar en BD.
     *
     * <p>RNF-SEG03: "Contraseñas almacenadas con BCrypt (cost factor ≥ 12).
     * Nunca almacenadas en texto plano ni en logs."
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Indica si la contraseña es provisional (generada por el sistema).
     * Si es {@code true}, el frontend debe forzar al usuario a cambiarla.
     *
     * <p>Valor inicial: {@code true} al crear el usuario (RF06).
     * <p>Equivale a TINYINT(1) DEFAULT 1 en MySQL.
     */
    @Column(name = "es_password_temporal", nullable = false)
    @Builder.Default
    private boolean esPasswordTemporal = true;

    /**
     * Estado de la cuenta del usuario.
     * Usuarios INACTIVOS no pueden autenticarse aunque la contraseña sea correcta (RF05).
     * Equivale a ENUM('activo','inactivo') en MySQL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.activo;

    /**
     * Fecha y hora del último inicio de sesión exitoso.
     * Se actualiza en cada login correcto (RF02).
     */
    @Column(name = "ultimo_acceso", columnDefinition = "DATETIME")
    private LocalDateTime ultimoAcceso;

    /**
     * URL de la foto de perfil del usuario.
     * Puede ser URL absoluta (S3) o relativa al servidor de archivos.
     * Tamaño máximo: 2 MB (RF-U01).
     */
    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    // ============================================================
    // RELACIÓN CON ROL
    // ============================================================

    /**
     * Rol asignado al usuario.
     *
     * <p>{@code @ManyToOne}: muchos usuarios pueden tener el mismo rol.
     * <p>{@code FetchType.LAZY}: el rol NO se carga automáticamente con cada usuario.
     *    Se carga solo cuando accedemos a {@code usuario.getRol()}.
     *    Esto mejora el rendimiento (Sección 3.29).
     * <p>{@code @JoinColumn}: especifica la columna FK en la tabla usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    // ============================================================
    // IMPLEMENTACIÓN DE UserDetails (Spring Security)
    // ============================================================
    // Spring Security necesita estos métodos para autenticar al usuario.
    // getAuthorities() → qué puede hacer el usuario
    // getPassword()    → contraseña cifrada para compararla con BCrypt
    // getUsername()    → identificador único (usamos el correo)
    // isEnabled()      → ¿puede el usuario iniciar sesión?
    // ============================================================

    /**
     * Retorna los permisos/roles del usuario para Spring Security.
     *
     * <p>Convierte el rol de la BD en un {@link SimpleGrantedAuthority}
     * con el formato "ROLE_nombre" que Spring Security espera.
     * Ejemplo: rol "administrador" → authority "ROLE_administrador"
     *
     * <p>Nota: Cargamos el rol manualmente aquí para evitar
     * {@code LazyInitializationException} fuera de transacción.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Construye el authority con el prefijo "ROLE_" requerido por Spring Security
        String autoridad = "ROLE_" + rol.getNombre().name();
        return List.of(new SimpleGrantedAuthority(autoridad));
    }

    /**
     * Retorna la contraseña cifrada (BCrypt hash).
     * Spring Security la usa para comparar contra la contraseña ingresada.
     */
    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    /**
     * Retorna el identificador único del usuario para Spring Security.
     * Usamos el correo porque es único e inmutable (RF08).
     */
    @Override
    public String getUsername() {
        return this.correo;
    }

    /**
     * Indica si la cuenta no ha expirado.
     * En nuestro sistema no manejamos expiración de cuenta, siempre es true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta no está bloqueada.
     * En nuestro sistema un usuario INACTIVO se trata como "bloqueado" (RF05).
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.estado == EstadoUsuario.activo;
    }

    /**
     * Indica si las credenciales (contraseña) no han expirado.
     * Siempre true; la política de contraseña temporal se maneja con esPasswordTemporal.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está habilitado para autenticarse.
     * Solo usuarios ACTIVOS pueden iniciar sesión (RF05).
     */
    @Override
    public boolean isEnabled() {
        return this.estado == EstadoUsuario.activo;
    }

    // ============================================================
    // ENUM DE ESTADO
    // ============================================================

    /**
     * Posibles estados de un usuario en el sistema.
     * Equivale al ENUM('activo','inactivo') de MySQL (Sección 3.1).
     */
    public enum EstadoUsuario {
        /** La cuenta está operativa y puede autenticarse. */
        activo,
        /** La cuenta está desactivada. No puede iniciar sesión (RF05). */
        inactivo
    }
}
