package com.ganshapebattle.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public class ImageUtils {
    private static Bitmap getBitmapFromImageView(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();

        // Nếu không có ảnh, trả về null
        if (drawable == null) {
            return null;
        }

        // Trường hợp 1: Nếu là BitmapDrawable, lấy bitmap trực tiếp (nhanh nhất)
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // Trường hợp 2: Nếu là loại Drawable khác (Vector, Color, GlideDrawable...)
        // Ta phải tạo một Bitmap mới và "vẽ" Drawable đó lên.
        try {
            // Tạo một Bitmap trống với kích thước của Drawable
            Bitmap bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),  // Lấy chiều rộng gốc
                    drawable.getIntrinsicHeight(), // Lấy chiều cao gốc
                    Bitmap.Config.ARGB_8888
            );

            // Tạo một Canvas để vẽ lên Bitmap
            Canvas canvas = new Canvas(bitmap);

            // Đặt kích thước cho Drawable (để nó biết vẽ to bao nhiêu)
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());

            // Vẽ Drawable lên Canvas (Canvas này đang "vẽ" lên Bitmap)
            drawable.draw(canvas);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setBitmapToImageView(ImageView imageView, Bitmap bitmap) {
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
        // Lưu ý: Nếu bitmap là null, imageView.setImageBitmap(null)
        // sẽ tự động xóa hình ảnh hiện tại khỏi ImageView.
    }

    /**
     * Chuyển đổi (encode) một Bitmap thành chuỗi Base64.
     *
     * @param bitmap Bitmap cần chuyển đổi.
     * @param format Định dạng nén (ví dụ: Bitmap.CompressFormat.PNG hoặc Bitmap.CompressFormat.JPEG).
     * @param quality Chất lượng nén (từ 0-100).
     * Nếu là null, hàm sẽ tự động dùng 100 (chất lượng tối đa).
     * PNG sẽ bỏ qua giá trị này (luôn là lossless).
     * @return Một chuỗi Base64 đại diện cho hình ảnh, hoặc null nếu có lỗi.
     */
    public static String bitmapToBase64(Bitmap bitmap, Bitmap.CompressFormat format, Integer quality) {
        if (bitmap == null) {
            return null;
        }

        // --- ĐÃ CẬP NHẬT ---
        // Nếu quality là null, gán bằng 100.
        // Đồng thời đảm bảo giá trị luôn nằm trong khoảng 0-100.
        int compressionQuality = 100; // Mặc định là 100
        if (quality != null) {
            compressionQuality = Math.max(0, Math.min(100, quality)); // Kẹp giá trị trong khoảng 0-100
        }
        // ---------------------

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // Nén bitmap thành byte array
            bitmap.compress(format, compressionQuality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            // Encode byte array thành Base64 string
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Chuyển đổi (decode) một chuỗi Base64 thành Bitmap.
     *
     * @param base64String Chuỗi Base64 cần giải mã.
     * @return Một đối tượng Bitmap, hoặc null nếu chuỗi đầu vào không hợp lệ.
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }

        try {
            // Decode Base64 string thành byte array
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Chuyển byte array thành Bitmap
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            // Lỗi nếu base64String không hợp lệ
            e.printStackTrace();
            return null;
        }
    }
}
