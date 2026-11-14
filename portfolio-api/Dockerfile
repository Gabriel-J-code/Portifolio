# =====================================================================
# Imagem da portfolio-api (multi-stage).
#   Construir:  docker build -t portfolio-api .
#   Rodar:      ver a secao "Imagem Docker" do README.
#
# Estagio 1 (build): tem o JDK e o Maven, compila e gera o .jar.
# Estagio 2 (runtime): so o JRE + a aplicacao. O JDK, o Maven, o codigo-fonte
# e o cache de dependencias ficam para tras -> imagem menor e com menos superficie de ataque.
# =====================================================================

# ---------- Estagio 1: build ----------
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /build

# Primeiro so o que define as dependencias (wrapper + pom.xml).
# O Docker guarda cada passo em cache: enquanto o pom.xml nao mudar, o download
# das dependencias (passo mais lento) e reaproveitado e so o codigo e recompilado.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Agora o codigo. Os testes NAO rodam aqui: eles precisam do Docker (Testcontainers),
# que nao existe dentro do build. Eles rodam antes, na maquina ou no CI (GitHub Actions).
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests \
    && cp target/portfolio-api-*.jar application.jar

# Separa o .jar em camadas (dependencias de terceiros, loader do Spring, codigo da aplicacao).
# No estagio 2 cada uma vira uma camada da imagem: ao mudar so o codigo, apenas a
# camada "application" (poucos KB) muda; as dependencias (~dezenas de MB) ficam em cache.
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- Estagio 2: runtime ----------
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

# Nao rodar como root: se alguem explorar uma falha na API, nao ganha root no container
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

# Da camada que menos muda para a que mais muda (melhor aproveitamento do cache)
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER spring

# Perfil de producao por padrao (Swagger desligado, SMTP real, sem padroes de dev).
# Pode ser trocado na hora de rodar: docker run -e SPRING_PROFILES_ACTIVE=...
ENV SPRING_PROFILES_ACTIVE=prod
# A JVM reserva por padrao so 25% da memoria do container para o heap.
# Num container dedicado a API, 75% aproveita melhor a memoria (ex.: Cloud Run com 512 MiB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Documenta a porta (o Cloud Run informa a real pela variavel PORT; padrao 8080)
EXPOSE 8080

# Forma "exec" (JSON): o java vira o processo principal e recebe o SIGTERM do
# Docker/Cloud Run, permitindo desligar de forma limpa.
ENTRYPOINT ["java", "-jar", "application.jar"]
