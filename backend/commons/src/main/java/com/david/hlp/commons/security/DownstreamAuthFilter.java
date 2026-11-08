package com.david.hlp.commons.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Authenticates requests that reach an analytics or crawler instance directly.
 *
 * <p>The bearer token is validated against the primary API's authenticated
 * user-information endpoint instead of being trusted locally. Token
 * revocation and permission changes therefore take effect immediately, and
 * identity-service failures fail closed.</p>
 */
public class DownstreamAuthFilter implements Filter {

    private final DownstreamAuthProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DownstreamAuthFilter(DownstreamAuthProperties properties) {
        this(properties, createRestTemplate(properties), new ObjectMapper());
    }

    DownstreamAuthFilter(DownstreamAuthProperties properties, RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private static RestTemplate createRestTemplate(DownstreamAuthProperties properties) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        // MVC decodes segments and strips matrix parameters; reject ambiguous routes before authorization.
        if (path.contains("%") || path.contains(";") || path.contains("\\") || path.contains("//")
                || !path.equals(org.springframework.util.StringUtils.cleanPath(path))) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "请求路径必须使用规范形式");
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.length() <= "Bearer ".length()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "缺少有效的访问令牌");
            return;
        }

        try {
            List<String> grants = verify(authorization);
            if (isPermissionProtected(path) && !hasDashboardPermission(authorization)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有看板访问权限");
                return;
            }
            if (isTrigger(path) && !grants.contains(properties.getTriggerPermission())) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有触发分析任务的权限");
                return;
            }
        } catch (DownstreamAuthException e) {
            writeError(response, e.status, e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return matchesPrefix(path, properties.getPublicPaths());
    }

    private boolean isPermissionProtected(String path) {
        return matchesPrefix(path, properties.getPermissionProtectedPaths());
    }

    private boolean isTrigger(String path) {
        String triggerPath = properties.getTriggerPath();
        return triggerPath != null && !triggerPath.isEmpty() && triggerPath.equals(path);
    }

    private static boolean matchesPrefix(String path, List<String> prefixes) {
        if (prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isEmpty()
                    && (path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"))) {
                return true;
            }
        }
        return false;
    }

    private List<String> verify(String authorization) {
        JsonNode root = getJson(properties.getVerificationUrl(), authorization);
        JsonNode data = successfulData(root, "身份服务返回无效的权限数据");
        if (!data.isArray()) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务返回无效的权限数据");
        }
        List<String> grants = new ArrayList<>();
        for (JsonNode grant : data) {
            if (grant.isTextual() && !grant.asText().isEmpty()) {
                grants.add(grant.asText());
            }
        }
        return grants;
    }

    /**
     * Lets the primary API resolve the path's configured permission against the
     * caller's live enabled grants. Paths never have to equal permission IDs.
     */
    private boolean hasDashboardPermission(String authorization) {
        String url = UriComponentsBuilder.fromHttpUrl(properties.getPermissionCheckUrl())
                .queryParam("path", properties.getDashboardPath())
                .build().encode().toUriString();
        JsonNode data = successfulData(getJson(url, authorization), "身份服务返回无效的页面权限数据");
        if (!data.isBoolean()) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务返回无效的页面权限数据");
        }
        return data.booleanValue();
    }

    private JsonNode successfulData(JsonNode root, String invalidMessage) {
        if (root == null) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, invalidMessage);
        }
        JsonNode code = root.get("code");
        if (code != null && code.asInt(-1) == HttpServletResponse.SC_UNAUTHORIZED) {
            throw new DownstreamAuthException(HttpServletResponse.SC_UNAUTHORIZED, "访问令牌已失效");
        }
        if (code == null || code.asInt(-1) != HttpServletResponse.SC_OK) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, invalidMessage);
        }
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, invalidMessage);
        }
        return data;
    }

    private JsonNode getJson(String url, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpServletResponse.SC_UNAUTHORIZED) {
                throw new DownstreamAuthException(HttpServletResponse.SC_UNAUTHORIZED, "访问令牌已失效");
            }
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务暂时不可用");
        } catch (RestClientException e) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务暂时不可用");
        }
        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务返回无效响应");
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException e) {
            throw new DownstreamAuthException(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "身份服务返回无效响应");
        }
    }

    private JsonNode findRouter(JsonNode nodes, String parentPath) {
        for (JsonNode node : nodes) {
            String path = resolvePath(node.path("path").asText(""), parentPath);
            if (properties.getDashboardPath().equals(path)) {
                return node;
            }
            JsonNode children = node.get("children");
            if (children != null && children.isArray()) {
                JsonNode match = findRouter(children, path);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static String resolvePath(String raw, String parentPath) {
        String path = raw == null ? "" : raw.trim();
        if (path.isEmpty()) {
            return parentPath;
        }
        if (!path.startsWith("/")) {
            path = parentPath + "/" + path;
        }
        path = path.replaceAll("/{2,}", "/");
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", status);
        body.put("message", message);
        body.putNull("data");
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class DownstreamAuthException extends RuntimeException {
        private final int status;

        private DownstreamAuthException(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
