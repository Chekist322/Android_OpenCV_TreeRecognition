package test.cvtest;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoen on 09.07.2017.
 */

public class DataBaseHelper {
    private List<CVTree> treeList;
//    private List<CVLeaf> leafList;

    DataBaseHelper() {
//        leafList = new ArrayList<>();
        treeList = new ArrayList<>();
    }


    private void add(CVTree tree) {
        treeList.add(tree);
    }

    public void fillList(Activity current) {
        try {
            AssetManager assets = current.getAssets();
            InputStream is;
            String[] folders = assets.list(""); // массив имен файлов
            String outCursor;
            String inCursor;

            for (int i = 0; i < folders.length; i++) {
                outCursor = folders[i];
                if (folders[i].matches(".+[А-Я].+")) {
                    CVTree cvTree = new CVTree(outCursor);
                    String[] images = assets.list(outCursor);
                    for (int j = 0; j < images.length; j++) {
                        if (images[j].matches(".+jpg")) {
                            inCursor = images[j];
                            is = assets.open(outCursor + "/" + inCursor);
                            CVLeaf cvLeaf = new CVLeaf(outCursor + "/" + inCursor);
                            cvLeaf.setName(outCursor);
                            System.out.println(outCursor + " / " + inCursor);
                            cvLeaf.setImageByBitmap(BitmapFactory.decodeStream(is));
                            cvTree.addLeaf(cvLeaf);
                        }
                    }

                    this.add(cvTree);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public CVTree getTreeByIndex(int index) {
        return treeList.get(index);
    }

    public int getSize() {
        return treeList.size();
    }
}
