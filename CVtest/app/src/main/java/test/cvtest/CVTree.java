package test.cvtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

/**
 * Created by Phoen on 09.07.2017.
 */

public class CVTree {
    MatOfKeyPoint keyPoint;
    FeatureDetector detector;
    DescriptorExtractor dExtractor;
    Mat descriptors;
    Mat image;
    String name;

//    public void setImageByPath(String pathName){
//        image = BitmapFactory.decodeFile(pathName);
//        detectAndDescript();
//    }

    public void setName(String string){
        name = string;
    }

    public void setImageByMat(Mat mat){
        image = mat;
        detectAndDescript();
    }



    private void detectAndDescript(){
        keyPoint = new MatOfKeyPoint();

        detector = FeatureDetector.create(FeatureDetector.ORB);
        detector.detect(image, keyPoint);

        dExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
        descriptors = new Mat();
        dExtractor.compute(image, keyPoint, descriptors);
        System.out.println(descriptors.size());
    }
}
