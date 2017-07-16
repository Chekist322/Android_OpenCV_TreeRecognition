package test.cvtest;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Phoen on 09.07.2017.
 */

public class CVLeaf {
    private static FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
    private static DescriptorExtractor dExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    int numberOfGoodMatches;
    MatOfKeyPoint keyPoint;
    double averageDistance = 0;
    //    double averageValueOfGoodMatches;
    Mat descriptors;
    private String path;
    private Mat image;
    private Bitmap bitmap;

    private String name;


    CVLeaf() {
        keyPoint = null;
        descriptors = null;
        image = null;
        name = null;
    }

    CVLeaf(String _path) {
        path = _path;
    }

    static Bitmap scaleBitmap(Bitmap image) {
        int MAX_DIM = 200;
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

    public void setImageByBitmap(Bitmap selectedImage) {

        selectedImage = scaleBitmap(selectedImage);
        image = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Utils.bitmapToMat(selectedImage, image);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        detectAndDescript();
    }


    private void detectAndDescript() {
        keyPoint = new MatOfKeyPoint();
//        detector = FeatureDetector.create(FeatureDetector.ORB);
        detector.detect(image, keyPoint);

//        dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
        descriptors = new Mat();
        dExtractor.compute(image, keyPoint, descriptors);
    }

    public Mat getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public void setName(String string) {
        name = string;
    }

    public String getPath() {
        return path;
    }

    public int getNumberOfGoodMatches() {
        return numberOfGoodMatches;
    }

    public void setNumberOfGoodMatches(int tmp) {
        numberOfGoodMatches = tmp;
    }
}
