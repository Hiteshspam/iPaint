package com.example.ipaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ipaint.Interface.ToolsListener;
import com.example.ipaint.adabters.ToolsAdabters;
import com.example.ipaint.model.ToolsItem;
import com.example.ipaint.widget.PaintSurfaceView;
import com.example.ipaint.widget.PaintView;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import common.common;

public class MainActivity extends AppCompatActivity implements ToolsListener {

    private static final int REQUEST_PERMISSION = 1001 ;
    private static final int PICK_IMAGE = 1000;
    private static final int REQUEST_FOR_IMAGE_FROM_GALLERY = 1002;
    PaintSurfaceView mPaintView;
    int colorBackground, colorBrush;
    int brushSize, eraserSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initTools();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mPaintView.startDrawThread();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mPaintView.stopDrawThread();
    }



    private void initTools() {
        colorBackground = Color.WHITE;
        colorBrush = Color.BLACK;
        eraserSize = brushSize = 5;
        mPaintView = findViewById(R.id.paint_view);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_tools);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        ToolsAdabters toolsAdabters = new ToolsAdabters(loadTools(), this);
        recyclerView.setAdapter(toolsAdabters);
    }

    private List<ToolsItem> loadTools() {

        List<ToolsItem> result = new ArrayList<>();

        result.add(new ToolsItem(R.drawable.ic_brush_black_24dp, common.BRUSH));
        result.add(new ToolsItem(R.drawable.eraser_white, common.ERASER));
        result.add(new ToolsItem(R.drawable.ic_image_black_24dp, common.IMAGE));
        result.add(new ToolsItem(R.drawable.ic_palette_black_24dp, common.COLORS));
        result.add(new ToolsItem(R.drawable.paint_white, common.BACKGROUND));
        result.add(new ToolsItem(R.drawable.ic_undo_black_24dp,common.RETURN));

        return result;
    }

    public void finishPaint(View view) {
        finish();
    }

    public void shareApp(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String bodyText = "Ok It is Made By Me(Hitesh)";
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT,bodyText);
        startActivity(Intent.createChooser(intent,"Share This App"));
    }

    public void showFiles(View view) {
        startActivity(new Intent(this,ListFilesAct.class));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void saveFile(View view) {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED){
             requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_PERMISSION);
        }else{
            try {
                saveBitmap();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveBitmap() throws IOException {
        Bitmap bitmap = mPaintView.getBitmap();
        String file_name = UUID.randomUUID() + ".jpeg";

        OutputStream outputStream;
        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)+File.separator+getString(R.string.app_name));
        boolean saved;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + getString(R.string.app_name));
        }else{
            folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ File.separator + getString(R.string.app_name));
        }

        if(!folder.exists()){
            folder.mkdir();
        }
        File image = new File(folder + File.separator+file_name);
        Uri imageUri = Uri.fromFile(image);



        outputStream = new FileOutputStream(image);
        saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,file_name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+File.separator+getString(R.string.app_name));
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);
            outputStream = resolver.openOutputStream(uri);
            saved = bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);

        }else{
            sendPicturesToGallery(imageUri);
        }

        if(saved)
        {
            Toast.makeText(this, "Picture Saved", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Picture Not Saved", Toast.LENGTH_SHORT).show();
        }
        outputStream.flush();
        outputStream.close();;

    }

    private void sendPicturesToGallery(Uri imageUri) {
    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    intent.setData(imageUri);
    sendBroadcast(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length >=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if(requestCode == REQUEST_PERMISSION) {
                try {
                    saveBitmap();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(requestCode == REQUEST_FOR_IMAGE_FROM_GALLERY){
                getImage();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSelected(String name) {
        switch(name){
            case common.BRUSH:
                mPaintView.toMove = false;
                mPaintView.disableEraser();
                mPaintView.invalidate();
                showDialogSize(false);
                break;
            case common.ERASER:
                mPaintView.enableEraser();
                showDialogSize(true);
                break;
            case common.RETURN:
                mPaintView.returnLastAction();
                break;
            case common.BACKGROUND:
                updateColor(name);
                break;
            case common.COLORS:
                updateColor(name);
                break;
            case common.IMAGE:
                if((ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) !=PackageManager.PERMISSION_GRANTED)){
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_FOR_IMAGE_FROM_GALLERY);
            }else {
                    getImage();
                }
                break;


        }

    }

    private void getImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == PICK_IMAGE && data != null && resultCode == RESULT_OK)
        {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){

                try {
                    ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(data.getData(),"r");
                    Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor());
                    mPaintView.setImage(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
            else {
                Uri pickedImage = data.getData();
                String[] filepath = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(pickedImage, filepath, null, null, null);
                cursor.moveToFirst();
                String imagePath = cursor.getString(cursor.getColumnIndex(filepath[0]));

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
                mPaintView.setImage(bitmap);

                cursor.close();
            }


        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateColor(final String name) {

        final int color;
        if(name.equals(common.BACKGROUND)){
            color = colorBackground;

        }else{
            color = colorBrush;
        }
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose Color")
                .initialColor(color)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("OK", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int lastSelectedColor, Integer[] allColors) {
                        if(name.equals(common.BACKGROUND)){
                            colorBackground = lastSelectedColor;
                            mPaintView.setColorBackground(colorBackground);

                        }else{
                            colorBrush = lastSelectedColor;
                            mPaintView.setBrushColor(colorBrush);

                        }
                    }
                }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).build()
                .show();

    }

    private void showDialogSize(final boolean isEraser) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_dialog,null,false);

        TextView toolsSelected = view.findViewById(R.id.status_tools_selected);
        final TextView statusSize = view.findViewById(R.id.status_size);
        ImageView ivTools = view.findViewById(R.id.iv_tools);
        SeekBar seekBar = view.findViewById(R.id.seekbar_size);
        seekBar.setMax(99);

        if(isEraser){

            toolsSelected.setText("Eraser Size");
            ivTools.setImageResource(R.drawable.eraser_black);
            statusSize.setText("Selected Size : " + eraserSize);
        }else {
            toolsSelected.setText("Brush Size");
            ivTools.setImageResource(R.drawable.ic_brush_24dp);
            statusSize.setText("Selected Size : " + brushSize);
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(isEraser)
                {
                   eraserSize = i+1;
                   statusSize.setText("Selected Size : " +eraserSize);
                   mPaintView.setSizeEraser(eraserSize);
                }else{
                    brushSize = i+1;
                    statusSize.setText("Selected Size : " +brushSize);
                    mPaintView.setSizeBrush(brushSize);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setView(view);
        builder.show();
    }


}
