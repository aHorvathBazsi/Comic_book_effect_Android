package com.example.firsteffect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.icu.util.Output;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
public class MainActivity extends AppCompatActivity {


    private static final int CAPTURE_PERMISSION_CODE = 1000;
    private static final int PICK_PERMISSION_CODE = 1001;
    private static final int IMAGE_CAPTURE_CODE =1002;
    private static final int IMAGE_PICK_CODE = 1003;


    Button mCaptureBtn;
    Button mChooseBtn;
    ImageView mImageView;
    Uri image_uri;
    Button mApplyFilter;
    OutputStream outputStream;

    Bitmap bmp;
    Bitmap operation;
    Bitmap dithered_image;
    Bitmap grayscale_image;
    Bitmap final_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView=findViewById(R.id.image_view);
        mCaptureBtn=findViewById(R.id.capture_image_btn);
        mChooseBtn = findViewById(R.id.choose_image_btn);
        mApplyFilter = findViewById(R.id.apply_filter);

        mCaptureBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(checkSelfPermission(Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED)
                    {
                        String[] permission = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission,CAPTURE_PERMISSION_CODE);
                    }
                    else{
                        openCamera();
                    }
                }
                else {
                    openCamera();
                }
            }



        });

        mChooseBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_DENIED)
                    {
                        String[] permission = {Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permission,PICK_PERMISSION_CODE);
                    }
                    else{
                        pickImageGallery();
                    }
                }
                else {
                    pickImageGallery();
                }
            }



        });

        mApplyFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    apply_effect();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAPTURE_PERMISSION_CODE:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openCamera();
                }
                else{
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
                }
            }
            case PICK_PERMISSION_CODE:{
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    pickImageGallery();
                }
                else{
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_CODE);
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION,"From Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_CAPTURE_CODE); }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode==IMAGE_CAPTURE_CODE) {
            mImageView.setImageURI(image_uri);
        }
        if(resultCode == RESULT_OK && requestCode==IMAGE_PICK_CODE){
            mImageView.setImageURI(data.getData());
        }
    }

    private void apply_effect() throws FileNotFoundException {
        mImageView.invalidate();
        BitmapDrawable abmp = (BitmapDrawable) mImageView.getDrawable();
        bmp = abmp.getBitmap();
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(),bmp.getConfig());
        dithered_image = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(),bmp.getConfig());

        for (int i=0;i<bmp.getWidth();i++){
            for (int j=0;j<bmp.getHeight();j++){
                int p=bmp.getPixel(i,j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);
                int alpha = Color.alpha(p);

                r = linear_transform(r);
                g = linear_transform(g);
                b = linear_transform(b);

                int[] new_values = add_noise2(r,g,b);
                r=new_values[0];
                g=new_values[1];
                b=new_values[2];

                operation.setPixel(i, j, Color.argb(alpha, r, g, b));
            }
        }
        SaveImage(operation);


        bmp.recycle();
        grayscale_image = toGrayScale(operation);
        SaveImage(grayscale_image);


        for (int j =0;j<grayscale_image.getHeight()-1;j++) {
            for (int i = 1; i < grayscale_image.getWidth()-1; i++) {
                int p = grayscale_image.getPixel(i, j);
                float oldR = Color.red(p);
                float oldG = Color.green(p);
                float oldB = Color.blue(p);
                int alpha = Color.alpha(p);

                int factor = 4;
                int newR = Math.round(factor*oldR/255)*(255/factor);
                int newG = Math.round(factor*oldG/255)*(255/factor);
                int newB = Math.round(factor*oldB/255)*(255/factor);
                dithered_image.setPixel(i,j,Color.argb(alpha,newR,newG,newB));

                float errR = oldR - newR;
                float errG = oldG - newG;
                float errB = oldB - newB;

                int p1 = grayscale_image.getPixel(i+1,j);
                int p2 = grayscale_image.getPixel(i-1,j+1);
                int p3 = grayscale_image.getPixel(i,j+1);
                int p4 = grayscale_image.getPixel(i+1,j+1);

                float factor1 = (float) 0.4375;
                float factor2 = (float) 0.1875;
                float factor3 = (float) 0.3125;
                float factor4 = (float) 0.0625;

                int[] p1_new = diffuse_error(p1, errR, errG, errB,factor1);
                dithered_image.setPixel(i+1,j,Color.argb(p1_new[0],p1_new[1],p1_new[2],p1_new[3]));

                int[] p2_new = diffuse_error(p2, errR, errG, errB,factor2);
                dithered_image.setPixel(i-1,j+1,Color.argb(p2_new[0],p2_new[1],p2_new[2],p2_new[3]));

                int[] p3_new = diffuse_error(p3, errR, errG, errB,factor3);
                dithered_image.setPixel(i,j+1,Color.argb(p3_new[0],p3_new[1],p3_new[2],p3_new[3]));

                int[] p4_new = diffuse_error(p4, errR, errG, errB,factor4);
                dithered_image.setPixel(i+1,j+1,Color.argb(p4_new[0],p4_new[1],p4_new[2],p4_new[3]));

            }
        }
        grayscale_image.recycle();

        SaveImage(dithered_image);

        final_image = Bitmap.createBitmap(dithered_image.getWidth(),dithered_image.getHeight(),dithered_image.getConfig());
        Canvas newCanvas = new Canvas(final_image);
        newCanvas.drawBitmap(operation,0,0,null);
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
        newCanvas.drawBitmap(dithered_image,0,0,paint);
        operation.recycle();
        dithered_image.recycle();
        mImageView.setImageBitmap(final_image);
        SaveImage(final_image);
    }

    private int[] diffuse_error(int p1, float errR, float errG, float errB,float factor) {

        float r = Color.red(p1);
        float g = Color.green(p1);
        float b = Color.blue(p1);
        int alpha = Color.alpha(p1);

        r = r + errR * factor;
        g = g + errG * factor;
        b = b + errB * factor;

        r = check_value((int) r);
        g = check_value((int) g);
        b = check_value((int) b);

        return new int[] { alpha,(int) r, (int) g,(int) b};

    }

    private int[] add_noise2(int r, int g, int b) {
        Random noise = new Random();
        float y = (float) (0.299*r + 0.587*g + 0.114*b);
        float u = (float) (0.492*(b-y));
        float v = (float) (0.877 * (r-y));
        int noise_value= (int) Math.round(noise.nextGaussian()*15);
        y = y + noise_value;

        r = (int) (y + 1.140*v);
        g = (int) (y - 0.395*u -0.581*v);
        b = (int) (y + 2.032*u);

        r = check_value(r);
        g = check_value(g);
        b = check_value(b);

        return new int[] {r, g, b};
    }

    private int check_value(int pixel_value){
        if(pixel_value<0){
            pixel_value = 0;
        }
        if(pixel_value>255){
            pixel_value = 255;
        }
        return pixel_value;
    }

    private int linear_transform(int pixel_value){
        if(pixel_value<60){
            pixel_value=0;
        }
        else{
            if(pixel_value>220){
                pixel_value=255;
            }
            else{
                pixel_value= (int) (1.59375*(pixel_value-60));
            }
        }
        return pixel_value;
    }

    public Bitmap toGrayScale(Bitmap bmpOriginal){

        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public void SaveImage(Bitmap bitmap) throws FileNotFoundException {

        OutputStream fOut = null;
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(System.currentTimeMillis());
        File path = Environment.getExternalStorageDirectory();
        File dir = new File(String.valueOf(path)+"/Comic_Effect");
        String imagename = time + ".PNG";
        File file = new File(dir,imagename);
        fOut = new FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.PNG,100,fOut);

        Toast.makeText(getApplicationContext(),"Image saved to internal memory",Toast.LENGTH_SHORT).show();

        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
