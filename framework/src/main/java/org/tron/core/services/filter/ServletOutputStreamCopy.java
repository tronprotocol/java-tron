package org.tron.core.services.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

class ServletOutputStreamCopy extends ServletOutputStream {

  private OutputStream outputStream;
  private ByteArrayOutputStream copy;
  private int MAX_RESPONSE_SIZE = 4096;

  public ServletOutputStreamCopy(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.copy = new ByteArrayOutputStream(MAX_RESPONSE_SIZE);
  }

  @Override
  public void write(int b) throws IOException {
    outputStream.write(b);
    copy.write(b);
  }

  public int getStreamByteSize() {
    return this.copy.size();
  }

  @Override
  public boolean isReady() {
    return false;
  }

  @Override
  public void setWriteListener(WriteListener writeListener) {

  }
}
