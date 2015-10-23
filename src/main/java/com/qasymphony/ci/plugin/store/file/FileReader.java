package com.qasymphony.ci.plugin.store.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author trongle
 * @version 10/23/2015 1:12 AM trongle $
 * @since 1.0
 */
public class FileReader implements Closeable, AutoCloseable {
  // Shared across all instances
  private static final ForkJoinPool DEFAULT_POOL = new ForkJoinPool();

  private static final long MIN_FORK_THRESHOLD = 1000000L;
  private static final String READ_MODE = "r";

  private final BufferedAccessFile raf;
  private final Charset charset;
  private final SortedSet<Long> index;

  private final Lock lock;

  private boolean isClosed = false;

  public FileReader(File file) throws IOException {
    this(file, Charset.defaultCharset(), 1, DEFAULT_POOL);
  }

  public FileReader(File file, Charset charset) throws IOException {
    this(file, charset, 1, DEFAULT_POOL);
  }

  public FileReader(File file, Charset charset, int splitCount)
    throws IOException {
    this(file, charset, splitCount, DEFAULT_POOL);
  }

  public FileReader(File file, Charset charset, int splitCount,
    ForkJoinPool pool) throws IOException {
    this.raf = new BufferedAccessFile(file, READ_MODE);
    this.charset = charset;

    long threshold = Math.max(MIN_FORK_THRESHOLD, file.length()
      / splitCount);
    this.index = Collections.unmodifiableSortedSet(pool
      .invoke(new IndexingTask(file, 0, file.length(), threshold)));

    this.lock = new ReentrantLock();
  }

  public FileReader(File file, int splitCount) throws IOException {
    this(file, Charset.defaultCharset(), splitCount, DEFAULT_POOL);
  }

  @Override
  public void close() throws IOException {
    raf.close();
    isClosed = true;
  }

  public SortedMap<Integer, String> find(int from, int to, String regex)
    throws IOException {
    assertIsOpen();
    if (regex == null) {
      throw new NullPointerException("Regex cannot be null");
    }
    if (from < 1) {
      throw new IllegalArgumentException("'from' must be greater than or equal to 1");
    }
    if (to < from) {
      throw new IllegalArgumentException("'to' must be greater than or equal to 'from'");
    }

    SortedMap<Integer, String> lines = new TreeMap<Integer, String>();
    List<Long> positions = new ArrayList<Long>(index);

    try {
      lock.lock();

      raf.seek(positions.get(from - 1));
      for (int i = from; i <= to; i++) {
        String line = raf.getNextLine(charset);
        if (line != null) {
          if (line.matches(regex)) {
            lines.put(i, line);
          }
        } else {
          break;
        }
      }

      return lines;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get lines count of file
   *
   * @return
   */
  public int size() {
    return index.size();
  }

  /**
   * Read first n line
   *
   * @param n
   * @return
   * @throws IOException
   */
  public SortedMap<Integer, String> head(int n) throws IOException {
    assertIsOpen();
    if (n < 1) {
      throw new IllegalArgumentException("'n' must be greater than or equal to 1");
    }
    return readLines(1, n);
  }

  /**
   * @return
   * @throws IOException
   */
  public SortedMap<Integer, String> readAll() throws IOException {
    assertIsOpen();
    return readLines(1, this.size());
  }

  /**
   * Read from line to line
   *
   * @param from
   * @param to
   * @return
   * @throws IOException
   */
  public SortedMap<Integer, String> readLines(int from, int to)
    throws IOException {
    assertIsOpen();
    if (from < 1) {
      throw new IllegalArgumentException("'from' must be greater than or equal to 1");
    }
    if (to < from) {
      throw new IllegalArgumentException("'to' must be greater than or equal to 'from'");
    }
    if (from > index.size()) {
      throw new IllegalArgumentException("'from' must be less than the file's number of lines");
    }

    SortedMap<Integer, String> lines = new TreeMap<>();
    List<Long> positions = new ArrayList<>(index);

    try {
      lock.lock();

      raf.seek(positions.get(from - 1));
      for (int i = from; i <= to; i++) {
        String line = raf.getNextLine(charset);
        if (line != null) {
          lines.put(i, line);
        } else {
          break;
        }
      }

      return lines;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Read last n line from file
   *
   * @param n
   * @return
   * @throws IOException
   */
  public SortedMap<Integer, String> tail(int n) throws IOException {
    assertIsOpen();
    if (n < 1) {
      throw new IllegalArgumentException("'n' must be greater than or equal to 1");
    }

    int from = index.size() - n;
    int to = from + n;
    return readLines(from, to);
  }

  private void assertIsOpen() {
    if (isClosed) {
      throw new IllegalStateException("Reader is closed!");
    }
  }

  private static final class IndexingTask extends
    RecursiveTask<SortedSet<Long>> {
    private static final long serialVersionUID = 3509549890190032574L;
    private final File file;
    private final long start;
    private final long end;
    private final long length;
    private final long threshold;

    public IndexingTask(File file, long start, long end, long threshold) {
      this.file = file;
      this.start = start;
      this.end = end;
      this.length = end - start;
      this.threshold = threshold;
    }

    @Override
    protected SortedSet<Long> compute() {
      SortedSet<Long> index = new TreeSet<Long>();
      try {
        if (length < threshold) {
          BufferedAccessFile raf = null;
          try {
            raf = new BufferedAccessFile(file, "r");
            raf.seek(start);

            // Add the position for 1st line
            if (raf.getFilePointer() == 0L) {
              index.add(Long.valueOf(raf.getFilePointer()));
            }
            while (raf.getFilePointer() < end) {
              raf.getNextLine();
              index.add(Long.valueOf(raf.getFilePointer()));
            }
          } finally {
            if (raf != null) {
              raf.close();
            }
          }
        } else {
          long start1 = start;
          long end1 = start + (length / 2);

          long start2 = end1;
          long end2 = end;

          IndexingTask task1 = new IndexingTask(file, start1, end1,
            threshold);
          task1.fork();
          IndexingTask task2 = new IndexingTask(file, start2, end2,
            threshold);

          index.addAll(task2.compute());
          index.addAll(task1.join());
        }
      } catch (IOException ex) {
        throw new FileReaderException("Error while index file:" + ex.getMessage(), ex);
      }

      return index;
    }
  }
}
