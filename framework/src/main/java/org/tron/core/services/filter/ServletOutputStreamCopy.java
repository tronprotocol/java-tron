package org.tron.core.services.filter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ServletOutputStreamCopy extends ServletOutputStream {

  private final OutputStream outputStream;
  private final AtomicLong byteSize = new AtomicLong(0);

  public ServletOutputStreamCopy(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
    byteSize.incrementAndGet();
  }

  @Override
  public void write(byte @NonNull [] b, int off, int len) throws IOException {
    outputStream.write(b, off, len);
    byteSize.addAndGet(len);
  }

  @Override
  public void write(byte @NonNull [] b) throws IOException {
    outputStream.write(b);
    byteSize.addAndGet(b.length);
  }

  @Override
  public void flush() throws IOException {
    outputStream.flush();
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }

  public long getStreamByteSize() {
    return byteSize.get();
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {

  }
}
