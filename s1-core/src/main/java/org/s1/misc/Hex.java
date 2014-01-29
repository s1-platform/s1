package org.s1.misc;

import java.math.BigInteger;

/**
 * s1v2
 * User: GPykhov
 * Date: 19.01.14
 * Time: 13:08
 */
public class Hex {

    public static String encode(byte [] bytes){
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    public static byte [] decode(String hex){
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

}
