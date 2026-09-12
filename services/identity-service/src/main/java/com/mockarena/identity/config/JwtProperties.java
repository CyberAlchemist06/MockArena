package com.mockarena.identity.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="identity.jwt") public record JwtProperties(String issuer,String audience,String privateKeyPath,String publicKeyPath){}
