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

package org.exoplatform.commons.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.serialization.api.factory.DefaultObjectFactory;
import org.exoplatform.commons.serialization.api.factory.ObjectFactory;
import org.exoplatform.commons.serialization.model.TypeDomain;
import org.exoplatform.commons.serialization.serial.ObjectReader;
import org.exoplatform.commons.serialization.serial.ObjectWriter;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class SerializationContext {

    /** . */
    private final TypeDomain typeDomain;

    /** . */
    private final Map<Class<?>, ObjectFactory<?>> factories;

    public SerializationContext(TypeDomain typeDomain) {
        HashMap<Class<?>, ObjectFactory<?>> factories = new HashMap<Class<?>, ObjectFactory<?>>();
        factories.put(Object.class, new DefaultObjectFactory());

        //
        this.typeDomain = typeDomain;
        this.factories = factories;
    }

    public <O> void addFactory(ObjectFactory<O> factory) {
        // OK
        Class<ObjectFactory<O>> factoryClass = (Class<ObjectFactory<O>>) factory.getClass();

        //
        ParameterizedType pt = (ParameterizedType) factoryClass.getGenericSuperclass();

        // OK
        Class<?> objectType = (Class<Object>) pt.getActualTypeArguments()[0];

        //
        factories.put(objectType, factory);
    }

    public TypeDomain getTypeDomain() {
        return typeDomain;
    }

    public <O> ObjectFactory<? super O> getFactory(Class<O> type) {
        // OK
        ObjectFactory<O> factory = (ObjectFactory<O>) factories.get(type);

        //
        if (factory == null) {
            return getFactory(type.getSuperclass());
        }

        //
        return factory;
    }

    public <O> O clone(O o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectWriter writer = new ObjectWriter(this, baos);
        writer.writeObject(o);
        writer.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectReader in = new ObjectReader(this, bais);
        return (O) in.readObject();
    }

    public void write(Object o, OutputStream out) throws IOException {
        ObjectWriter writer = new ObjectWriter(this, out);
        writer.writeObject(o);
        writer.flush();
    }

    public byte[] write(Object o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectWriter writer = new ObjectWriter(this, baos);
        writer.writeObject(o);
        writer.close();
        return baos.toByteArray();
    }

    public Object read(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectReader in = new ObjectReader(this, bais);
        return in.readObject();
    }

    public Object read(InputStream in) throws IOException, ClassNotFoundException {
        ObjectReader or = new ObjectReader(this, in);
        return or.readObject();
    }
}
