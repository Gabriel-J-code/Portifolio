package dev.gabriel.portfolio_api.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name = "acessos")
public class Acesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, unique = true)
    private String email;

    
    @Column(nullable = false)
    private String senha; // Armazenada com hash BCrypt

    // --- Campos extras para o 2FA via E-mail ---
    @Column(name = "two_factor_code")
    private String twoFactorCode;

    @Column(name = "two_factor_expires_at")
    private LocalDateTime twoFactorCodeExpiresAt;

    @Column(name = "enabled_2fa")
    private boolean enabled2Fa = true;

    public Acesso() {
    }

    public Acesso(int id, String email, String senha) {
        this.id = id;
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

    public LocalDateTime getTwoFactorCodeExpiresAt() {
        return twoFactorCodeExpiresAt;
    }

    public void setTwoFactorCodeExpiresAt(LocalDateTime twoFactorCodeExpiresAt) {
        this.twoFactorCodeExpiresAt = twoFactorCodeExpiresAt;
    }

    public boolean isEnabled2Fa() {
        return enabled2Fa;
    }

    public void setEnabled2Fa(boolean enabled2Fa) {
        this.enabled2Fa = enabled2Fa;
    }
}
