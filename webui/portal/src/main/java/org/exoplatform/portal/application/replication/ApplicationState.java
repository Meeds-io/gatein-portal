/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.portal.application.replication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.exoplatform.commons.serialization.SerializationContext;
import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.core.UIApplication;

/**
 * The state of an application.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class ApplicationState implements Serializable {

    /** . */
    private UIApplication application;

    /** . */
    private byte[] serialization;

    /** . */
    private String userName;

    public ApplicationState(UIApplication application, String userName) {
        if (application == null) {
            throw new NullPointerException();
        }
        this.application = application;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public UIApplication getApplication() throws IOException, ClassNotFoundException {
        if (serialization != null) {
            SerializationContext serializationContext = SerializationContextSingleton.getInstance();
            byte[] bytes = serialization;
            serialization = null;
            application = (UIApplication) serializationContext.read(bytes);
        }
        return application;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.putFields();
        oos.writeFields();

        if (userName != null) {
            oos.writeBoolean(true);
            oos.writeUTF(userName);
        } else {
            oos.writeBoolean(false);
        }

        //
        if (application != null && application.getClass().getAnnotation(Serialized.class) != null) {
            oos.writeBoolean(true);

            //
            SerializationContext serializationContext = SerializationContextSingleton.getInstance();

            //
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializationContext.write(application, baos);
            baos.close();

            //
            byte[] bytes = baos.toByteArray();
            oos.writeInt(bytes.length);
            oos.write(bytes);
        } else {
            oos.writeBoolean(false);
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.readFields();

        if (ois.readBoolean()) {
            userName = ois.readUTF();
        }

        //
        if (ois.readBoolean()) {
            int size = ois.readInt();
            byte[] bytes = new byte[size];
            ois.readFully(bytes);
            serialization = bytes;
        } else {
            serialization = null;
        }

        //
        application = null;
    }
}
