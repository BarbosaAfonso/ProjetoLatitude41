package com.example.gestao_restaurante.Views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

public class ApiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ApiService() {
        this(resolveConfiguredBaseUrl());
    }

    public ApiService(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public ObjectNode createObject() {
        return objectMapper.createObjectNode();
    }

    public JsonNode getObject(String path) {
        return sendForJson("GET", path, null);
    }

    public ArrayNode getArray(String path) {
        JsonNode json = getObject(path);
        if (json instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        if (json == null || json.isNull()) {
            return objectMapper.createArrayNode();
        }
        throw new RuntimeException("Resposta inesperada da API para " + path + ".");
    }

    public JsonNode post(String path, JsonNode body) {
        return sendForJson("POST", path, body);
    }

    public JsonNode put(String path, JsonNode body) {
        return sendForJson("PUT", path, body);
    }

    public boolean delete(String path) {
        HttpResponse<String> response = send("DELETE", path, null);
        if (response.statusCode() == 404) {
            return false;
        }
        ensureSuccess(response, path);
        return true;
    }

    private JsonNode sendForJson(String method, String path, JsonNode body) {
        HttpResponse<String> response = send(method, path, body);
        ensureSuccess(response, path);

        String responseBody = response.body();
        if (responseBody == null || responseBody.isBlank()) {
            return NullNode.getInstance();
        }

        try {
            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            throw new RuntimeException("Resposta invalida da API em " + path + ": " + e.getMessage(), e);
        }
    }

    private HttpResponse<String> send(String method, String path, JsonNode body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        }

        try {
            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel contactar a API em " + baseUrl + ": " + exceptionDetail(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Pedido a API interrompido.", e);
        }
    }

    private URI resolve(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Caminho da API invalido.");
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(baseUrl + normalizedPath);
    }

    private void ensureSuccess(HttpResponse<String> response, String path) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        String body = response.body();
        String detail = body == null || body.isBlank() ? "" : " Resposta: " + truncate(body.strip(), 300);
        throw new RuntimeException("Erro da API em " + path + " (HTTP " + status + ")." + detail);
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String exceptionDetail(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String resolveConfiguredBaseUrl() {
        String systemProperty = System.getProperty("latitude41.api.url");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String environmentVariable = System.getenv("LATITUDE41_API_URL");
        if (environmentVariable != null && !environmentVariable.isBlank()) {
            return environmentVariable;
        }

        Properties properties = new Properties();
        try (InputStream input = ApiService.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                properties.load(input);
                String configured = properties.getProperty("latitude41.api.url");
                if (configured != null && !configured.isBlank()) {
                    return configured;
                }
            }
        } catch (IOException ignored) {
            return "http://localhost:8080";
        }

        return "http://localhost:8080";
    }
}

class CrudApiService {

    protected final ApiService apiService;
    private final String resourcePath;

    CrudApiService(ApiService apiService, String resourcePath) {
        this.apiService = apiService;
        this.resourcePath = resourcePath;
    }

    public ObjectNode createObject() {
        return apiService.createObject();
    }

    public ArrayNode getAll() {
        return apiService.getArray(resourcePath);
    }

    public ArrayNode listarTodos() {
        return getAll();
    }

    public JsonNode getById(int id) {
        return apiService.getObject(resourcePath + "/" + id);
    }

    public JsonNode create(ObjectNode body) {
        return apiService.post(resourcePath, body);
    }

    public JsonNode update(int id, ObjectNode body) {
        return apiService.put(resourcePath + "/" + id, body);
    }

    public boolean delete(int id) {
        return apiService.delete(resourcePath + "/" + id);
    }
}

class MesaService extends CrudApiService {

    MesaService(ApiService apiService) {
        super(apiService, "/mesas");
    }
}

class ProdutoService extends CrudApiService {

    ProdutoService(ApiService apiService) {
        super(apiService, "/produtos");
    }
}

class ReservaService extends CrudApiService {

    ReservaService(ApiService apiService) {
        super(apiService, "/reservas");
    }
}

class StockService extends CrudApiService {

    StockService(ApiService apiService) {
        super(apiService, "/stocks");
    }
}

class IngredienteService extends CrudApiService {

    IngredienteService(ApiService apiService) {
        super(apiService, "/ingredientes");
    }
}

class UtilizadorService extends CrudApiService {

    UtilizadorService(ApiService apiService) {
        super(apiService, "/utilizadores");
    }

    public Optional<JsonNode> login(String email, String password) {
        ObjectNode body = createObject();
        body.put("email", email);
        body.put("password", password);

        try {
            return Optional.of(apiService.post("/auth/login", body));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("HTTP 401")) {
                return Optional.empty();
            }
            throw e;
        }
    }
}

class PedidoService extends CrudApiService {

    PedidoService(ApiService apiService) {
        super(apiService, "/pedidos");
    }

    public JsonNode createCompleto(ObjectNode body) {
        return apiService.post("/pedidos/completo", body);
    }

    public ArrayNode getPedidosCompletosByMesa(int mesaId) {
        return apiService.getArray("/pedidos/mesa/" + mesaId + "/completos");
    }
}

class FaturaService extends CrudApiService {

    FaturaService(ApiService apiService) {
        super(apiService, "/faturas");
    }

    public JsonNode gerarFatura(ObjectNode body) {
        return apiService.post("/faturas/gerar", body);
    }
}

class RelatorioService {

    private final ApiService apiService;

    RelatorioService(ApiService apiService) {
        this.apiService = apiService;
    }

    public JsonNode analise(String dataInicio, String dataFim) {
        return apiService.getObject("/relatorios/analise?dataInicio=" + dataInicio + "&dataFim=" + dataFim);
    }

    public JsonNode exportarPdf(String dataInicio, String dataFim) {
        return apiService.getObject("/relatorios/exportar-pdf?dataInicio=" + dataInicio + "&dataFim=" + dataFim);
    }
}
