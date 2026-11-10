package dev.gabriel.portfolio_api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe um SQL Server descartavel no Docker para os testes.
 *
 * - O container e um bean: o Spring o inicia antes da aplicacao e o para no fim.
 * - @ServiceConnection faz o Spring Boot apontar o datasource (url, usuario, senha)
 *   para esse container automaticamente, ignorando o banco de desenvolvimento.
 * - Mesma imagem do docker-compose: testamos contra o mesmo banco que usamos.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    MSSQLServerContainer sqlServerContainer() {
        return new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                .acceptLicense(); // a imagem da Microsoft exige aceitar a licenca (EULA)
    }
}
