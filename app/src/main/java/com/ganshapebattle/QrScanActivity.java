package com.ganshapebattle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
//import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Activity nhận Intent từ ứng dụng/quy trình quét QR và giải mã nội dung.
 *
 * Hỗ trợ các trường hợp phổ biến:
 * - ACTION_VIEW với Data URI (camera hoặc deep link)
 * - ZXing: extras "SCAN_RESULT" và "SCAN_RESULT_FORMAT"
 * - Extra tuỳ chỉnh: "qr_payload" (text)
 *
 * Kết quả trả về cho caller (RESULT_OK):
 * - "qr_raw": Chuỗi gốc sau khi lấy từ Intent
 * - "qr_text": Chuỗi đã cố gắng URL-decode / Base64-decode (nếu áp dụng)
 * - "qr_uri": Chuỗi URI nếu dữ liệu là một URI hợp lệ
 */
public class QrScanActivity extends AppCompatActivity {

    public static final String EXTRA_QR_RAW = "qr_raw";
    public static final String EXTRA_QR_TEXT = "qr_text";
    public static final String EXTRA_QR_URI = "qr_uri";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent inbound = getIntent();
        String raw = extractRawPayload(inbound);

        if (raw != null && !raw.isEmpty()) {
            handlePayloadAndFinish(raw);
        } else {
            // Không có dữ liệu đi vào → chủ động mở app quét QR (ZXing) và đợi kết quả
            tryLaunchZxingScanner();
        }
    }

    private void handlePayloadAndFinish(String raw) {
        String decoded = tryDecode(raw);
        String asUri = tryAsUri(decoded != null ? decoded : raw);

        Intent result = new Intent();
        result.putExtra(EXTRA_QR_RAW, raw);
        if (decoded != null) {
            result.putExtra(EXTRA_QR_TEXT, decoded);
        }
        if (asUri != null) {
            result.putExtra(EXTRA_QR_URI, asUri);
        }
        setResult(RESULT_OK, result);

        String preview = decoded != null ? decoded : raw;
//        Toast.makeText(this, preview.length() > 200 ? preview.substring(0, 200) + "…" : preview, Toast.LENGTH_SHORT).show();

        finish();
    }

    private void tryLaunchZxingScanner() {
        try {
            Intent scanIntent = new Intent("com.google.zxing.client.android.SCAN");
            scanIntent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(scanIntent, 9911);
        } catch (Exception e) {
            // Không có app ZXing → trả về CANCELED
            Log.w("QrScanActivity", "ZXing scanner not available: " + e.getMessage());
            setResult(RESULT_CANCELED);
//            Toast.makeText(this, "Không tìm thấy ứng dụng quét QR.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9911) {
            if (resultCode == RESULT_OK && data != null) {
                String scanResult = data.getStringExtra("SCAN_RESULT");
                if (scanResult != null && !scanResult.isEmpty()) {
                    handlePayloadAndFinish(scanResult);
                    return;
                }
            }
            // Người dùng huỷ hoặc không có dữ liệu
            setResult(RESULT_CANCELED);
//            Toast.makeText(this, "Huỷ quét hoặc không có dữ liệu.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String extractRawPayload(@Nullable Intent intent) {
        if (intent == null) return null;

        // 1) ACTION_VIEW với Data URI
        Uri data = intent.getData();
        if (data != null) {
            String dataString = data.toString();
            if (!dataString.isEmpty()) return dataString;
        }

        // 2) ZXing chuẩn
        if (intent.hasExtra("SCAN_RESULT")) {
            String scanResult = intent.getStringExtra("SCAN_RESULT");
            if (scanResult != null && !scanResult.isEmpty()) return scanResult;
        }

        // 3) Extra tuỳ chỉnh phổ biến
        if (intent.hasExtra("qr_payload")) {
            String custom = intent.getStringExtra("qr_payload");
            if (custom != null && !custom.isEmpty()) return custom;
        }

        // 4) Fallback: thử lấy bất kỳ extra text nào tên "text"/"data"
        if (intent.hasExtra("text")) {
            String t = intent.getStringExtra("text");
            if (t != null && !t.isEmpty()) return t;
        }
        if (intent.hasExtra("data")) {
            String t = intent.getStringExtra("data");
            if (t != null && !t.isEmpty()) return t;
        }

        return null;
    }

    private String tryDecode(@Nullable String input) {
        if (input == null || input.isEmpty()) return null;

        // Thử URL decode trước
        String urlDecoded = tryUrlDecode(input);
        if (urlDecoded != null && !urlDecoded.equals(input)) {
            // Nếu URL decode thay đổi nội dung, dùng nội dung đã decode
            input = urlDecoded;
        }

        // Thử Base64 (chuẩn, không wrap)
        String b64 = tryBase64(input);
        if (b64 != null) return b64;

        return input;
    }

    private String tryUrlDecode(String input) {
        try {
            // Tránh decode nếu không có dấu hiệu encoding
            if (!input.contains("%") && !input.contains("+")) return input;
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return input;
        } catch (IllegalArgumentException e) {
            // Malformed % sequence
            Log.w("QrScanActivity", "URL decode failed: " + e.getMessage());
            return input;
        }
    }

    private String tryBase64(String input) {
        // Heuristic: Base64 thường có độ dài bội số 4 và chỉ gồm [A-Za-z0-9+/=]
        String trimmed = input.trim();
        if (trimmed.length() % 4 != 0) return null;
        if (!trimmed.matches("[A-Za-z0-9+/=\\r\\n]+")) return null;
        try {
            byte[] decoded = Base64.decode(trimmed, Base64.DEFAULT);
            String asText = new String(decoded, "UTF-8");
            // Chỉ nhận nếu là chuỗi in được
            if (asText.trim().isEmpty() && !trimmed.isEmpty()) return null;
            return asText;
        } catch (Exception e) {
            return null;
        }
    }

    private String tryAsUri(@Nullable String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            Uri uri = Uri.parse(input);
            if (uri.getScheme() != null) {
                return uri.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
