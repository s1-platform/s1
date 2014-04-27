/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.misc;

/**
 * Base64 utils
 */
public class Base64 {

    private static final String base64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static byte[] base64bytes;

    static {
        base64bytes = new byte[64];
        for (int i = 0; i<64; i++) {
            byte c = (byte) base64chars.charAt(i);
            base64bytes[i] = c;
        }
    }

    /**
     *
     * @param bytes
     * @return
     */
    public static String encode(byte [] bytes){
        int length = bytes.length;
        int start = 0;

        byte[] dst = new byte[(length+2)/3 * 4 + length/72];
        int x = 0;
        int dstIndex = 0;
        int state = 0;	// which char in pattern
        int old = 0;	// previous byte
        int len = 0;	// length decoded so far
        int max = length + start;
        for (int srcIndex = start; srcIndex<max; srcIndex++) {
            x = bytes[srcIndex];
            switch (++state) {
                case 1:
                    dst[dstIndex++] = base64bytes[(x>>2) & 0x3f];
                    break;
                case 2:
                    dst[dstIndex++] = base64bytes[((old<<4)&0x30)
                            | ((x>>4)&0xf)];
                    break;
                case 3:
                    dst[dstIndex++] = base64bytes[((old<<2)&0x3C)
                            | ((x>>6)&0x3)];
                    dst[dstIndex++] = base64bytes[x&0x3F];
                    state = 0;
                    break;
            }
            old = x;
            if (++len >= 72) {
                dst[dstIndex++] = (byte) '\n';
                len = 0;
            }
        }

	/*
	 * now clean up the end bytes
	 */

        switch (state) {
            case 1: dst[dstIndex++] = base64bytes[(old<<4) & 0x30];
                dst[dstIndex++] = (byte) '=';
                dst[dstIndex++] = (byte) '=';
                break;
            case 2: dst[dstIndex++] = base64bytes[(old<<2) & 0x3c];
                dst[dstIndex++] = (byte) '=';
                break;
        }
        return new String(dst);
    }

    /**
     *
     * @param base64
     * @return
     * @throws Base64FormatException
     */
    public static byte [] decode(String base64) throws Base64FormatException{
        base64 = base64.replace("\n","").replace("\r","");
        if(base64.length()%4!=0)
            throw new Base64FormatException("Bad base64 string");

        int end = 0;	// end state
        if (base64.endsWith("=")) {
            end++;
        }
        if (base64.endsWith("==")) {
            end++;
        }

        int len = (base64.length() + 3)/4 * 3 - end;
        byte[] result = new byte[len];
        int dst = 0;

        try{
        for(int src = 0; src< base64.length(); src++) {
            int code =  base64chars.indexOf(base64.charAt(src));
            if (code == -1) {
                break;
            }
            switch (src%4) {
                case 0:
                    result[dst] = (byte) (code<<2);
                    break;
                case 1:
                    result[dst++] |= (byte) ((code>>4) & 0x3);
                    result[dst] = (byte) (code<<4);
                    break;
                case 2:
                    result[dst++] |= (byte) ((code>>2) & 0xf);
                    result[dst] = (byte) (code<<6);
                    break;
                case 3:
                    result[dst++] |= (byte) (code & 0x3f);
                    break;
            }
        }
        } catch (ArrayIndexOutOfBoundsException e) {}
        return result;
    }

}
