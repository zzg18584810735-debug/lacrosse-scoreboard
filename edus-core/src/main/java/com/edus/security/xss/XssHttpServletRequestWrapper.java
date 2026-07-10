package com.edus.security.xss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * XSS请求包装器
 * 重写请求参数和请求体获取方法，对参数值进行XSS过滤
 *
 * @author edus
 */
@Slf4j
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SENSITIVE_JSON_FIELDS = Set.of(
            "password", "oldpassword", "newpassword", "confirmpassword",
            "token", "accesstoken", "refreshtoken", "authorization",
            "signature", "secret", "apikey", "api_key"
    );

    private final byte[] cachedBody;
    private final boolean jsonRequest;

    public XssHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.jsonRequest = isJsonRequest(request);
        this.cachedBody = jsonRequest ? readAndCleanJsonBody(request) : null;
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }

        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXss(values[i]);
        }
        return cleanValues;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXss(value);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (!jsonRequest) {
            return super.getInputStream();
        }
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (!jsonRequest) {
            return super.getReader();
        }
        return new BufferedReader(new InputStreamReader(getInputStream(), getCharset()));
    }

    /**
     * 判断是否为JSON请求体。
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        return lowerContentType.contains("application/json") || lowerContentType.contains("+json");
    }

    /**
     * 读取并清洗JSON请求体，只清理字符串值，避免破坏JSON结构。
     */
    private byte[] readAndCleanJsonBody(HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readAllBytes();
        if (body.length == 0) {
            return body;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            JsonNode cleanedRoot = cleanJsonNode(root, null);
            return OBJECT_MAPPER.writeValueAsString(cleanedRoot).getBytes(getCharset());
        } catch (Exception e) {
            log.warn("JSON请求体XSS过滤失败，保留原始请求体", e);
            return body;
        }
    }

    /**
     * 递归清理JSON字符串值。
     */
    private JsonNode cleanJsonNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return node;
        }

        if (node.isTextual()) {
            if (isSensitiveJsonField(fieldName)) {
                return node;
            }
            return JsonNodeFactory.instance.textNode(cleanXss(node.asText()));
        }

        if (node.isObject()) {
            ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
            node.fields().forEachRemaining(entry ->
                    objectNode.set(entry.getKey(), cleanJsonNode(entry.getValue(), entry.getKey())));
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            node.forEach(item -> arrayNode.add(cleanJsonNode(item, fieldName)));
            return arrayNode;
        }

        return node;
    }

    /**
     * 凭证类字段不做内容改写，避免影响登录、验签和令牌校验。
     */
    private boolean isSensitiveJsonField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return SENSITIVE_JSON_FIELDS.contains(fieldName.replace("_", "").toLowerCase(Locale.ROOT));
    }

    private Charset getCharset() {
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }

    /**
     * 清理XSS攻击字符
     * 使用HTML过滤工具清理危险字符
     *
     * @param value 原始值
     * @return 清理后的安全值
     */
    private String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        try {
            // 使用XssUtil进行过滤
            return XssUtil.clean(value);
        } catch (Exception e) {
            log.warn("XSS过滤处理异常，返回原始值", e);
            return value;
        }
    }

    /**
     * 基于缓存字节数组的ServletInputStream。
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        private CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 同步读取场景无需额外处理。
        }

        @Override
        public int read() {
            return inputStream.read();
        }
    }
}
