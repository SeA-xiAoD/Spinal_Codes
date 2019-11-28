/**
 * Created by SeAxiAoD on 2019/11/12.
 * Random Number Generator
 */

import java.util.Random;

public class RNG {

    private int seed;
    private int scope;
    private Random rng;

    /**
     * Random number generator.
     *
     * @param seed  The seed of RNG.
     * @param c The length of symbol of each pass. (c need to less than 32)
     *
     */
    public RNG (int seed, int c) {
        if(c >= 32){
            this.scope = Integer.MAX_VALUE;
        }
        else {
            this.scope = 1 << c;
        }
        this.seed = seed;
        this.rng = new Random(this.seed);
    }

    /**
     * @return a 32-bit value, the last c bits contains the random number.
     */
    public int next() {
        return this.rng.nextInt(scope);
    }

    /**
     * Getters and setters.
     */
    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public Random getRng() {
        return rng;
    }

    public void setRng(Random rng) {
        this.rng = rng;
    }
}
