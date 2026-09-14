FROM eclipse-temurin:21.0.6_7-jdk-jammy
RUN groupadd --gid 10001 mockarena && useradd --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin mockarena && mkdir /workspace && chown mockarena:mockarena /workspace && chmod 1777 /workspace
USER 10001:10001
WORKDIR /workspace
