package dev.gabriel.portfolio_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import dev.gabriel.portfolio_api.config.AuthProperties;

/**
 * Teste UNITARIO: sem Spring, sem banco, sem container. Roda em milissegundos.
 * O JavaMailSender e um mock; o teste so confere a mensagem que o EmailService monta.
 */
class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AuthProperties props = new AuthProperties(
            "gabriel@exemplo.com", "senha", Duration.ofMinutes(10), "nao-responda@portfolio.local");
    private final EmailService emailService = new EmailService(mailSender, props);

    @Test
    void montaMensagemComCodigoEValidade() {
        emailService.enviarCodigoDeAcesso("gabriel@exemplo.com", "042137", Duration.ofMinutes(10));

        ArgumentCaptor<SimpleMailMessage> enviada = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(enviada.capture());
        SimpleMailMessage msg = enviada.getValue();

        assertThat(msg.getTo()).containsExactly("gabriel@exemplo.com");
        assertThat(msg.getFrom()).isEqualTo("nao-responda@portfolio.local");
        assertThat(msg.getSubject()).contains("042137");
        assertThat(msg.getText()).contains("042137").contains("10 minutos");
    }

    @Test
    void mascaraEmail() {
        assertThat(AcessoService.mascarar("gabriel@gmail.com")).isEqualTo("g******@gmail.com");
        assertThat(AcessoService.mascarar("a@gmail.com")).isEqualTo("*@gmail.com");
    }
}
