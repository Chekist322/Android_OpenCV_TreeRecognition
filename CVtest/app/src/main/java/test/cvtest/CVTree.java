package test.cvtest;

import java.util.ArrayList;

/**
 * Created by Phoen on 14.07.2017.
 */

public class CVTree {
    String name;
    int averageNumberOfGoodMatch;
    double averageDistance;
    private ArrayList<CVLeaf> leaves;

    CVTree(String str) {
        name = str;
        leaves = new ArrayList<>();
    }

    public void addLeaf(CVLeaf leaf) {
        leaves.add(leaf);
    }

    public void countAverage() {
        averageNumberOfGoodMatch = 0;
        averageDistance = 0;
        int count = leaves.size();
        for (int i = 0; i < leaves.size(); i++) {
            averageNumberOfGoodMatch += leaves.get(i).getNumberOfGoodMatches();
            averageDistance += leaves.get(i).averageDistance;

        }
        averageNumberOfGoodMatch = averageNumberOfGoodMatch / leaves.size();
        averageDistance = averageDistance / count;
    }

    public String getName() {
        return name;
    }

    public void setName(String str) {
        name = str;
    }

    public int getSize() {
        return leaves.size();
    }

    public CVLeaf getLeafByIndex(int index) {
        return leaves.get(index);
    }
}
