package com.aac27.joyeria.shared.mail;

import com.aac27.joyeria.shared.exception.CorreoNoEnviadoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Envío de correos transaccionales del sistema (RF06).
 *
 * <p>Reglas:
 * <ul>
 *   <li>Configuración SMTP solo por variables de entorno (RNF-INF02).</li>
 *   <li>Nunca se registra en el log el contenido del correo (RNF-SEG03).</li>
 *   <li>Si {@code app.mail.enabled=true} y no hay servidor SMTP, la app no arranca.</li>
 * </ul>
 */
@Slf4j
@Service
public class CorreoService {

    private final JavaMailSender mailSender; // null si no hay spring.mail.host
    private final boolean habilitado;
    private final String remitente;

    public CorreoService(Optional<JavaMailSender> mailSender,
                         @Value("${app.mail.enabled:false}") boolean habilitado,
                         @Value("${app.mail.from:no-reply@aac27.local}") String remitente) {
        this.mailSender = mailSender.orElse(null);
        this.habilitado = habilitado;
        this.remitente = remitente;

        // Fail-fast: un envío "habilitado" sin servidor SMTP es una configuración rota
        if (habilitado && this.mailSender == null) {
            throw new IllegalStateException(
                    "app.mail.enabled=true pero no hay servidor SMTP: define MAIL_HOST (spring.mail.host)");
        }
    }

    /** Indica si el envío real está activo. */
    public boolean estaHabilitado() {
        return habilitado;
    }

    /**
     * Envía al usuario recién creado su contraseña provisional.
     *
     * @throws CorreoNoEnviadoException si el servidor SMTP rechaza o no responde
     */
    public void enviarPasswordProvisional(String destino, String nombre, String passwordProvisional) {
        if (!habilitado) {
            // No se escribe el contenido ni la clave, solo que se omitió el envío
            log.info("Envío de correo deshabilitado (app.mail.enabled=false); no se envió mensaje a {}", destino);
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject("AAC27 - Tu cuenta fue creada");
        mensaje.setText("""
                Hola %s,

                El administrador creó tu cuenta en el sistema AAC27.

                Usuario (correo): %s
                Contraseña provisional: %s

                Al iniciar sesión por primera vez el sistema te pedirá cambiarla por una
                contraseña propia. Hasta que la cambies no podrás usar el resto del sistema.

                Si no esperabas este mensaje, ignóralo y avisa al administrador.
                """.formatted(nombre, destino, passwordProvisional));

        try {
            mailSender.send(mensaje);
            log.info("Correo de cuenta creada enviado a {}", destino);
        } catch (MailException e) {
            // Se registra el motivo del servidor SMTP, nunca el cuerpo del mensaje
            log.error("No se pudo enviar el correo a {}: {}", destino, e.getMessage());
            throw new CorreoNoEnviadoException("Fallo el envío de correo a " + destino, e);
        }
    }
}