/**
 * Created by SeAxiAoD on 2019/11/26.
 */

import com.sun.org.apache.xpath.internal.operations.Mult;

import java.util.ArrayList;
import java.lang.Math;

public class SpinalDecoder {

    private int k, v, c, l, B, d;
    private int v_mask;
    private JenkinsHash hash_function;

    /**
     * Spinal codes decoder.
     *
     * @param k The number of bits for each message piece m. (k need to less than 32)
     * @param v The number of bits for each spine value s. (v need to less than 32)
     * @param c The number of bits for each transmitted symbol x_i,j. (c need to less than 32)
     * @param l The number of passes.
     * @param B The number of beam.
     * @param d The depth of sub-tree using in decoding.
     *
     */
    public SpinalDecoder(int k, int v, int c, int l, int B, int d) {
        this.k = k;
        this.v = v;
        this.c = c;
        this.l = l;
        this.B = B;
        this.d = d;
        this.hash_function = new JenkinsHash();

        // build mask of v
        if(v == 32) {
            this.v_mask = 0xffffffff;
        }
        else {
            for (int i = 0; i < v; i++) {
                this.v_mask |= 1 << i;
            }
        }
    }

    /**
     * Decode symbols.
     *
     * @param symbols Array of encoded bytes.
     *
     * @return String of message for encoding.
     *
     */
    public byte[] decode(byte[] symbols) {

        // convert symbols to array of integers
        int[] symbols_int = this.divideSymbols2int(symbols);

        /************************ Step 1: build root of tree ***********************/

        // note: the parameters of root node are useless
        MultiTreeNode root = new MultiTreeNode(0, 0, new RNG(0,0));
        root.setSpine_value(0);
        root.setCost(0);

        // build child for root
        root.setChild(this.buildChild(root, symbols_int, 0));

        // expand root to depth d
        ArrayList<MultiTreeNode> temp_leaves = root.getChild(); // leaves of current tree
        ArrayList<MultiTreeNode> next_layer_leaves = new ArrayList<MultiTreeNode>(); // leaves of next layer
        for (int i = 1; i < this.d; i++) {
            for (MultiTreeNode temp_node : temp_leaves) {
                ArrayList<MultiTreeNode> temp_child = buildChild(temp_node, symbols_int, i);
                temp_node.setChild(temp_child);
                next_layer_leaves.addAll(temp_child);
            }
            temp_leaves = next_layer_leaves;
            next_layer_leaves = new ArrayList<MultiTreeNode>();
        }

        /************************ Step 2: build pruning tree ***********************/

        ArrayList<MultiTreeNode> beam = new ArrayList<MultiTreeNode>();
        ArrayList<Candidate> candidate_list = new ArrayList<Candidate>();
        ArrayList<MultiTreeNode> subtree_of_T = new ArrayList<MultiTreeNode>();
        ArrayList<MultiTreeNode> current_leaves = new ArrayList<MultiTreeNode>();
        ArrayList<MultiTreeNode> new_leaves = new ArrayList<MultiTreeNode>();

        // set of rooted tree
        beam.add(root);

        for (int i = 1; i < symbols_int.length / this.l - this.d + 1; i++) {
            candidate_list = new ArrayList<Candidate>();
            for (MultiTreeNode T : beam) {

                // get subtrees(root(T))
                subtree_of_T = T.getChild();
                if (subtree_of_T == null) {
                    subtree_of_T = new ArrayList<MultiTreeNode>();
                    subtree_of_T.add(T);
                }

                for (MultiTreeNode T_apostrophe : subtree_of_T) {

                    // Expand T' from depth d-1 to depth d
                    new_leaves = new ArrayList<MultiTreeNode>();
                    current_leaves = this.getLeaves(T_apostrophe, this.d - 1);
                    for (MultiTreeNode a_leave_node : current_leaves) {
                        temp_leaves = this.buildChild(a_leave_node, symbols_int, i + this.d - 1);
                        a_leave_node.setChild(temp_leaves);
                        new_leaves.addAll(temp_leaves);
                    }

                    // compute and store path_cost in expanded nodes
                    double T_apostrophe_cost = this.selectMinCost(new_leaves);
                    candidate_list.add(new Candidate(T_apostrophe, T_apostrophe_cost));
                }

            }
            // get B lowest cost candidates
            beam = this.getBeam(candidate_list, this.B);
        }

        // get the best T_apostrophe
        new_leaves = new ArrayList<MultiTreeNode>();
        double lowest_path_cost = Double.MAX_VALUE;
        MultiTreeNode best_leaf = root;
        for (MultiTreeNode best_candidate : beam) {
            current_leaves = this.getLeaves(best_candidate, this.d);
            for (MultiTreeNode a_leaf : current_leaves) {
                if (a_leaf.getCost() < lowest_path_cost) {
                    best_leaf = a_leaf;
                    lowest_path_cost = a_leaf.getCost();
                }
            }
        }

        /************************ Step 3: build decoded string ***********************/
        byte[] decoded_message = new byte[symbols.length / this.l * this.k / this.c];
        int pointer = 7; // record the position in each bytes => e.g. [0000000p] => pointer = 7
        int count = symbols.length / this.l * this.k / this.c - 1; // record the position in array of integers
        while (best_leaf != root) {
            for (int i = 0; i < this.k; i++) {
                decoded_message[count] |= ((best_leaf.getMessage_value() & (1 << i)) >> i) == 1 ? (1 << (7-pointer)) : 0;
                pointer--;
                if (pointer == -1) {
                    pointer = 7;
                    count--;
                }
            }
            best_leaf = best_leaf.getParent();
        }

        return decoded_message;
    }

    /**
     * Use array of integer to store symbols.
     *
     * @param parent The parent node.
     * @param symbols_int Symbols of integer format.
     * @param parent_depth The depth of parent in the whole pruning tree.
     *
     * @return Array of MultiTreeNode.
     */
    private ArrayList<MultiTreeNode> buildChild(MultiTreeNode parent, int[] symbols_int, int parent_depth) {
        ArrayList<MultiTreeNode> temp_child = new ArrayList<MultiTreeNode>();
        for (int i = 0; i < (1 << this.k); i++) {

            // build a temp node
            byte[] temp_bytes_for_hash = this.twoIntToByte(parent.getSpine_value(), i);
            int temp_spine_value = this.hash_function.hash32(temp_bytes_for_hash) & this.v_mask;
            RNG temp_RNG = new RNG(temp_spine_value, this.c);
            MultiTreeNode temp_node = new MultiTreeNode(i, temp_spine_value, temp_RNG);
            temp_node.setParent(parent);

            // calculate path cost
            double temp_loss = 0;
            for (int j = 0; j < this.l; j++) {
                int rng_generated_symbol = temp_node.getNextRNGValue();
                temp_loss += Math.pow(symbols_int[j * symbols_int.length / this.l + parent_depth] - rng_generated_symbol, 2);
            }
            temp_node.setCost(parent.getCost() + temp_loss / this.l);

            // add to temp child array
            temp_child.add(temp_node);
        }
        return temp_child;
    }

    /**
     * Use array of integer to store symbols.
     *
     * @param symbols Array of encoded bytes.
     *
     * @return Array of int.
     *
     * If c = 6, the input symbols would be shown as:
     *                          [0]0 1 2 3 4 5 | 6 7
     *                          [1]8 9 10 11 | 12 13 14 15
     *                          [2]16 17 | 18 19 20 21 22 23
     * Convert to array of ints:
     *                          [0]0...0 0 1 2 3 4 5  (32-bits)
     *                          [1]0...0 6 7 8 9 10 11  (32-bits)
     *                          [2]0...0 12 13 14 15 16 17  (32-bits)
     *                          ...
     */
    private int[] divideSymbols2int(byte[] symbols) {
        int[] symbols_int = new int[symbols.length * 8 / this.c];
        int pointer = 0; // record the position in each bytes => e.g. [p0000000] => pointer = 0
        int count = 0; // record the position in array of integers
        for (int i = 0; i < symbols_int.length; i++) {
            for (int j = 0; j < this.c; j++) {
                symbols_int[i] |= ((symbols[count] & (1 << (8-pointer-1))) >> (8-pointer-1)) == 1 ? (1<<(this.c-j-1)) : 0;
                ++pointer;
                if(pointer == 8) {
                    pointer = 0;
                    ++count;
                }
            }
        }
        return symbols_int;
    }

    /**
     * Convert two integers to bytes for hash encoding.
     *
     * @param val1 Integer value 1.
     * @param val2 Integer value 2.
     *
     * @return Array of int.
     */
    private byte[] twoIntToByte(int val1, int val2) {
        byte[] b = new byte[8];
        b[0] = (byte)(val1 & 0xff);
        b[1] = (byte)((val1 >> 8) & 0xff);
        b[2] = (byte)((val1 >> 16) & 0xff);
        b[3] = (byte)((val1 >> 24) & 0xff);
        b[4] = (byte)(val2 & 0xff);
        b[5] = (byte)((val2 >> 8) & 0xff);
        b[6] = (byte)((val2 >> 16) & 0xff);
        b[7] = (byte)((val2 >> 24) & 0xff);
        return b;
    }

    /**
     * Get all leaves of node T in specific depth.
     *
     * @param T The node for search.
     * @param depth The depth of leaves.
     *
     * @return Minimum cost of leaves
     */
    private ArrayList<MultiTreeNode> getLeaves(MultiTreeNode T, int depth) {
        ArrayList<MultiTreeNode> new_leaves = new ArrayList<MultiTreeNode>();
        ArrayList<MultiTreeNode> temp_leaves = T.getChild();
        if (depth == 0) {
            temp_leaves = new ArrayList<MultiTreeNode>();
            temp_leaves.add(T);
        }
        else {
            for (int i = 0; i < depth - 1; i++) {
                for (MultiTreeNode node : temp_leaves) {
                    new_leaves.addAll(node.getChild());
                }
                temp_leaves = new_leaves;
                new_leaves = new ArrayList<MultiTreeNode>();
            }
        }
        return temp_leaves;
    }

    /**
     * Convert two integers to bytes for hash encoding.
     *
     * @param leaves Leaves of tree T'.
     *
     * @return Minimum cost of leaves
     */
    private double selectMinCost(ArrayList<MultiTreeNode> leaves) {
        double temp_min = Double.MAX_VALUE;
        MultiTreeNode temp;
        for (MultiTreeNode node : leaves) {
            if (node.getCost() < temp_min) {
                temp_min = node.getCost();
                temp = node;
            }
        }
        return temp_min;
    }

    /**
     * Get B lowest cost candidates.
     *
     * @param candidate_list List of candidates (MultiTreeNode, path_cost).
     * @param B The number of beam.
     *
     * @return B lowest cost candidates.
     */
    private ArrayList<MultiTreeNode> getBeam(ArrayList<Candidate> candidate_list, int B) {
        candidate_list.sort(new CandidateComparator());
        ArrayList<MultiTreeNode> temp_beam = new ArrayList<MultiTreeNode>();
        if(candidate_list.size() < B) {
            for (Candidate a_candidate : candidate_list) {
                temp_beam.add(a_candidate.getT());
            }
        }
        else {
            for (int i = 0; i < B; i++) {
                temp_beam.add(candidate_list.get(i).getT());
            }
        }
        return temp_beam;
    }
}
