package test.cvtest;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by Phoen on 09.07.2017.
 */

public class CVLeaf {
    int numberOfGoodMatches;
    MatOfKeyPoint keyPoint;
    Mat descriptors;
    private String path;
    private FeatureDetector detector;
    private DescriptorExtractor dExtractor;
    private Mat image;
    private String name;

//    public void setImageByPath(String pathName){
//        image = BitmapFactory.decodeFile(pathName);
//        detectAndDescript();
//    }

    CVLeaf(){
        keyPoint = null;
        descriptors = null;
        detector = null;
        dExtractor = null;
        image = null;
        name = null;
    }

    CVLeaf(String _path){
        path = _path;
    }


    public void setName(String string){
        name = string;
    }

    public void setImageByBitmap(Bitmap selectedImage){
        selectedImage = scaleBitmap(selectedImage);
        image = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Utils.bitmapToMat(selectedImage, image);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        detectAndDescript();
    }



    private void detectAndDescript(){
        keyPoint = new MatOfKeyPoint();
        detector = FeatureDetector.create(FeatureDetector.ORB);
        detector.detect(image, keyPoint);

        dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
        descriptors = new Mat();
        dExtractor.compute(image, keyPoint, descriptors);
    }

    static Bitmap scaleBitmap(Bitmap image){
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

    public Mat getImage(){
        return image;
    }
    public String getName(){
        return name;
    }
    public String getPath(){
        return path;
    }
    public int getNumberOfGoodMatches(){return numberOfGoodMatches;}
}
