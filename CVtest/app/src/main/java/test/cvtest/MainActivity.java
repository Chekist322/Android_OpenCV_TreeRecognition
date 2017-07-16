package test.cvtest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
import org.opencv.features2d.DescriptorMatcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    final static DataBaseHelper dataBaseHelper = new DataBaseHelper();
    private final int PICK_IMAGE = 1;
    private final int DIDNT_GET_PERMISSIONS = 3;
    private final int CAMERA_RESULT = 4;
    String FILE_NAME;
    Bitmap selectedImage;
    CVLeaf firstImage;
    CVLeaf secondImage;
    CVTree currentTree;
    Mat finalImg;
    ProgressBar bar;
    Uri imageUri;
    private ImageView imageView;
    private TextView textView;
    private double bestMatch = 200;
    private int index = -1;
    private Button process;
    private Button pickImage;
    private Button takePicture;
    private int PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            String string = savedInstanceState.getString("key");
        }

        FILE_NAME = getFilesDir().getAbsolutePath() + "/kek.jpg";
        System.out.println(FILE_NAME);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) findViewById(R.id.verify_progress);
        bar.setVisibility(View.INVISIBLE);
        textView = (TextView) findViewById(R.id.textView);
        textView.setVisibility(View.INVISIBLE);
        imageView = (ImageView) findViewById(R.id.imageView);
        pickImage = (Button) findViewById(R.id.galeryButton);

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

        takePicture = (Button) findViewById(R.id.cameraButton);
        takePicture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_RESULT);
                } else {
                    requestMultiplePermissions();
                }
            }
        });

        DataBaseLoad dataBaseLoad = new DataBaseLoad();
        dataBaseLoad.execute();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {

        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == RESULT_OK) {
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
            case CAMERA_RESULT:
                selectedImage = (Bitmap) imageReturnedIntent.getExtras().get("data");
                imageView.setImageBitmap(selectedImage);
                break;
        }
    }


    /**
     * Запрос разрешений от пользователя
     */
    public void requestMultiplePermissions() {

        ActivityCompat.requestPermissions(this,
                new String[]{
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
            } else {
                showPermissionDialog(MainActivity.this);
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    /**
     * Класс поток для загрузки БД
     */
    private class DataBaseLoad extends AsyncTask<Void, Void, Void> {

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
            takePicture.setVisibility(View.GONE);
            pickImage.setVisibility(View.GONE);
            process.setVisibility(View.GONE);

        }

        @Override
        protected void onPostExecute(Void result) {
            bar.setVisibility(View.GONE);
            takePicture.setVisibility(View.VISIBLE);
            pickImage.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
            Toast toast = Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT);
            toast.show();


        }
    }

    /**
     * Класс поток для сравнения деревьев
     */
    private class Comparison extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            for (int i = 0; i < dataBaseHelper.getSize(); i++) {

                currentTree = dataBaseHelper.getTreeByIndex(i);

                for (int j = 0; j < currentTree.getSize(); j++) {
                    secondImage = currentTree.getLeafByIndex(j);
                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                    MatOfDMatch matches = new MatOfDMatch();
                    matcher.match(secondImage.descriptors, firstImage.descriptors, matches);


                    List<DMatch> allMatches = matches.toList();


                    for (int k = 0; k < secondImage.descriptors.rows(); k++) {

                        secondImage.averageDistance += allMatches.get(k).distance;

                    }
                    secondImage.averageDistance /= allMatches.size();
                    System.out.println(allMatches.size());


                    System.out.print(dataBaseHelper.getTreeByIndex(i).getLeafByIndex(j).averageDistance);
                    System.out.print("/");
                    System.out.println(dataBaseHelper.getTreeByIndex(i).getLeafByIndex(j).getName());

                }
                currentTree.countAverage();

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar.setVisibility(View.VISIBLE);
            takePicture.setVisibility(View.GONE);
            pickImage.setVisibility(View.GONE);
            process.setVisibility(View.GONE);

        }

        @Override
        protected void onPostExecute(Void result) {
            bar.setVisibility(View.GONE);
            takePicture.setVisibility(View.VISIBLE);
            pickImage.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);

            AssetManager assets = MainActivity.this.getAssets();
            try {
                for (int i = 0; i < dataBaseHelper.getSize(); i++) {
                    if (bestMatch > dataBaseHelper.getTreeByIndex(i).averageDistance) {
                        bestMatch = dataBaseHelper.getTreeByIndex(i).averageDistance;
                        index = i;
                        System.out.println(bestMatch);
                    }
                }
                if (index == -1) {
                    textView.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Take another picture.", Toast.LENGTH_LONG).show();
                } else {
                    textView.setVisibility(View.VISIBLE);
                    InputStream is = assets.open(dataBaseHelper.getTreeByIndex(index).getLeafByIndex(0).getPath());
                    imageView.setImageBitmap(BitmapFactory.decodeStream(is));
                    textView.setText("This is " + dataBaseHelper.getTreeByIndex(index).getName());
//                    System.out.println(bestMatch);
                    bestMatch = 200;
                    index = -1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("key", "value");
    }

}
