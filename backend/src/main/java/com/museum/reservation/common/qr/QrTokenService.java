package com.museum.reservation.common.qr;

import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class QrTokenService {

    private final byte[] secret;

    public QrTokenService(@Value("${app.qr.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generate(String orderNo, Instant expireAt) {
        String payload = orderNo + "." + expireAt.getEpochSecond();
        return Constants.TICKET_PREFIX + payload + "." + sign(payload);
    }

    public TicketPayload parse(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("票码不能为空");
        }
        String raw = code.trim();
        if (raw.startsWith(Constants.TICKET_PREFIX)) {
            raw = raw.substring(Constants.TICKET_PREFIX.length());
        }
        String[] parts = raw.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException("电子票格式不正确");
        }
        String payload = parts[0] + "." + parts[1];
        String expected = sign(payload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("电子票签名无效，请使用本人电子票");
        }
        long epoch;
        try {
            epoch = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new BusinessException("电子票有效期无效");
        }
        if (Instant.now().getEpochSecond() > epoch) {
            throw new BusinessException("电子票已过期");
        }
        return new TicketPayload(parts[0], Instant.ofEpochSecond(epoch));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("电子票签名失败", e);
        }
    }

    public record TicketPayload(String orderNo, Instant expireAt) {
    }
}
