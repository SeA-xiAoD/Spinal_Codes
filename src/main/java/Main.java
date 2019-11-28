/**
 * Created by SeAxiAoD on 2019/11/12.
 */

import java.util.Random;

public class Main {
    public static void main(String[] args){

        /************************ Step 1: Initialization ***********************/
        SpinalEncoder encoder = new SpinalEncoder(4,32,6,1);
        SpinalDecoder decoder = new SpinalDecoder(4,32,6,1, 16, 1);
        String message128 = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567";
//        String message = "11111111";
        byte[] message_bytes = message128.getBytes();

        /************************ Step 2: Decoding ***********************/
        long start_time = System.currentTimeMillis();
        byte[] symbols = encoder.encode(message_bytes);
        long encoding_time = System.currentTimeMillis();

        /************************ Step 3: Disturbance ***********************/
//        System.out.println(symbols[62]);
//        symbols[60] = -76;

        /************************ Step 4: Decoding ***********************/
        byte[] decoded_symbols = decoder.decode(symbols);
        long decoding_time = System.currentTimeMillis();

        System.out.printf("Message\t\t\tDecoded message\n");
        for (int i = 0; i < decoded_symbols.length; i++) {
            System.out.printf("%d\t\t\t%d\n", message_bytes[i], decoded_symbols[i]);
        }

        /************************ Step 5: Output ***********************/
        System.out.println("Encoding cost:");
        System.out.println(encoding_time - start_time);
        System.out.println("Decoding cost:");
        System.out.println(decoding_time - encoding_time);
    }
}
