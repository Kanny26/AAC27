package com.aac27.joyeria.shared.mail;

import com.aac27.joyeria.shared.exception.CorreoNoEnviadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CorreoServiceTest {

    @Mock
    private JavaMailSender sender;

    @Test
    void habilitadoEnviaElCorreoConLosDatosDelUsuario() {
        CorreoService servicio = new CorreoService(Optional.of(sender), true, "no-reply@aac27.test");

        servicio.enviarPasswordProvisional("maria@aac27.com", "Maria Garcia", "abc123def456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage enviado = captor.getValue();
        assertThat(enviado.getTo()).containsExactly("maria@aac27.com");
        assertThat(enviado.getFrom()).isEqualTo("no-reply@aac27.test");
        assertThat(enviado.getText())
                .contains("Maria Garcia")
                .contains("abc123def456")
                .contains("cambiarla");
    }

    @Test
    void deshabilitadoNoEnviaNada() {
        CorreoService servicio = new CorreoService(Optional.of(sender), false, "no-reply@aac27.test");

        servicio.enviarPasswordProvisional("maria@aac27.com", "Maria", "abc123def456");

        verifyNoInteractions(sender);
    }

    @Test
    void siElServidorFallaLanzaCorreoNoEnviado() {
        CorreoService servicio = new CorreoService(Optional.of(sender), true, "no-reply@aac27.test");
        doThrow(new MailSendException("smtp caido")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() ->
                servicio.enviarPasswordProvisional("maria@aac27.com", "Maria", "abc123def456"))
                .isInstanceOf(CorreoNoEnviadoException.class);
    }

    @Test
    void habilitadoSinServidorSmtpImpideArrancar() {
        assertThatThrownBy(() -> new CorreoService(Optional.empty(), true, "no-reply@aac27.test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_HOST");
    }
}