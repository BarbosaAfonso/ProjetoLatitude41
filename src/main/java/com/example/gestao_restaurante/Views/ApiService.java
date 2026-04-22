package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class ApiService {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(50))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ArrayNode getArray(String path) {
        JsonNode response = send("GET", path, null);
        if (response == null || response.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (!response.isArray()) {
            throw new RuntimeException("Resposta inesperada da API para " + path);
        }
        return (ArrayNode) response;
    }

    public JsonNode getObject(String path) {
        JsonNode response = send("GET", path, null);
        if (response == null || response.isNull()) {
            return objectMapper.createObjectNode();
        }
        return response;
    }

    public JsonNode post(String path, ObjectNode payload) {
        return send("POST", path, payload);
    }

    public Optional<JsonNode> login(String email, String password) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("email", email);
        payload.put("password", password);

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = sendRaw(request);
        int status = response.statusCode();

        if (status == 200) {
            try {
                return Optional.ofNullable(objectMapper.readTree(response.body()));
            } catch (IOException e) {
                throw new RuntimeException("Erro ao ler resposta de login da API", e);
            }
        }

        if (status == 401) {
            return Optional.empty();
        }

        throw new RuntimeException("Erro " + status + " no login: " + response.body());
    }

    public JsonNode put(String path, ObjectNode payload) {
        return send("PUT", path, payload);
    }

    public boolean delete(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .DELETE()
                .build();

        HttpResponse<String> response = sendRaw(request);
        int status = response.statusCode();

        if (status == 200 || status == 204) {
            return true;
        }
        if (status == 404) {
            return false;
        }

        throw new RuntimeException(formatError(status, "DELETE", path, response.body()));
    }

    public ObjectNode createObject() {
        return objectMapper.createObjectNode();
    }

    private JsonNode send(String method, String path, ObjectNode payload) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json");

        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else if ("POST".equalsIgnoreCase(method)) {
            builder.POST(HttpRequest.BodyPublishers.ofString(payload == null ? "{}" : payload.toString()));
        } else if ("PUT".equalsIgnoreCase(method)) {
            builder.PUT(HttpRequest.BodyPublishers.ofString(payload == null ? "{}" : payload.toString()));
        } else {
            throw new IllegalArgumentException("Metodo HTTP nao suportado: " + method);
        }

        HttpResponse<String> response = sendRaw(builder.build());
        int status = response.statusCode();

        if (status == 200 || status == 201) {
            String body = response.body();
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }
            try {
                return objectMapper.readTree(body);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao ler resposta JSON da API", e);
            }
        }

        if (status == 204) {
            return objectMapper.nullNode();
        }

        if (status == 404) {
            throw new RuntimeException("Recurso nao encontrado (404) em " + path);
        }

        throw new RuntimeException(formatError(status, method, path, response.body()));
    }

    private HttpResponse<String> sendRaw(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel comunicar com a API em " + BASE_URL, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Pedido HTTP interrompido", e);
        }
    }

    private String formatError(int status, String method, String path, String body) {
        String detalhe = extrairMensagemErro(body);
        return detalhe.isBlank()
                ? "Erro " + status + " em " + method + " " + path + "."
                : "Erro " + status + " em " + method + " " + path + ": " + detalhe;
    }

    private String extrairMensagemErro(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            String message = node.path("message").asText("").trim();
            if (!message.isBlank()) {
                return message;
            }

            String error = node.path("error").asText("").trim();
            if (!error.isBlank()) {
                return error;
            }
        } catch (IOException ignored) {
        }

        return body.trim();
    }
}
