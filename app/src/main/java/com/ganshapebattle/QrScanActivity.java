package com.ganshapebattle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

// === THÊM 2 DÒNG IMPORT SAU ĐỂ SỬA LỖI BỊ ĐỎ ===
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
// =============================================

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

// === ĐẢM BẢO BẠN IMPORT ĐÚNG CLASS NÀY ===
import com.google.mlkit.vision.common.InputImage;
// =========================================

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// === THÊM ANNOTATION NÀY VÀO TRÊN CLASS ===
@OptIn(markerClass = ExperimentalGetImage.class)
// ==========================================
public class QrScanActivity extends AppCompatActivity {

    public static final String EXTRA_QR_RAW = "qr_raw";
    public static final String EXTRA_QR_TEXT = "qr_text";
    public static final String EXTRA_QR_URI = "qr_uri";

    private static final String TAG = "QrScanActivity_MLKit";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private volatile boolean isScannerBusy = false; // Cờ để tránh xử lý chồng chéo

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan); // Sử dụng layout mới

        previewView = findViewById(R.id.cameraPreview);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Yêu cầu quyền Camera
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Bạn phải cấp quyền camera để quét mã QR.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Lỗi khởi động camera: ", e);
                Toast.makeText(this, "Không thể khởi động camera.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // 1. Cấu hình Preview (Hiển thị lên màn hình)
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 2. Cấu hình Camera sau
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // 3. Cấu hình ML Kit Scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // 4. Cấu hình ImageAnalysis (Phân tích khung hình)
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (isScannerBusy) {
                image.close(); // Bỏ qua nếu đang bận
                return;
            }

            // Dòng @androidx.camera.core.ExperimentalGetImage không cần thiết nữa
            // vì chúng ta đã @OptIn ở cấp độ class
            InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

            if (inputImage == null) {
                image.close();
                return;
            }

            isScannerBusy = true;
            barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            // Tìm thấy mã!
                            Barcode barcode = barcodes.get(0);
                            String rawValue = barcode.getRawValue();
                            handlePayloadAndFinish(rawValue);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "ML Kit Barcode processing failed", e);
                    })
                    .addOnCompleteListener(task -> {
                        image.close(); // Luôn đóng ảnh
                        isScannerBusy = false; // Sẵn sàng cho khung hình tiếp theo
                    });
        });

        // 5. Gắn tất cả vào vòng đời (lifecycle)
        try {
            cameraProvider.unbindAll(); // Hủy liên kết cũ
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e(TAG, "Gắn Camera use cases thất bại", e);
        }
    }

    /**
     * Hàm này được gọi khi ML Kit quét được mã
     */
    private void handlePayloadAndFinish(String raw) {
        if (!isActivityRunning()) return; // Tránh gọi 2 lần

        Log.d(TAG, "Mã QR được phát hiện: " + raw);

        // Ngừng camera
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProviderFuture.get().unbindAll();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi unbind camera", e);
            }
        }, ContextCompat.getMainExecutor(this));

        // Trả kết quả về LobbyUserActivity
        Intent result = new Intent();
        result.putExtra(EXTRA_QR_RAW, raw);
        result.putExtra(EXTRA_QR_TEXT, raw); // Gửi giá trị thô

        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }

    private boolean isActivityRunning() {
        return !isFinishing() && !isDestroyed();
    }
}