/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.runtime.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tron.common.runtime.config.VMConfig;
import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

public final class VMUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger("VM");

    private VMUtils() {
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

    private static File createProgramTraceFile(VMConfig config, String txHash) {
        File result = null;

        if (config.vmTrace()) {

            File file = new File(new File("./", "vm_trace"), txHash + ".json");

            if (file.exists()) {
                if (file.isFile() && file.canWrite()) {
                    result = file;
                }
            } else {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    result = file;
                } catch (IOException e) {
                    // ignored
                }
            }
        }

        return result;
    }

    private static void writeStringToFile(File file, String data) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (data != null) {
                out.write(data.getBytes("UTF-8"));
            }
        } catch (Exception e){
            LOGGER.error(format("Cannot write to file '%s': ", file.getAbsolutePath()), e);
        } finally {
            closeQuietly(out);
        }
    }

    public static void saveProgramTraceFile(VMConfig config, String txHash, String content) {
        File file = createProgramTraceFile(config, txHash);
        if (file != null) {
            writeStringToFile(file, content);
        }
    }

    private static final int BUF_SIZE = 4096;

    private static void write(InputStream in, OutputStream out, int bufSize) throws IOException {
        try {
            byte[] buf = new byte[bufSize];
            for (int count = in.read(buf); count != -1; count = in.read(buf)) {
                out.write(buf, 0, count);
            }
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    public static byte[] compress(byte[] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DeflaterOutputStream out = new DeflaterOutputStream(baos, new Deflater(), BUF_SIZE);

        write(in, out, BUF_SIZE);

        return baos.toByteArray();
    }

    public static byte[] compress(String content) throws IOException {
        return compress(content.getBytes("UTF-8"));
    }

    public static byte[] decompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);

        ByteArrayInputStream in = new ByteArrayInputStream(data);
        InflaterOutputStream out = new InflaterOutputStream(baos, new Inflater(), BUF_SIZE);

        write(in, out, BUF_SIZE);

        return baos.toByteArray();
    }

    public static String zipAndEncode(String content) {
        try {
            return encodeBase64String(compress(content));
        } catch (Exception e) {
            LOGGER.error("Cannot zip or encode: ", e);
            return content;
        }
    }

    public static String unzipAndDecode(String content) {
        try {
            byte[] decoded = decodeBase64(content);
            return new String(decompress(decoded), "UTF-8");
        } catch (Exception e) {
            LOGGER.error("Cannot unzip or decode: ", e);
            return content;
        }
    }
}
