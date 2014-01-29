package org.s1.misc;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 12:24
 */
public class IOUtils {

    public static String toString(byte[] b, String charset) {
        return new String(b, Charset.forName(charset));
    }

    public static String toString(InputStream is, String charset) {
        Scanner s = new Scanner(is, charset).useDelimiter("\\A");
        return s.hasNext() ? s.next() : null;
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

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
