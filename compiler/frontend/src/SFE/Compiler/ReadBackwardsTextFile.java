package SFE.Compiler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class ReadBackwardsTextFile {
  /*
   * Instance variables
   */
  private RandomAccessFile raf;
  private int seekTableCursor;
  /*
   * A list of seek positions which are guaranteed to start at the beginning of lines, and
   * to be suitably spread apart (we attempt to provide bufferSize increments).
   */
  private ArrayList<Long> seekTable;
  /*
   * The current chunk of the file loaded into memory.
   */
  private ArrayList<String> buffer;
  private int bufferCursor;
  private int bufferSize; //in bytes
  private long filesize; //total file size of the file

  /*
   * Constructors
   */
  public ReadBackwardsTextFile(File file, int bufferSize) {
    if (bufferSize <= 2) {
      throw new RuntimeException("Buffer size must be greater than 2");
    }

    this.bufferSize = bufferSize;
    buffer = new ArrayList();
    try {
      raf = new RandomAccessFile(file, "r");
      seekTable = new ArrayList();
      long lastPos = 0;
      seekTable.add(lastPos);
      byte[] slushbuffer = new byte[bufferSize];
      while(true) {
        long pos = raf.getFilePointer();

        //Skip at most bufferSize - 2 bytes (the last two might be a line return)
        raf.read(slushbuffer, 0, bufferSize - 2);

        //Continue until an endline is reached (either \n or \r\n) or end of file is reached.
        loop: while(true) {
          int c = raf.read();
          switch(c) {
          case -1:
          case '\n':
            break loop;
          case '\r':
            raf.read();
            break loop;
          }
        }

        if (raf.getFilePointer() == pos) {
          //EOF.
          break;
        }

        if (pos - lastPos >= bufferSize) {
          lastPos = pos;
          seekTable.add(pos);
        }
      }
      filesize = raf.getFilePointer();
      seekTableCursor = seekTable.size();
      bufferCursor = 0;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
  /*
   * Methods
   */
  public boolean hasPreviousLine() {
    return bufferCursor > 0 || seekTableCursor > 0;
  }
  public double progress() {
    try {
      return (filesize - raf.getFilePointer()) / (double)filesize;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  public String previousLine() {
    if (bufferCursor > 0) {
      bufferCursor--;
      return buffer.get(bufferCursor);
    }
    seekTableCursor--;
    long bufferStart = seekTable.get(seekTableCursor);
    try {
      raf.seek(bufferStart);
      buffer.clear();
      while(true) {
        long pos = raf.getFilePointer();
        if (pos - bufferStart >= bufferSize) {
          break; //Don't read any more lines.
        }
        String line = raf.readLine();
        if (raf.getFilePointer() == pos) {
          //EOF
          break;
        }
        buffer.add(line);
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    bufferCursor = buffer.size();
    return previousLine();
  }

}
