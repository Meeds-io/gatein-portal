/**
 * Copyright (C) 2009 eXo Platform SAS.
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

package org.exoplatform.test;

/**
 * Created by The eXo Platform SARL
 * Author : Lai Van Khoi
 *          laivankhoi46pm1@yahoo.com
 * Nov 27, 2006  
 */
import org.exoplatform.component.test.AbstractGateInTest;
import org.exoplatform.services.chars.chardet.Detector;
import org.exoplatform.services.chars.chardet.ICharsetDetectionObserver;
import org.exoplatform.services.chars.chardet.PSMDetector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class TestFile extends AbstractGateInTest
{
   public static boolean found = false;

   public void testFile() throws Exception
   {
      File file = new File(ClassLoader.getSystemResource("normal.html").getFile());
      assertEquals(true, file.exists());
      FileInputStream input = new FileInputStream(file);
      BufferedInputStream buffer = new BufferedInputStream(input);
      byte[] data = new byte[buffer.available()];
      int available = -1;

      Detector det = new Detector(PSMDetector.ALL);

      //Set an observer...
      //The Notify() will be called when a matching charset is found.
      det.init(new ICharsetDetectionObserver()
      {
         public void notify(String charset)
         {
            System.out.println("CHARSET === " + charset);
         }
      });

      boolean done = false;
      boolean isAscii = true;

      while ((available = buffer.read(data)) > -1)
      {
         //Khoilv'code.
         //System.out.print(data);
         if (isAscii)
            isAscii = det.isAscii(data, available);

         //DoIt if non-ascii and not done yet.
         if (!isAscii && !done)
            done = det.doIt(data, available, false);
      }

      det.dataEnd();

      if (isAscii)
      {
         System.out.println("CHARSET = ASCII");
         found = true;
      }

      if (!found)
      {
         String prob[] = det.getProbableCharsets();
         for (int i = 0; i < prob.length; i++)
         {
            System.out.println("Probable Charset = " + prob[i]);
         }
      }
   }
}
