package com.douglas.horrorhub.alerta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class TelegramNotifier {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

    private final RestClient http;
    private final String token;

    public TelegramNotifier(RestClient.Builder builder, @Value("${telegram.base-url}") String baseUrl,
                            @Value("${telegram.token}") String token) {
        this.http = builder.baseUrl(baseUrl).build();
        this.token = token;
    }

    /** true se a mensagem foi entregue; sem token configurado só loga e conta como entregue. */
    public boolean enviar(String chatId, String texto) {
        if (token == null || token.isBlank()) {
            log.info("[telegram desligado] para {}: {}", chatId, texto);
            return true;
        }
        try {
            http.post().uri("/bot{token}/sendMessage", token)
                    .body(Map.of("chat_id", chatId, "text", texto))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            // a mensagem da exceção pode trazer a URL, que contém o token: loga só o tipo
            log.warn("falha ao enviar para o Telegram (chat {}): {}", chatId, e.getClass().getSimpleName());
            return false;
        }
    }
}
