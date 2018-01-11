package com.example.solution_color;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity  {

    private static final int CAMERA = 1;
    private static final int SELECT_PICTURE = 0;
    private static final int TOOLBAR_ALPHA = 120;
    private int sketch = 35;
    private int saturation = 150;

    private String path;
    private static final String PATH_NAME = "path";
    private static final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getPath();

    private Drawable currentBackground;
    private Bitmap bitmap;
    private RelativeLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = (RelativeLayout) findViewById(R.id.relativeLayout);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setTitle("");

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.getBackground().setAlpha(TOOLBAR_ALPHA);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        DisplayMetrics metrics = this.getResources().getDisplayMetrics();

        SharedPreferences settings = getSharedPreferences(PATH_NAME, 0);
        String newPath = settings.getString("path", null);

        if(newPath != null) {
            path = newPath;
            bitmap = Camera_Helpers.loadAndScaleImage(newPath, metrics.heightPixels, metrics.widthPixels);
            setBackground(bitmap);
        }
        else {
            bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.gutters);
            setBackground(bitmap);
            path = DEFAULT_PATH;
        }
    }

    public void setBackground(Bitmap bmp) {
        currentBackground = new BitmapDrawable(getResources(), bmp);
        layout.setBackgroundDrawable(currentBackground);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
                break;
            case R.id.action_gallery:
                getImageFromGallery();
                break;
            case R.id.action_save:
                break;
            case R.id.action_share:
                share();
                break;
            case R.id.action_colorize:
                colorize();
                break;
            case R.id.action_edit:
                grayScale();
                break;
            case R.id.action_revert:
                revert();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void takePhoto(View view) {
        try {
            File file = File.createTempFile("camera", ".png", getExternalCacheDir());
            path = file.getAbsolutePath();
            Uri uri = Uri.fromFile(file);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(cameraIntent, CAMERA);
        }catch(java.io.IOException e) {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    public void getImageFromGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();

        Uri data = Uri.parse(pictureDirectoryPath);

        photoPickerIntent.setDataAndType(data, "image/*");

        startActivityForResult(photoPickerIntent, SELECT_PICTURE);
    }

    public void colorize() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int sketchy = preferences.getInt("sketchiness", sketch);
        int saturationy = preferences.getInt("saturation", saturation);

        Bitmap colorizedbmp = BitMap_Helpers.colorBmp(bitmap, saturationy);
        BitMap_Helpers.merge(colorizedbmp, BitMap_Helpers.thresholdBmp(bitmap, sketchy));

        Bitmap bmp = colorizedbmp;
        setBackground(bmp);
        Camera_Helpers.saveProcessedImage(bmp, path);
    }

    public void grayScale() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int sketchy = preferences.getInt("sketchiness", sketch);

        Bitmap bmp = BitMap_Helpers.thresholdBmp(bitmap, sketchy);
        setBackground(bmp);
        Camera_Helpers.saveProcessedImage(bmp, path);
    }

    public void revert() {
        Camera_Helpers.delSavedImage(path);
        bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.gutters);
        setBackground(bitmap);
        Camera_Helpers.saveProcessedImage(bitmap, path);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        int screenHeight = metrics.heightPixels;
        int screenWidth = metrics.widthPixels;

        if (requestCode == CAMERA && resultCode == Activity.RESULT_OK) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            bitmap = Camera_Helpers.loadAndScaleImage(path, screenHeight, screenWidth);

            Drawable d = new BitmapDrawable(getResources(), bitmap);
            currentBackground = d;
            ViewGroup relativeLayout = (ViewGroup) findViewById(R.id.relativeLayout);
            relativeLayout.setBackgroundDrawable(d);
        }

        Toast.makeText(this, "Loading", Toast.LENGTH_LONG).show();

        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            InputStream inputStream;

            try {
                inputStream = getContentResolver().openInputStream(imageUri);

                //get a bitmap from the stream
                bitmap = BitmapFactory.decodeStream(inputStream);
                Camera_Helpers.saveProcessedImage(bitmap, path);

                bitmap = Camera_Helpers.loadAndScaleImage(path, screenHeight, screenWidth);
                setBackground(bitmap);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void share() {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, preferences.getString("text", "Default text"));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, preferences.getString("subject", "Default subject"));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, "photo"));
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences settings = getSharedPreferences(PATH_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("path", path);
        editor.commit();
    }
}

