package test.cvtest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.Scalar;
import org.opencv.features2d.*;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textView;
    private double bestMatch;
    private int index = 0;
    Bitmap selectedImage;
    CVLeaf firstImage;
    CVLeaf secondImage;
    Mat finalImg;
    ProgressBar bar;
    private final int PICK_IMAGE = 1;
    Uri imageUri;
    private Button process;
    private Button pickImage;
    private int PERMISSION_REQUEST_CODE = 2;
    private final int DIDNT_GET_PERMISSIONS = 3;
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
                if (ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, PICK_IMAGE);
                } else {
                    requestMultiplePermissions();
                }
            }
        });

        process = (Button) findViewById(R.id.button2);
        process.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {
                    finalImg = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));
                    firstImage = new CVLeaf();
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
            case PICK_IMAGE:
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
                break;
            case DIDNT_GET_PERMISSIONS:
                requestApplicationConfig();
                break;
        }
    }


    /**
     * Запрос разрешений от пользователя
     */
    public void requestMultiplePermissions() {

        ActivityCompat.requestPermissions(this,
                new String[] {
                        READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                PERMISSION_REQUEST_CODE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == (PERMISSION_REQUEST_CODE) && grantResults.length == 2) {
            if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                System.out.println("permission have got");
            }else{
                showPermissionDialog(MainActivity.this);
            }

        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
            Toast toast = Toast.makeText(MainActivity.this, "Database loading...", Toast.LENGTH_SHORT);
            toast.show();
            bar.setVisibility(View.VISIBLE);
            pickImage.setVisibility(View.GONE);
            process.setVisibility(View.GONE);

        }

        @Override
        protected void onPostExecute(Void result){
            bar.setVisibility(View.GONE);
            pickImage.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
            Toast toast = Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT);
            toast.show();
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
                secondImage = dataBaseHelper.getLeaf(i);
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



                if (bestMatch < goodEnough.rows()){
                    bestMatch = goodEnough.rows();
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
                InputStream is = assets.open(dataBaseHelper.getLeaf(index).getPath());
                imageView.setImageBitmap(BitmapFactory.decodeStream(is));
                textView.setText("This is "+ dataBaseHelper.getLeaf(index).getName());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    private void showPermissionDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String title = getResources().getString(R.string.app_name);
        builder.setTitle(title);
        builder.setMessage(title + " need permissions!");

        String positiveText = "Settings";
        builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                openAppSettings();
            }
        });

        String negativeText = "Quit";
        builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, DIDNT_GET_PERMISSIONS);
    }

    private boolean isPermissionGranted(String permission) {
        // проверяем разрешение - есть ли оно у нашего приложения
        int permissionCheck = ActivityCompat.checkSelfPermission(this, permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }


    private void requestApplicationConfig() {
        if ((isPermissionGranted(READ_EXTERNAL_STORAGE)) && (isPermissionGranted(CAMERA))) {
            Toast.makeText(MainActivity.this, "Good job, comrade!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "Don't be so shy!", Toast.LENGTH_LONG).show();
            requestMultiplePermissions();
        }
    }

}
