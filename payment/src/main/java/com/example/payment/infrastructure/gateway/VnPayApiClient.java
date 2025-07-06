package com.example.payment.infrastructure.gateway;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
public class VnPayApiClient {
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_TMNCODE = "ONKJNU1D";
    private static final String VNP_RETURN_URL = "http://localhost:8080/api/payments/vnpay/return";
    private static final String VNP_HASH_SECRET = "T81UQ58X7Q54PYBVEO6FJ4AKQSUAP1S2";
    private static final String VNP_BASE_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public String generatePaymentUrl(String bookingId, BigDecimal amount, LocalDateTime expiryTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDateTime now = LocalDateTime.now(vnZone);

            String formattedExpireDate = expiryTime.atZone(vnZone).format(formatter);
            String formattedCreateDate = now.format(formatter);

            Map<String, String> vnpParams = new TreeMap<>();
            vnpParams.put("vnp_Version", VNP_VERSION);
            vnpParams.put("vnp_Command", VNP_COMMAND);
            vnpParams.put("vnp_TmnCode", VNP_TMNCODE);
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", bookingId);
            vnpParams.put("vnp_OrderInfo", "Payment for booking: " + bookingId);
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).intValue() + "");
            vnpParams.put("vnp_ReturnUrl", VNP_RETURN_URL);
            vnpParams.put("vnp_CreateDate", formattedCreateDate);
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            vnpParams.put("vnp_ExpireDate", formattedExpireDate);

            String signData = buildSignData(vnpParams);
            String secureHash = generateHMAC(VNP_HASH_SECRET, signData);
            vnpParams.put("vnp_SecureHash", secureHash);

            return buildPaymentUrl(VNP_BASE_URL, vnpParams);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate VNPAY URL", e);
        }
    }

    private String buildSignData(Map<String, String> params) throws Exception {
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (signData.length() > 0) {
                signData.append("&");
            }
            signData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return signData.toString();
    }

    private String buildPaymentUrl(String baseUrl, Map<String, String> params) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (urlBuilder.length() > baseUrl.length() + 1) {
                urlBuilder.append("&");
            }
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return urlBuilder.toString();
    }

    private String generateHMAC(String secretKey, String signData) throws Exception {
        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSha512.init(secretKeySpec);

        byte[] hashBytes = hmacSha512.doFinal(signData.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : hashBytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}