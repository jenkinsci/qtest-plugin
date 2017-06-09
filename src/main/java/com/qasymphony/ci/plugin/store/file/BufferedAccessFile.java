package com.qasymphony.ci.plugin.store.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

/**
 * @author trongle
 * @version 10/23/2015 1:20 AM trongle $
 * @since 1.0
 */
public class BufferedAccessFile extends RandomAccessFile {
  private static final int DEFAULT_BUFFER_SIZE = 256;
  private final int bufferSize;
  private byte buffer[];
  private int bufferEnd = 0;
  private int bufferPos = 0;
  private long realPos = 0L;

  /**
   * @param file file to access
   * @param mode mode
   * @throws IOException IOException
   */
  public BufferedAccessFile(File file, String mode) throws IOException {
    this(file, mode, DEFAULT_BUFFER_SIZE);
  }

  /**
   * @param file       file to access
   * @param mode       mode
   * @param bufferSize bufferSize
   * @throws IOException IOException
   */
  public BufferedAccessFile(File file, String mode, int bufferSize)
    throws IOException {
    super(file, mode);
    invalidate();
    this.bufferSize = bufferSize;
    this.buffer = new byte[bufferSize];
  }

  /**
   * @param filename fileName
   * @param mode     mode
   * @throws IOException IOException
   */
  public BufferedAccessFile(String filename, String mode)
    throws IOException {
    this(filename, mode, DEFAULT_BUFFER_SIZE);
  }

  /**
   * @param filename fileName
   * @param mode     mode
   * @param bufsize  bufsize
   * @throws IOException IOException
   */
  public BufferedAccessFile(String filename, String mode, int bufsize)
    throws IOException {
    super(filename, mode);
    invalidate();
    this.bufferSize = bufsize;
    this.buffer = new byte[bufsize];
  }

  @Override
  public long getFilePointer() throws IOException {
    return (realPos - bufferEnd) + bufferPos;
  }

  /**
   * @return line
   * @throws IOException IOException
   */
  public final String getNextLine() throws IOException {
    return getNextLine(Charset.defaultCharset());
  }

  /**
   * @param charset charset
   * @return line
   * @throws IOException IOException
   */
  public final String getNextLine(Charset charset) throws IOException {
    String str = null;

    // Fill the buffer
    if (bufferEnd - bufferPos <= 0) {
      if (fillBuffer() < 0) {
        return null;
      }
    }

    // Find line terminator from buffer
    int lineEnd = -1;
    for (int i = bufferPos; i < bufferEnd; i++) {
      if (buffer[i] == '\n') {
        lineEnd = i;
        break;
      }
    }

    // Line terminator not found from buffer
    if (lineEnd < 0) {
      StringBuilder sb = new StringBuilder(256);

      int c;
      while (((c = read()) != -1) && (c != '\n')) {
        if ((char) c != '\r') {
          sb.append((char) c);
        }
      }
      if ((c == -1) && (sb.length() == 0)) {
        return null;
      }

      return sb.toString();
    }

    if (lineEnd > 0 && buffer[lineEnd - 1] == '\r') {
      str = new String(buffer, bufferPos, lineEnd - bufferPos - 1,
        charset);
    } else {
      str = new String(buffer, bufferPos, lineEnd - bufferPos, charset);
    }

    bufferPos = lineEnd + 1;
    return str;
  }

  @Override
  public final int read() throws IOException {
    if (bufferPos >= bufferEnd) {
      if (fillBuffer() < 0) {
        return -1;
      }
    }

    if (bufferEnd == 0) {
      return -1;
    } else {
      return buffer[bufferPos++];
    }
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    int leftover = bufferEnd - bufferPos;
    if (len <= leftover) {
      System.arraycopy(buffer, bufferPos, b, off, len);
      bufferPos += len;
      return len;
    }

    for (int i = 0; i < len; i++) {
      int c = this.read();
      if (c != -1)
        b[off + i] = (byte) c;
      else {
        return i;
      }
    }

    return len;
  }

  @Override
  public void seek(long pos) throws IOException {
    int n = (int) (realPos - pos);
    if (n >= 0 && n <= bufferEnd) {
      bufferPos = bufferEnd - n;
    } else {
      super.seek(pos);
      invalidate();
    }
  }

  private int fillBuffer() throws IOException {
    int n = super.read(buffer, 0, bufferSize);
    if (n >= 0) {
      realPos += n;
      bufferEnd = n;
      bufferPos = 0;
    }

    return n;
  }

  private void invalidate() throws IOException {
    bufferEnd = 0;
    bufferPos = 0;
    realPos = super.getFilePointer();
  }
}
