/**
 * Created by SeAxiAoD on 2019/11/13.
 */

public class SpinalEncoder {

    private int k, v, c, l;
    private RNG rng_list[];
    private static int s_0 = 0;
    private int v_mask = 0;
    private JenkinsHash hash_function;

    /**
     * Spinal codes encoder.
     *
     * @param k The number of bits for each message piece m. (k need to less than 32)
     * @param v The number of bits for each spine value s. (v need to less than 32)
     * @param c The number of bits for each transmitted symbol x_i,j. (c need to less than 32)
     * @param l The number of passes.
     *
     */
    public SpinalEncoder(int k, int v, int c, int l) {
        this.k = k;
        this.v = v;
        this.c = c;
        this.l = l;
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
     * Generate encoded codes.
     *
     * @param message_byte Message_byte for encoding. (message_byte.length/k should be an integer)
     *
     * @return Array of encoded bytes.
     *
     * For example, if c = 6, the encoded bytes might show as follow,
     * symbols after encoding: 011001 100110 010010 101001 ...
     * output in array of bytes: [0]011001 10  (8-bits)
     *                           [1]0110 0100  (8-bits)
     *                           [2]10 101001  (8-bits)
     *                           ...
     */
    public byte[] encode(byte[] message_byte) {

        // judge whether len(M)*l*c/k is an integer, and add '\0'.
        String M = new String(message_byte);
        int message_length_reminder = M.length() % this.k;
        if(message_length_reminder != 0) {
            for (int i = 0; i < (this.k - message_length_reminder); i++) {
                M += '\0';
            }
            System.out.printf("\nAdded %d \'\\0\' for encoding.\n\n", this.k - message_length_reminder);
        }

        // divide message by k-bits and use array of int to store the results
        int[] divided_messages = this.divideMessage(M.getBytes());

        // build spine values and RNGs
        int[] spine_values = new int[divided_messages.length];
        RNG[] rng_list = this.buildSpineValuesAndRNGs(s_0, spine_values, divided_messages);

        // generate symbols
        int[] temp_symbols = new int[this.l * rng_list.length];
        for (int i = 0; i < this.l; i++) {
            for (int j = 0; j < rng_list.length; j++) {
                temp_symbols[i * rng_list.length + j] = rng_list[j].next();
            }
        }

        // convert temp symbols to bytes
        return this.tempSymbols2Symbols(temp_symbols);
    }

    /**
     * Generate encoded codes.
     *
     * @param message_bytes Message bytes for division.
     *
     * @return Array of int.
     *
     * At first, the string would be converted into 8-bits bytes, shown as:
     * (utilized sequence number to denote binary value 0 or 1 for convenient understanding)
     *                          [0]0 1 2 3 4 5 6 7
     *                          [1]8 9 10 11 12 13 14 15
     *                          [2]16 17 18 19 ...
     * For example, if k = 6, the divided ints might show as follow,
     * output in array of ints: [0]0...0 0 1 2 3 4 5  (32-bits)
     *                          [1]0...0 6 7 8 9 10 11  (32-bits)
     *                          [2]0...0 12 13 14 15 16 17  (32-bits)
     *                          ...
     */
    private int[] divideMessage(byte[] message_bytes) {

        // initialize divided_messages
        int[] divided_messages = new int[message_bytes.length * 8 / this.k];
        for (int i = 0; i < message_bytes.length * 8 / this.k; i++) {
            divided_messages[i] = 0;
        }

        // division procedure
        int pointer = 0; // record the position in each bytes => e.g. [p 0 0 0] => pointer = 0
        int count = 0; // record the position in bytes array of M
        for (int i = 0; i < message_bytes.length * 8 / this.k; i++) {
            for (int j = 0; j < this.k; j++) {
                divided_messages[i] |= ((message_bytes[count] & (1<< (8-pointer-1))) >> (8-pointer-1)) == 1 ? (1<<(this.k-j-1)) : 0;
                ++pointer;
                if(pointer == 8){
                    ++count;
                    pointer = 0;
                }
            }
        }
        return divided_messages;
    }

    /**
     * Convert two integers to bytes for hash encoding.
     *
     * @param s_0 Spine value 0.
     * @param spine_values Integer array of spine values.
     * @param divided_messages Integer array of divided_messages.
     *
     * @return Array of int.
     */
    private RNG[] buildSpineValuesAndRNGs(int s_0, int[] spine_values, int[] divided_messages) {
        RNG[] RNGs = new RNG[divided_messages.length];

        // built spine value 1 and RNG 1
        byte[] temp_bytes_for_hash = this.twoIntToByte(s_0, divided_messages[0]);
        spine_values[0] = this.hash_function.hash32(temp_bytes_for_hash) & this.v_mask;
        RNGs[0] = new RNG(spine_values[0], this.c);

        // built spine value 2 -> divided_messages.length and RNG 2 -> divided_messages.length
        for (int i = 1; i < spine_values.length; i++) {
            temp_bytes_for_hash = this.twoIntToByte(spine_values[i-1], divided_messages[i]);
            spine_values[i] = this.hash_function.hash32(temp_bytes_for_hash) & this.v_mask;
            RNGs[i] = new RNG(spine_values[i], this.c);
        }
        return RNGs;
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
     * Convert int format symbols into bytes.
     *
     * @param temp_symbols Symbols with integer format.
     * For example, if c = 5, the divided ints might show as follow,
     * temp_symbols: [0]0...0 0 1 2 3 4 (32-bits)
     *               [1]0...0 5 6 7 8 9 (32-bits)
     *               [2]0...0 10 11 12 13 14 (32-bits)
     *               ...
     * output bytes: [0]0 1 2 3 4 | 5 6 7
     *               [1]8 9 | 10 11 12 13
     *               ...
     *
     * @return Array of int.
     */
    private byte[] tempSymbols2Symbols(int[] temp_symbols) {
        byte[] symbols = new byte[temp_symbols.length * this.c / 8];
        int pointer = 0; // record the position in each bytes => e.g. [p0000000] => pointer = 0
        int count = 0; // record the position in bytes array of M
        for (int i = 0; i < temp_symbols.length; i++) {
            for (int j = 0; j < this.c; j++) {
                symbols[count] |= ((temp_symbols[i] & (1<<(this.c-j-1))) >> (this.c-j-1)) == 1 ? (1<<(8-pointer-1)) : 0;
                ++pointer;
                if(pointer == 8) {
                    pointer = 0;
                    ++count;
                }
            }
        }
        return symbols;
    }

    /**
     * Getters and setters.
     */
    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    public int getL() {
        return l;
    }

    public void setL(int l) {
        this.l = l;
    }
}
