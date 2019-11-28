/**
 * Created by SeAxiAoD on 2019/11/25.
 */

import java.util.ArrayList;
import java.util.Comparator;

public class MultiTreeNode {

    private int message_value;
    private int spine_value;
    private RNG rng;
    private double cost;

    private MultiTreeNode parent;
    private ArrayList<MultiTreeNode> child;

    /**
     * Multi-tree node.
     *
     * @param message_value Integer array of message values.
     * @param spine_value Integer array of spine values.
     * @param rng Random number generator.
     *
     */
    public MultiTreeNode(int message_value, int spine_value, RNG rng) {
        this.message_value = message_value;
        this.spine_value = spine_value;
        this.rng = rng;
    }

    /**
     * @return A 32-bit value, the last c bits contains the random number.
     */
    public int getNextRNGValue() {
        return this.rng.next();
    }

    /**
     * Getters and setters.
     */
    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int getMessage_value() {
        return message_value;
    }

    public void setMessage_value(int message_value) {
        this.message_value = message_value;
    }

    public int getSpine_value() {
        return spine_value;
    }

    public void setSpine_value(int spine_value) {
        this.spine_value = spine_value;
    }

    public MultiTreeNode getParent() {
        return parent;
    }

    public void setParent(MultiTreeNode parent) {
        this.parent = parent;
    }

    public ArrayList<MultiTreeNode> getChild() {
        return child;
    }

    public void setChild(ArrayList<MultiTreeNode> child) {
        this.child = child;
    }
}
