package org.tron.core.services.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class CharResponseWrapper extends HttpServletResponseWrapper {

  private ServletOutputStream outputStream;
  private PrintWriter writer;
  private ServletOutputStreamCopy streamCopy;


  public CharResponseWrapper(HttpServletResponse response) throws IOException {
    super(response);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("getWriter() has been called .");
    }

    if (outputStream == null) {
      outputStream = getResponse().getOutputStream();
      streamCopy = new ServletOutputStreamCopy(outputStream);
    }

    return streamCopy;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException("getOutputStream() has been called.");
    }

    if (writer == null) {
      streamCopy = new ServletOutputStreamCopy(getResponse().getOutputStream());
      // set auto flash so that copy can be valid
      writer = new PrintWriter(new OutputStreamWriter(streamCopy,
          getResponse().getCharacterEncoding()), true);
    }

    return writer;
  }

  @Override
  public void flushBuffer() throws IOException {  // flush both stream
    if (writer != null) {
      writer.flush();
    } else if (outputStream != null) {
      streamCopy.flush();
    }
  }

  public int getByteSize() {
    return streamCopy.getStreamByteSize();
  }

}