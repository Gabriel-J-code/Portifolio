package dev.gabriel.portfolio_api.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Credenciais do painel administrativo (uma unica linha: a conta do Gabriel).
 * O 2FA e sempre obrigatorio: todo login exige senha + codigo enviado por e-mail.
 */
@Entity
@Table(name = "acessos")
public class Acesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, unique = true)
    private String email; // guardado em minusculas

    @Column(nullable = false)
    private String senha; // hash BCrypt, nunca o texto

    // --- 2FA via e-mail ---
    // Hash BCrypt do codigo de 6 digitos (o codigo em si so existe no e-mail).
    // Se o banco vazar, um codigo ainda valido nao pode ser lido.
    @Column(name = "two_factor_code")
    private String twoFactorCode;

    // Instant = momento absoluto (UTC), sem ambiguidade de fuso horario
    @Column(name = "two_factor_expires_at")
    private Instant twoFactorCodeExpiresAt;

    public Acesso() {
    }

    public Acesso(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }

    // getters and setters for all fields
    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getTwoFactorCode() {
        return twoFactorCode;
    }

    public void setTwoFactorCode(String twoFactorCode) {
        this.twoFactorCode = twoFactorCode;
    }

    public Instant getTwoFactorCodeExpiresAt() {
        return twoFactorCodeExpiresAt;
    }

    public void setTwoFactorCodeExpiresAt(Instant twoFactorCodeExpiresAt) {
        this.twoFactorCodeExpiresAt = twoFactorCodeExpiresAt;
    }
}
