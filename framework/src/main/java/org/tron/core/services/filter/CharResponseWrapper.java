package org.tron.core.services.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class CharResponseWrapper extends HttpServletResponseWrapper {

  private ServletOutputStreamCopy outputStream;
  private PrintWriter writer;

  public CharResponseWrapper(HttpServletResponse response) {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has been called .");
    }

    if (outputStream == null) {
      outputStream = new ServletOutputStreamCopy(super.getOutputStream());
    }

    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException("getOutputStream() has been called.");
    }

    if (writer == null) {
      outputStream = new ServletOutputStreamCopy(super.getOutputStream());
      // set auto flash so that copy can be valid
      writer = new PrintWriter(new OutputStreamWriter(outputStream, super.getCharacterEncoding()),
          true);
    }

    return writer;
  }

  @Override
  public void flushBuffer() throws IOException {  // flush both stream
    if (writer != null) {
      writer.flush();
    }
    if (outputStream != null) {
      outputStream.flush();
    }
    super.flushBuffer();
  }

  public long getByteSize() throws IOException {
    flushBuffer();
    return outputStream == null ? 0 : outputStream.getStreamByteSize();
  }

}