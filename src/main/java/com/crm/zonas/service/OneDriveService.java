package com.crm.zonas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Conecta con Microsoft Graph API usando Client Credentials (sin login de usuario).
 * Requiere una App Registration en Azure AD con permiso Files.Read.All.
 */
@Service
public class OneDriveService {

    private static final Logger log = LoggerFactory.getLogger(OneDriveService.class);

    // Azure AD — obtenidos de la App Registration
    @Value("${azure.tenant-id}")
    private String tenantId;

    @Value("${azure.client-id}")
    private String clientId;

    @Value("${azure.client-secret}")
    private String clientSecret;

    // ID del usuario dueño de la carpeta (email o GUID del usuario en Azure AD
    @Value("${onedrive.user-id}")
    private String userId;

    // Ruta de la carpeta dentro de OneDrive del usuario
    // Ejemplo: "CRM/Archivos" si la carpeta está en OneDrive/CRM/Archivos
    @Value("${onedrive.folder-path}")
    private String folderPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Token ───────────────────────────────────────────────────────────────

    /**
     * Obtiene un access token usando Client Credentials Flow.
     * El token dura 1 hora; se solicita uno nuevo en cada ejecución del scheduler.
     */
    public String obtenerToken() throws Exception {
        String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "client_credentials");
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        body.add("scope",         "https://graph.microsoft.com/.default");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        JsonNode json = mapper.readTree(response.getBody());
        String token = json.get("access_token").asText();
        log.debug("Token de Microsoft Graph obtenido correctamente.");
        return token;
    }

    // ─── Listar archivos xlsx en la carpeta ──────────────────────────────────

    public record ArchivoOneDrive(String id, String nombre) {}

    /**
     * Lista todos los archivos .xlsx dentro de la carpeta configurada.
     * Usa la API: GET /users/{userId}/drive/root:/{folderPath}:/children
     */
    public List<ArchivoOneDrive> listarArchivosXlsx(String token) throws Exception {
        String url = "https://graph.microsoft.com/v1.0/users/" + userId
                + "/drive/root:/" + folderPath + ":/children"
                + "?$filter=endswith(name,'.xlsx')"
                + "&$select=id,name,createdDateTime"
                + "&$orderby=createdDateTime desc";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        JsonNode root  = mapper.readTree(response.getBody());
        JsonNode items = root.get("value");

        List<ArchivoOneDrive> archivos = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                archivos.add(new ArchivoOneDrive(
                        item.get("id").asText(),
                        item.get("name").asText()
                ));
            }
        }
        log.info("Archivos .xlsx encontrados en OneDrive: {}", archivos.size());
        return archivos;
    }

    // ─── Descargar contenido del archivo ─────────────────────────────────────

    /**
     * Descarga el contenido binario de un archivo por su ID.
     * Usa la API: GET /users/{userId}/drive/items/{itemId}/content
     */
    public byte[] descargarArchivo(String token, String itemId) throws Exception {
        String url = "https://graph.microsoft.com/v1.0/users/" + userId
                + "/drive/items/" + itemId + "/content";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);

        return response.getBody();
    }
}