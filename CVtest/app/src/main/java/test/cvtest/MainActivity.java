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
    Bitmap selectedImage;
    Bitmap secondSelectedImage;
    Mat matSelectedImage;
    Mat secondMatSelectedImage;
    Mat finalImg;
    ProgressBar bar;
    private final int Pick_image = 1;
    private final int Pick_second_image = 2;
    Uri imageUri;
    private Button process;
    private int PERMISSION_REQUEST_CODE = 2;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.out.println(FILENAME);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) findViewById(R.id.verify_progress);
        bar.setVisibility(View.INVISIBLE);
        imageView = (ImageView) findViewById(R.id.imageView);
        final Button PickImage = (Button) findViewById(R.id.button);


        PickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestMultiplePermissions();
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, Pick_image);
                    startActivityForResult(photoPickerIntent, Pick_second_image);
                }
            }
        });


        try {
            AssetManager am = MainActivity.this.getAssets();
            InputStream stream =  am.open("kek.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }



        process = (Button) findViewById(R.id.button2);
        process.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedImage != null) {

                    selectedImage = scaleBitmap(selectedImage);
                    secondSelectedImage = scaleBitmap(secondSelectedImage);
                    finalImg = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));
                    matSelectedImage = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));
                    secondMatSelectedImage = new Mat(secondSelectedImage.getHeight(), secondSelectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));


                    Utils.bitmapToMat(selectedImage, matSelectedImage);
                    Imgproc.cvtColor(matSelectedImage, matSelectedImage, Imgproc.COLOR_RGB2GRAY);


                    Utils.bitmapToMat(secondSelectedImage, secondMatSelectedImage);
                    Imgproc.cvtColor(secondMatSelectedImage, secondMatSelectedImage, Imgproc.COLOR_RGB2GRAY);





                    /**
                     * Прогнать 2 image через CVTree и сравнить
                     */
                    int maximumNumberOfMatches = 40;

                    CVTree firstImage = new CVTree();
                    CVTree secondImage = new CVTree();

                    firstImage.setImageByMat(matSelectedImage);
                    secondImage.setImageByMat(secondMatSelectedImage);


                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                    MatOfDMatch matches = new MatOfDMatch();
                    matcher.match(secondImage.descriptors, firstImage.descriptors, matches);

                    ArrayList<DMatch> goodMatches = new ArrayList<DMatch>();
                    List<DMatch> allMatches = matches.toList();

                    double minDist = 50;
                    for (int i = 0; i < secondImage.descriptors.rows(); i++) {
                        double dist = allMatches.get(i).distance;
                        if (dist < minDist) minDist = dist;
                    }
                    for (int i = 0; i < secondImage.descriptors.rows() && goodMatches.size() < maximumNumberOfMatches; i++) {
                        if (allMatches.get(i).distance <= 2 * minDist) {
                            goodMatches.add(allMatches.get(i));
                        }
                    }
                    MatOfDMatch goodEnough = new MatOfDMatch();
                    goodEnough.fromList(goodMatches);

                    Features2d.drawMatches(secondMatSelectedImage, secondImage.keyPoint, matSelectedImage, firstImage.keyPoint, goodEnough, finalImg, Scalar.all(-1), Scalar.all(-1), new MatOfByte(), Features2d.DRAW_RICH_KEYPOINTS + Features2d.NOT_DRAW_SINGLE_POINTS);

                    Imgcodecs.imwrite(FILENAME, finalImg);
                    finalImg = Imgcodecs.imread(FILENAME);

                    TextView num = (TextView) findViewById(R.id.allMatches);
                    num.setText(String.valueOf(allMatches.size()));
                    num = (TextView) findViewById(R.id.goodMatches);
                    num.setText(goodEnough.size().toString());
                    NewThread thread = new NewThread();

                    thread.execute();
                }
            }
        });



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
                } break;
            case Pick_second_image:
                if(resultCode == RESULT_OK) {
                    try {
                        imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        secondSelectedImage = BitmapFactory.decodeStream(imageStream);
                        imageView.setImageBitmap(secondSelectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;

        }
    }



    private Bitmap scaleBitmap(Bitmap image){
        int MAX_DIM = 600;
        int width;
        int height;
        Bitmap scaledImage;
        if (image.getWidth() >= image.getHeight()) {
            width = MAX_DIM;
            height = image.getHeight() * MAX_DIM / image.getWidth();
        } else {
            height = MAX_DIM;
            width = image.getWidth() * MAX_DIM / image.getHeight();
        }
        scaledImage = Bitmap.createScaledBitmap(image, width, height, false);
        return scaledImage;
    }

    public void requestMultiplePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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

    class NewThread extends AsyncTask<Void, Integer, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = null;
            bitmap = BitmapFactory.decodeFile(FILENAME);

            return bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            bar.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onPostExecute(Bitmap result){
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
            bar.setVisibility(View.GONE);
        }


    }





}
