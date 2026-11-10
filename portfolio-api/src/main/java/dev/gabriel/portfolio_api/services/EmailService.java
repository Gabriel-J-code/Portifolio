package dev.gabriel.portfolio_api.services;

import java.time.Duration;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import dev.gabriel.portfolio_api.config.AuthProperties;

/**
 * Envio de e-mails. Hoje so o codigo do 2FA.
 *
 * O JavaMailSender e configurado pelo Spring Boot a partir de spring.mail.* (host, porta...).
 * Este service so monta a mensagem; trocar Mailpit por um SMTP real e so mudar as propriedades.
 *
 * O envio e sincrono: o login espera o e-mail sair. Mais adiante, com RabbitMQ, ele pode virar
 * um evento processado em segundo plano.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final AuthProperties authProperties;

    public EmailService(JavaMailSender mailSender, AuthProperties authProperties) {
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    public void enviarCodigoDeAcesso(String destino, String codigo, Duration validade) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(authProperties.remetente());
        mensagem.setTo(destino);
        mensagem.setSubject("Seu codigo de acesso ao painel: " + codigo);
        mensagem.setText("""
                Ola!

                Seu codigo de acesso ao painel do portfolio e:

                    %s

                Ele vale por %d minutos e so pode ser usado uma vez.
                Se nao foi voce que tentou entrar, troque sua senha: alguem a conhece.
                """.formatted(codigo, validade.toMinutes()));
        mailSender.send(mensagem);
    }
}
