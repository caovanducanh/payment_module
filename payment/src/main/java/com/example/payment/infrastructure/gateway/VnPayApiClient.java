package com.example.payment.infrastructure.gateway;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Component
public class VnPayApiClient {
    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String VNP_TMNCODE = "ONKJNU1D"; // Thay bằng Terminal ID của bạn
    private static final String VNP_RETURN_URL = "http://localhost:8080/api/payments/vnpay/return";
    private static final String VNP_HASH_SECRET = "T81UQ58X7Q54PYBVEO6FJ4AKQSUAP1S2"; // Thay bằng Secret Key mới
    private static final String VNP_BASE_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public String generatePaymentUrl(String bookingId, BigDecimal amount, LocalDateTime expiryTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

            LocalDateTime now = LocalDateTime.now(vnZone);
            if (expiryTime == null || expiryTime.isBefore(now.plusMinutes(5))) {
                expiryTime = now.plusMinutes(15);
            }

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
            System.out.println("✅ Raw sign data: " + signData);

            String secureHash = generateHMAC(VNP_HASH_SECRET, signData);
            System.out.println("✅ Generated hash: " + secureHash);

            vnpParams.put("vnp_SecureHash", secureHash);

            String paymentUrl = buildPaymentUrl(VNP_BASE_URL, vnpParams);
            System.out.println("✅ Final payment URL: " + paymentUrl);

            return paymentUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate VNPAY URL", e);
        }
    }

    private String buildSignData(Map<String, String> params) throws Exception {
        StringBuilder signData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            signData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
        }
        return signData.deleteCharAt(signData.length() - 1).toString();
    }

    private String buildPaymentUrl(String baseUrl, Map<String, String> params) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(baseUrl).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .append("&");
        }
        return urlBuilder.deleteCharAt(urlBuilder.length() - 1).toString();
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
    public void testHashGeneration() throws Exception {
        String testData = "vnp_Amount=10000000&vnp_Command=pay&vnp_CreateDate=20250707080000&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Payment for booking: test123&vnp_OrderType=other&vnp_ReturnUrl=http://localhost:8080/api/payments/vnpay/return&vnp_TmnCode=ONKJNU1D&vnp_TxnRef=test123&vnp_Version=2.1.0";
        String hash = generateHMAC("T81UQ58X7Q54PYBVEO6FJ4AKQSUAP1S2", testData);
        System.out.println("Test hash: " + hash);
    }
}