/*
 * Copyright (C) 2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.portal.gadget.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Response wrapper which allows to alter response output
 */
public class CharResponseWrapper extends HttpServletResponseWrapper {
  private ByteArrayPrintWriter output;

  private boolean              usingWriter;

  public CharResponseWrapper(HttpServletResponse response) {
    super(response);
    usingWriter = false;
    output = new ByteArrayPrintWriter();
  }

  public byte[] getByteArray() {
    return output.toByteArray();
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    // will error out, if in use
    if (usingWriter) {
      super.getOutputStream();
    }
    usingWriter = true;
    return output.getStream();
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    // will error out, if in use
    if (usingWriter) {
      super.getWriter();
    }
    usingWriter = true;
    return output.getWriter();
  }

  public String toString() {
    return output.toString();
  }

  public static class ByteArrayServletStream extends ServletOutputStream {
    ByteArrayOutputStream baos;

    public ByteArrayServletStream(ByteArrayOutputStream baos) {
      this.baos = baos;
    }

    public void write(int param) throws IOException {
      baos.write(param);
    }
  }

  public static class ByteArrayPrintWriter {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    private PrintWriter           pw   = new PrintWriter(baos);

    private ServletOutputStream   sos  = new ByteArrayServletStream(baos);

    public PrintWriter getWriter() {
      return pw;
    }

    public ServletOutputStream getStream() {
      return sos;
    }

    byte[] toByteArray() {
      return baos.toByteArray();
    }
  }
}
