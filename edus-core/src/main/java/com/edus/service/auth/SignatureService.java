package com.edus.service.auth;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.edus.common.components.CacheManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;

/**
 * 请求签名服务，用于在没有登录 token 的情况下访问接口。
 *
 * <p>签名只使用 5 个字段：请求方法、URL、时间戳、防重复 nonce、appId。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    public static final String HEADER_APP_ID = "X-Sign-AppId";
    public static final String HEADER_TIMESTAMP = "X-Sign-Timestamp";
    public static final String HEADER_NONCE = "X-Sign-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private static final String NONCE_KEY_PREFIX = "request:sign:nonce:";

    private final CacheManager cacheManager;

    @Value("${edus.sign.enabled:true}")
    private boolean enabled;

    @Value("${edus.sign.app-id:edus-open-api}")
    private String appId;

    @Value("${edus.sign.secret:edus-sign-secret}")
    private String secret;

    @Value("${edus.sign.timestamp-expire-seconds:300}")
    private long timestampExpireSeconds;

    /**
     * 判断请求是否带了签名相关请求头。
     *
     * <p>这里只用于区分错误提示：带了签名头但验签失败时提示“签名验证失败”；
     * 完全没有 token 和签名头时提示“未登录或 token 已过期”。</p>
     */
    public boolean hasSignatureHeaders(HttpServletRequest request) {
        return StrUtil.isNotBlank(request.getHeader(HEADER_SIGNATURE))
                || StrUtil.isNotBlank(request.getHeader(HEADER_TIMESTAMP))
                || StrUtil.isNotBlank(request.getHeader(HEADER_NONCE));
    }

    /**
     * 校验当前请求签名是否合法。
     *
     * <p>校验顺序：
     * 1. 检查签名开关和必要请求头；
     * 2. 校验 appId 是否匹配；
     * 3. 校验时间戳是否在允许窗口内；
     * 4. 使用请求方法、URL、时间戳、nonce、appId 拼接签名原文；
     * 5. 计算 HMAC-SHA256 并与请求签名做常量时间比较；
     * 6. 签名通过后写入 nonce，防止同一个签名被重复使用。</p>
     */
    public boolean validate(HttpServletRequest request) {
        if (!enabled) {
            return false;
        }

        String requestAppId = request.getHeader(HEADER_APP_ID);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String signature = request.getHeader(HEADER_SIGNATURE);

        if (StrUtil.hasBlank(requestAppId, timestamp, nonce, signature, secret)) {
            return false;
        }
        if (StrUtil.isNotBlank(appId) && !appId.equals(requestAppId)) {
            return false;
        }
        if (!isTimestampValid(timestamp)) {
            return false;
        }

        // 先验签，验签通过后再占用 nonce，避免错误签名消耗防重复值。
        String canonical = buildCanonicalString(request, requestAppId, timestamp, nonce);
        String expectedSignature = hmacSha256Hex(canonical, secret);
        if (!constantTimeEquals(expectedSignature, signature)) {
            return false;
        }
        return markNonce(requestAppId, nonce);
    }

    /**
     * 构建签名原文。
     *
     * <p>调用方必须按完全相同的顺序和换行符拼接：
     * 请求方法、URL、时间戳、防重复 nonce、appId。</p>
     *
     * <pre>
     * GET
     * /api/user/list
     * 1720000000000
     * 8f2d1c
     * edus-dev-open-api
     * </pre>
     */
    public String buildCanonicalString(HttpServletRequest request, String appId, String timestamp, String nonce) {
        return request.getMethod().toUpperCase(Locale.ROOT)
                + "\n" + buildRequestUrl(request)
                + "\n" + timestamp
                + "\n" + nonce
                + "\n" + appId;
    }

    /**
     * 构建参与签名的 URL。
     *
     * <p>这里只使用 URI，不包含域名、协议和 queryString，避免 query 参数顺序不同导致签名失败。</p>
     */
    private String buildRequestUrl(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * 校验时间戳是否在允许窗口内。
     *
     * <p>支持秒级和毫秒级时间戳；默认允许前后 300 秒，避免网络延迟造成误判。</p>
     */
    private boolean isTimestampValid(String timestamp) {
        try {
            long requestTime = Long.parseLong(timestamp);
            if (timestamp.length() == 10) {
                requestTime = requestTime * 1000;
            }
            long now = Instant.now().toEpochMilli();
            long allowedMillis = timestampExpireSeconds * 1000;
            return Math.abs(now - requestTime) <= allowedMillis;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 将 nonce 写入 Redis，防止请求重放。
     *
     * <p>setIfAbsent 成功表示这个 nonce 第一次出现；如果已经存在，说明同一个签名
     * 在有效期内被重复使用，需要拒绝。</p>
     */
    private boolean markNonce(String appId, String nonce) {
        String key = NONCE_KEY_PREFIX + appId + ":" + nonce;
        return cacheManager.setIfAbsent(key, "1", timestampExpireSeconds);
    }

    /**
     * 使用 Hutool 计算 HMAC-SHA256 签名，输出十六进制字符串。
     */
    private String hmacSha256Hex(String data, String key) {
        HMac hMac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, StrUtil.bytes(key, CharsetUtil.CHARSET_UTF_8));
        return hMac.digestHex(data, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * 常量时间比较签名。
     *
     * <p>不使用普通 equals，是为了避免攻击者通过比较耗时推断签名前缀是否正确。</p>
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                StrUtil.bytes(expected.toLowerCase(Locale.ROOT), CharsetUtil.CHARSET_UTF_8),
                StrUtil.bytes(StrUtil.trim(actual).toLowerCase(Locale.ROOT), CharsetUtil.CHARSET_UTF_8)
        );
    }
}
