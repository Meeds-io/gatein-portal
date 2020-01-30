/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.web.security.codec;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.gatein.common.io.IOTools;

import junit.framework.TestCase;

/**
 * @author <a href="hoang281283@gmail.com">Minh Hoang TO</a>
 */
public class TestJCASymmetricCodec extends TestCase {

    public void testDefaultCodec() throws Exception {
        URL keyFile = Thread.currentThread().getContextClassLoader().getResource("conf/key.txt");

        if (System.getProperty("java.vendor").toLowerCase().contains("ibm")) {
            // this keystore is generated with the following command
            // /usr/lib/jvm/ibm-java-x86_64-70/bin/keytool -genseckey \
            // -keypass gtnKeyPass -storetype JCEKS -storepass gtnStorePass \
            // -alias gtnKey -keyalg AES -keystore key-ibmjvm.txt -keysize 128
            keyFile = Thread.currentThread().getContextClassLoader().getResource("conf/key-ibmjvm.txt");
        }

        Map<String, String> config = new HashMap<String, String>();
        config.put("gatein.codec.jca.symmetric.keyalg", "AES");
        config.put("gatein.codec.jca.symmetric.keystore", URLDecoder.decode(keyFile.getPath(), "UTF-8"));
        config.put("gatein.codec.jca.symmetric.storetype", "JCEKS");
        config.put("gatein.codec.jca.symmetric.alias", "gtnKey");
        config.put("gatein.codec.jca.symmetric.keypass", "gtnKeyPass");
        config.put("gatein.codec.jca.symmetric.storepass", "gtnStorePass");

        AbstractCodec codec = new JCASymmetricCodecBuilder().build(config);
        assertNotNull(codec);

        String encrypted = codec.encode("exoplatform");
        assertFalse("exoplatform".equals(encrypted));
        assertEquals("exoplatform", codec.decode(encrypted));
    }

    public void testCodecWithGeneratedKey() throws Exception {
        String alias = "testAlias";
        char[] keyPass = "testKeyPass".toCharArray();
        char[] storePass = "testStorePass".toCharArray();

        URL url = Thread.currentThread().getContextClassLoader().getResource("conf");
        File f = new File(new File(url.toURI()), "gen-key.txt");

        if (f.exists()) {
            // remove, as it might be that this keystore was created by a different vendor
            f.delete();
        }

        f.createNewFile();
        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
        SecretKey tmpSecretKey = keyGen.generateKey();
        KeyStore tmpStore = KeyStore.getInstance("JCEKS");
        tmpStore.load(null, storePass);
        tmpStore.setEntry(alias, new KeyStore.SecretKeyEntry(tmpSecretKey), new KeyStore.PasswordProtection(keyPass));
        OutputStream out = new FileOutputStream(f);
        try {
            tmpStore.store(out, storePass);
        } finally {
            IOTools.safeClose(out);
        }

        Map<String, String> config = new HashMap<String, String>();
        config.put("gatein.codec.jca.symmetric.keyalg", "DES");
        config.put("gatein.codec.jca.symmetric.keystore", f.getPath());
        config.put("gatein.codec.jca.symmetric.storetype", "JCEKS");
        config.put("gatein.codec.jca.symmetric.alias", alias);
        config.put("gatein.codec.jca.symmetric.keypass", "testKeyPass");
        config.put("gatein.codec.jca.symmetric.storepass", "testStorePass");

        AbstractCodec codec = new JCASymmetricCodecBuilder().build(config);
        assertNotNull(codec);
        assertFalse("exoplatform".equals(codec.encode("exoplatform")));
        assertEquals("exoplatform", codec.decode(codec.encode("exoplatform")));
        assertEquals("123456", codec.decode(codec.encode("123456")));
    }

}
