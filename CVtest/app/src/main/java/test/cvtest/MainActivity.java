package test.cvtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.features2d.*;

public class MainActivity extends AppCompatActivity {


    private static final String FILENAME = (Environment.getExternalStorageDirectory().getPath()+"/result.jpg");
    private ImageView imageView;
    private TextView textView;
    private double bestMatch;
    private int index = 0;
    Bitmap selectedImage;
    CVTree firstImage;
    CVTree secondImage;
    Mat finalImg;
    ProgressBar bar;
    private final int Pick_image = 1;
    Uri imageUri;
    private Button process;
    private Button pickImage;
    private int PERMISSION_REQUEST_CODE = 2;
    final static DataBaseHelper dataBaseHelper = new DataBaseHelper();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) findViewById(R.id.verify_progress);
        bar.setVisibility(View.INVISIBLE);
        textView = (TextView) findViewById(R.id.textView);
        textView.setVisibility(View.INVISIBLE);
        imageView = (ImageView) findViewById(R.id.imageView);
        pickImage = (Button) findViewById(R.id.button);

        pickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMultiplePermissions();
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, Pick_image);
                }
            }
        });

        process = (Button) findViewById(R.id.button2);
        process.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {

                    finalImg = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));

                    firstImage = new CVTree();
                    firstImage.setImageByBitmap(selectedImage);


                    Comparison comparison = new Comparison();
                    comparison.execute();
                }
            }
        });

        DataBaseLoad dataBaseLoad = new DataBaseLoad();
        dataBaseLoad.execute();



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent){

        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode){
            case Pick_image:
                if(resultCode == RESULT_OK){
                    try {
                        imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedImage = BitmapFactory.decodeStream(imageStream);
                        imageView.setImageBitmap(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }
    }


    /**
     * Запрос разрешений от пользователя
     */
    public void requestMultiplePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("permission have got");
            }
            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("permission have got");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * Класс поток для загрузки БД
     */
    private class DataBaseLoad extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            dataBaseHelper.fillList(MainActivity.this);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar.setVisibility(View.VISIBLE);
            pickImage.setVisibility(View.GONE);
            process.setVisibility(View.GONE);

        }

        @Override
        protected void onPostExecute(Void result){
            bar.setVisibility(View.GONE);
            pickImage.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Класс поток для сравнения деревьев
     */
    private class Comparison extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            int maximumNumberOfMatches = 500;
            index = 0;
            bestMatch = 0;

            for (int i = 0; i < dataBaseHelper.getSize(); i++) {
                secondImage = dataBaseHelper.get(i);
                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                MatOfDMatch matches = new MatOfDMatch();
                matcher.match(secondImage.descriptors, firstImage.descriptors, matches);

                ArrayList<DMatch> goodMatches = new ArrayList<DMatch>();
                List<DMatch> allMatches = matches.toList();

                double minDist = 90;
                for (int j = 0; j < secondImage.descriptors.rows(); j++) {
                    double dist = allMatches.get(j).distance;
                    if (dist < minDist) minDist = dist;
                }
                for (int j = 0; j < secondImage.descriptors.rows() && goodMatches.size() < maximumNumberOfMatches; j++) {
                    if (allMatches.get(j).distance <= 2 * minDist) {
                        goodMatches.add(allMatches.get(j));
                    }
                }
                MatOfDMatch goodEnough = new MatOfDMatch();
                goodEnough.fromList(goodMatches);

//                Features2d.drawMatches(secondImage.getImage(), secondImage.keyPoint, firstImage.getImage(), firstImage.keyPoint, goodEnough, finalImg, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DRAW_RICH_KEYPOINTS + Features2d.NOT_DRAW_SINGLE_POINTS);



                if (bestMatch <= (double)goodEnough.rows()/allMatches.size()){
                    bestMatch = (double)goodEnough.rows()/allMatches.size();
                    System.out.println(bestMatch);
                    index = i;
                }

//                System.out.println(index);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar.setVisibility(View.VISIBLE);
            pickImage.setVisibility(View.GONE);
            process.setVisibility(View.GONE);

        }

        @Override
        protected void onPostExecute(Void result){
            bar.setVisibility(View.GONE);
            pickImage.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            AssetManager assets = MainActivity.this.getAssets();
            try {
                InputStream is = assets.open(dataBaseHelper.get(index).getPath());
                imageView.setImageBitmap(BitmapFactory.decodeStream(is));
                textView.setText("This is "+ dataBaseHelper.get(index).getName());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }





}
