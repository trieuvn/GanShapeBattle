package com.ganshapebattle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.UUID;
import java.util.concurrent.Executor;

import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ganshapebattle.models.MLKit;
import com.ganshapebattle.view.DrawingView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.PublicPreviewAPI;

public class DesignActivity extends AppCompatActivity implements OnClickListener {

    private DrawingView drawView;
    private ImageButton currPaint, drawBtn, eraseBtn, newBtn, saveBtn;
    private float smallBrush, mediumBrush, largeBrush;
    private ImageView ganImage;

    private Button transformBtn;
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_design);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        context = this;

        drawView = (DrawingView)findViewById(R.id.drawing);
        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);

// 1. Lấy LinearLayout "wrapper" con đầu tiên (ví dụ: wrapper của màu "sky")
        LinearLayout firstColorWrapper = (LinearLayout) paintLayout.getChildAt(0);

// 2. Tìm ImageButton bên trong wrapper đó (nó là con đầu tiên của wrapper)
        currPaint = (ImageButton) firstColorWrapper.getChildAt(0);

// 3. (Nên sửa) Sử dụng ContextCompat để lấy drawable (an toàn hơn)
        currPaint.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.paint_pressed));
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        drawView.setBrushSize(mediumBrush);

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        ganImage = (ImageView)findViewById(R.id.ganimage);
        ganImage.setImageBitmap(drawView.getGanImage());
        ganImage.setOnClickListener(this);

        transformBtn = (Button)findViewById(R.id.transform_btn);
        transformBtn.setOnClickListener(this);
    }

    public void paintClicked(View view){
        if(view!=currPaint){
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
        }
        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
    }

    @OptIn(markerClass = PublicPreviewAPI.class)
    private void startTransformEdit() {
        AlertDialog.Builder transformingDialogBuilder = new AlertDialog.Builder(context);

        LayoutInflater inflater = this.getLayoutInflater(); // Hoặc LayoutInflater.from(context)
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        transformingDialogBuilder.setView(dialogView);

        ImageView gifImageView = dialogView.findViewById(R.id.gif_image_view);

        Glide.with(context)
                .load(R.drawable.transforming)
                .into(gifImageView);

        transformingDialogBuilder.setTitle(getResources().getString(R.string.transforming));

        AlertDialog transformingDialog = transformingDialogBuilder.create();
        transformingDialog.show();
        drawView.setDrawingCacheEnabled(true);
        Bitmap originalBitmap = drawView.getDrawingCache();

        String[] promptLines = getResources().getStringArray(R.array.prompt_photorealistic_segmentation_v2);

// 2. Nối mảng lại thành một String duy nhất, dùng ký tự xuống dòng "\n"
        StringBuilder promptBuilder = new StringBuilder();
        for (String line : promptLines) {
            promptBuilder.append(line).append("\n");
        }
        String imageEditPrompt = promptBuilder.toString();

        Executor mainExecutor = ContextCompat.getMainExecutor(this);

        Log.d("DesignActivity", "Calling static editImage function...");

        ListenableFuture<Bitmap> bitmapFuture = MLKit.editImage(
                originalBitmap,
                imageEditPrompt,
                mainExecutor
        );

        Futures.addCallback(bitmapFuture, new FutureCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap generatedBitmap) {
                drawView.setGanImage(generatedBitmap);
                ganImage.setImageBitmap(generatedBitmap);
                transformingDialog.cancel();
                AlertDialog.Builder ganImageDialog = new AlertDialog.Builder(context);
                ganImageDialog.setTitle(R.string.super_gan_image);

                ImageView imageView = new ImageView(context);

                imageView.setImageBitmap(drawView.getGanImage());

                int padding = 60;
                imageView.setPadding(padding, padding, padding, padding);
                imageView.setAdjustViewBounds(true);

                ganImageDialog.setView(imageView);
                ganImageDialog.setPositiveButton(R.string.ok, null);
                ganImageDialog.create().show();
            }

            @Override
            public void onFailure(Throwable t) {
                AlertDialog.Builder saveDialog = new AlertDialog.Builder(context);
                saveDialog.setTitle("FAIL");
                saveDialog.setMessage("That sucks...");
                saveDialog.show();
                transformingDialog.cancel();
            }
        }, mainExecutor); // Callback này cũng chạy trên Main Thread
        drawView.destroyDrawingCache();
    }

    @Override
    public void onClick(View view){
        if(view.getId()==R.id.draw_btn){
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(smallBrush);
                    drawView.setLastBrushSize(smallBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(mediumBrush);
                    drawView.setLastBrushSize(mediumBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setBrushSize(largeBrush);
                    drawView.setLastBrushSize(largeBrush);
                    drawView.setErase(false);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();

        }else if(view.getId()==R.id.erase_btn){
            //switch to erase - choose size
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);

            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });

            brushDialog.show();
        }
        else if(view.getId()==R.id.new_btn){
            //new button
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle(R.string.new_drawing);
            newDialog.setMessage(R.string.start_new_msg);
            newDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();
        }
        else if(view.getId()==R.id.save_btn){
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle(R.string.savedrawing);
            saveDialog.setMessage(R.string.savedrawing_msg);
            saveDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString()+".png", "drawing");
                    if(imgSaved!=null){
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                R.string.drawing_saved_to_gallery, Toast.LENGTH_SHORT);
                        savedToast.show();
                    }
                    else{
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                R.string.oops_image_could_not_be_saved, Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }
                    drawView.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();
        }
        else if (view.getId() == R.id.ganimage){
            AlertDialog.Builder ganImageDialog = new AlertDialog.Builder(this);
            ganImageDialog.setTitle(R.string.super_gan_image);

            ImageView imageView = new ImageView(this);

            imageView.setImageBitmap(drawView.getGanImage());

            int padding = 60;
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setAdjustViewBounds(true);

            ganImageDialog.setView(imageView);
            ganImageDialog.setPositiveButton(R.string.ok, null);
            ganImageDialog.create().show();
        }
        else if (view.getId() == R.id.transform_btn){
            startTransformEdit();
        }
    }
}