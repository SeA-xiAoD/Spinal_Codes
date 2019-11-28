import java.util.Comparator;

/**
 * Created by SeAxiAoD on 2019/11/27.
 */
public class CandidateComparator implements Comparator<Candidate> {

    @Override
    public int compare(Candidate o1, Candidate o2) {
        if (o1.getPath_cost() > o2.getPath_cost()) {
            return 1;
        }
        else if (o1.getPath_cost() < o2.getPath_cost()) {
            return -1;
        }
        else return 0;
    }
}
