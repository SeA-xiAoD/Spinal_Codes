import java.util.Comparator;

/**
 * Created by SeAxiAoD on 2019/11/27.
 */

public class Candidate {


    private MultiTreeNode T;
    private double path_cost;

    public Candidate(MultiTreeNode t, double path_cost) {
        T = t;
        this.path_cost = path_cost;
    }

    public MultiTreeNode getT() {
        return T;
    }

    public void setT(MultiTreeNode t) {
        T = t;
    }

    public double getPath_cost() {
        return path_cost;
    }

    public void setPath_cost(double path_cost) {
        this.path_cost = path_cost;
    }


}
