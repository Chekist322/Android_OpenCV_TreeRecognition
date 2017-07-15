package test.cvtest;

import java.util.ArrayList;

/**
 * Created by Phoen on 14.07.2017.
 */

public class CVTree {
    ArrayList<CVLeaf> leaves;
    String name;
    int averageNumberOfGoodMatch;

    public void addLeaf(CVLeaf leaf){
        leaves.add(leaf);
    }

    public void countAverage(){
        for (int i = 0; i < leaves.size(); i++) {
            averageNumberOfGoodMatch += leaves.get(i).getNumberOfGoodMatches();
        }
        averageNumberOfGoodMatch = averageNumberOfGoodMatch/leaves.size();
    }
}
