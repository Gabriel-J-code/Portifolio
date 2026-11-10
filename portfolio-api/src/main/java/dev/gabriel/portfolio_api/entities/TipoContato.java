package dev.gabriel.portfolio_api.entities;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Meios de contato exibidos no site (sem WhatsApp: numero pessoal).
 *
 * Cada tipo sabe validar o proprio valor: EMAIL espera um endereco de e-mail,
 * os demais esperam um link http(s). Assim a regra fica junto do dado que ela descreve,
 * e um tipo novo ja nasce obrigado a dizer o que aceita.
 */
public enum TipoContato {

    LINKEDIN(Formato.URL),
    GITHUB(Formato.URL),
    EMAIL(Formato.EMAIL),
    CURRICULO(Formato.URL); // link do PDF no RxResu.me

    public enum Formato { URL, EMAIL }

    // Simples de proposito: algo@dominio.ext, sem espacos. Validar e-mail "perfeitamente" por regex
    // e inviavel; a prova real seria enviar um e-mail, e aqui quem cadastra e o proprio dono do site.
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Formato formato;

    TipoContato(Formato formato) {
        this.formato = formato;
    }

    public Formato getFormato() {
        return formato;
    }

    public boolean aceita(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        return switch (formato) {
            case EMAIL -> EMAIL_REGEX.matcher(valor).matches();
            case URL -> ehUrlHttp(valor);
        };
    }

    private static boolean ehUrlHttp(String valor) {
        try {
            URI uri = new URI(valor);
            String esquema = uri.getScheme();
            return ("http".equalsIgnoreCase(esquema) || "https".equalsIgnoreCase(esquema))
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
