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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * IO utils
 */
public class IOUtils {

    /**
     *
     * @param b
     * @param charset
     * @return
     */
    public static String toString(byte[] b, String charset) {
        return new String(b, Charset.forName(charset));
    }

    /**
     *
     * @param is
     * @param charset
     * @return
     */
    public static String toString(InputStream is, String charset) {
        Scanner s = new Scanner(is, charset).useDelimiter("\\A");
        return s.hasNext() ? s.next() : null;
    }

    /**
     *
     * @param closeable
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     *
     * @param is
     * @param os
     * @return
     * @throws IOException
     */
    public static long copy(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[4096];
        long count = 0;
        int n = 0;
        while (-1 != (n = is.read(buffer))) {
            os.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     *
     * @param is
     * @param os
     * @param offset
     * @param length
     * @return
     * @throws IOException
     */
    public static long copy(InputStream is, OutputStream os, long offset, long length) throws IOException {
        if (offset > 0) {
            //skip
            long remain = offset;
            byte[] skipBuffer = new byte[4096];
            int skipBufferSize = skipBuffer.length;
            while (remain > 0) {
                long n = is.read(skipBuffer, 0, (int) Math.min(remain, skipBufferSize));
                if (n < 0) { // EOF
                    break;
                }
                remain -= n;
            }
            ;
        }
        if (length == 0)
            return 0;

        byte buffer[] = new byte[4096];
        final int bufferLength = buffer.length;
        int bytesToRead = bufferLength;
        if (length > 0 && length < bufferLength) {
            bytesToRead = (int) length;
        }
        int read;
        long totalRead = 0;
        while (bytesToRead > 0 && -1 != (read = is.read(buffer, 0, bytesToRead))) {
            os.write(buffer, 0, read);
            totalRead += read;
            if (length > 0) { // only adjust length if not reading to the end
                // Note the cast must work because buffer.length is an integer
                bytesToRead = (int) Math.min(length - totalRead, bufferLength);
            }
        }
        return totalRead;
    }

}
