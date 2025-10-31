package com.ganshapebattle.adapters;

import android.content.Context;
import android.graphics.Bitmap; // <-- Thêm
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// import com.bumptech.glide.Glide; // <-- Xóa Glide
import com.ganshapebattle.R;
import com.ganshapebattle.models.PlayerScore;
import com.ganshapebattle.utils.ImageUtils; // <-- Thêm ImageUtils

import java.util.ArrayList;

public class LeaderboardAdapter extends ArrayAdapter<PlayerScore> {

    private Context mContext;
    private int mResource;

    public LeaderboardAdapter(@NonNull Context context, @NonNull ArrayList<PlayerScore> objects) {
        super(context, R.layout.item_leaderboard_player, objects); // Sử dụng layout item tùy chỉnh
        this.mContext = context;
        this.mResource = R.layout.item_leaderboard_player;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Lấy thông tin người chơi
        PlayerScore player = getItem(position);

        // Thứ hạng (bắt đầu từ 6 vì Top 5 đã ở trên)
        int rank = position + 6;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
        }

        // Liên kết views trong item layout
        TextView tvRank = convertView.findViewById(R.id.tvPlayerRank);
        ImageView ivAvatar = convertView.findViewById(R.id.ivPlayerAvatar);
        TextView tvName = convertView.findViewById(R.id.tvPlayerName);
        TextView tvScore = convertView.findViewById(R.id.tvPlayerScore);

        if (player != null) {
            tvRank.setText(String.valueOf(rank));
            tvName.setText(player.getUsername());
            tvScore.setText(String.valueOf(player.getScore()) + " pts");

            // --- SỬA LỖI: Thay thế Glide bằng Base64 decode ---
            String base64String = player.getPictureBase64();
            Bitmap bitmap = ImageUtils.base64ToBitmap(base64String);

            if (bitmap != null) {
                ivAvatar.setImageBitmap(bitmap);
            } else {
                // Nếu ảnh lỗi hoặc null, dùng ảnh mặc định
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
            }
            // --- KẾT THÚC SỬA LỖI ---
        }
        return convertView;
    }
}
