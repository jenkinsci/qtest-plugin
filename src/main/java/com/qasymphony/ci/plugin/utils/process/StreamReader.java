package com.qasymphony.ci.plugin.utils.process;

import jline.internal.InputStreamReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;

import static java.nio.charset.Charset.defaultCharset;

public final class StreamReader implements Runnable {
    //~ class properties ========================================================
    private BufferedReader reader;
    private boolean completed;
    private final IStreamConsumer streamConsumer;


    //~ class members ===========================================================


    /**
     * Creates {@link StreamReader stream pumper} instance from the given stream,
     * the stream consumer, the prefix, the stream encoding and clock.
     *
     * @param input the given input used to read data.
     * @param streamConsumer the given stream consumer used to consume data.
     * @param encoding the given encoding.
     */
    public StreamReader(InputStream input, IStreamConsumer streamConsumer,
                        String encoding) {
        this.streamConsumer = streamConsumer;
        try {
            this.reader = (StringUtils.isEmpty(encoding) ? new LineNumberReader(new InputStreamReader(input))
                    : new LineNumberReader(new InputStreamReader(input, encoding)));
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(String.format("Unable to use [%s] to decode stream. The current charset is [%s]", encoding, defaultCharset()));
        }
    }

    @Override
    public void run() {
        try {
            String str = reader.readLine();
            while (str != null) {
                consumeLine(str);
                str = reader.readLine();
            }
        } catch (Exception e) {
            // don't worry, we don't want to handle this.
        } finally {
            IOUtils.closeQuietly(reader);
            completed = true;
        }
    }

    /**
     * Try to read data to end of stream.
     */
    public void readToEnd() {
        while (!completed) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                // don't worry, we don't want to handle this.
            }
        }
    }

    /**
     * Consumes the current value.
     *
     * @param str the given data to consume.
     */
    private void consumeLine(String str) {
        if (streamConsumer != null) {
            streamConsumer.consumeLine(str);
        }
    }

    //~ class helpers ===========================================================
    /**
     * Pumps the stream using the stream consumer, the prefix and encoding.
     *
     * @param input the given stream to read data.
     * @param consumer the given stream consumer used to consume stream data.
     * @param encoding the given encoding to read data from stream.
     * @return the {@link StreamReader stream pumper} instance.
     */
    public static StreamReader pump(InputStream input, IStreamConsumer consumer, String encoding) {
        StreamReader pumper = new StreamReader(input, consumer, encoding);
        new Thread(pumper).start();
        return pumper;
    }
}