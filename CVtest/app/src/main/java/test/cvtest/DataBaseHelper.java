package test.cvtest;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoen on 09.07.2017.
 */

public class DataBaseHelper{
    private List<CVTree> treeList;

    DataBaseHelper(){
        treeList = new ArrayList<>();
    }


    private void add(CVTree tree){
        treeList.add(tree);
    }

    public void fillList(Activity current){
        try {
            AssetManager assets = current.getAssets();
            InputStream is;
            String[] folders = assets.list(""); // массив имен файлов
            String outCursor;
            String inCursor;

            for (int i = 0; i < folders.length-3; i++) {
                outCursor = folders[i];
                String[] images = assets.list(outCursor);
                for (int j = 0; j < images.length; j++) {
                    inCursor = images[j];
                    is = assets.open(outCursor + "/" + inCursor);
                    CVTree cvTree = new CVTree(outCursor + "/" + inCursor);
                    cvTree.setName(outCursor);
                    System.out.println(outCursor+" / "+inCursor);
                    cvTree.setImageByBitmap(BitmapFactory.decodeStream(is));
                    this.add(cvTree);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public CVTree get(int index){
        return treeList.get(index);
    }

    public int getSize(){
        return treeList.size();
    }
}
