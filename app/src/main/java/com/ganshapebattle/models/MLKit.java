package com.ganshapebattle.models; // Đảm bảo package của bạn là đúng

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.ImagenModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.ImagePart;
import com.google.firebase.ai.type.Part;
import com.google.firebase.ai.type.ResponseModality;


import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// THÊM CHÚ THÍCH OPT-IN TẠI ĐÂY:
@com.google.firebase.ai.type.PublicPreviewAPI // <--- SỬA LỖI 3
public class MLKit { // Bạn có thể đổi tên lớp này thành ImagenControlNet
    private static final String TAG = "GeminiImageEditor";
    private static final String MODEL_NAME = "gemini-2.0-flash-preview-image-generation";

    private static final GenerativeModelFutures model;

    static {
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel(
                MODEL_NAME,
                new GenerationConfig.Builder()
                        .setResponseModalities(Arrays.asList(ResponseModality.TEXT, ResponseModality.IMAGE))
                        .build());

        model = GenerativeModelFutures.from(ai);
        Log.i(TAG, "Gemini Image Editor static model initialized.");
    }

    /**
     * Hàm static để chỉnh sửa ảnh bằng Gemini.
     */
    public static ListenableFuture<Bitmap> editImage(
            @NonNull Bitmap originalImage,
            @NonNull String textPrompt,
            @NonNull Executor executor
    ) {
        Content promptContent = new Content.Builder()
                .addImage(originalImage)
                .addText(textPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(promptContent);

        ListenableFuture<Bitmap> bitmapFuture = Futures.transform(
                responseFuture,
                (GenerateContentResponse result) -> {
                    // Lấy logic trích xuất Bitmap từ đoạn code của bạn:
                    for (Part part : result.getCandidates().get(0).getContent().getParts()) {
                        if (part instanceof ImagePart) {
                            ImagePart imagePart = (ImagePart) part;
                            return imagePart.getImage(); // Trả về Bitmap
                        }
                    }

                    // === SỬA LỖI TẠI ĐÂY ===
                    // Ném (throw) một UNCHECKED exception để làm FAILED Future
                    // (Không dùng "new Exception(...)")
                    throw new IllegalStateException("No image part found in Gemini response.");
                    // Bạn cũng có thể dùng:
                    // throw new java.util.NoSuchElementException("No image part found in Gemini response.");

                },
                executor // Tác vụ biến đổi (transform) chạy trên executor này
        );

        return bitmapFuture;
    }
}