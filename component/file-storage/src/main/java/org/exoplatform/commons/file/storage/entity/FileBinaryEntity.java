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
package org.exoplatform.commons.file.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity for Binary DATA File.
 * Created by The eXo Platform SAS
 * Author : eXoPlatform exo@exoplatform.com
 */
@Entity(name = "FileBinaryEntity")
@ExoEntity
@Table(name = "FILES_BINARY")
@NamedQueries({
        @NamedQuery(name = "FileBinaryEntity.findByName", query = "SELECT t FROM FileBinaryEntity t WHERE t.name = :name")})
public class FileBinaryEntity {
    @Id
    @Column(name = "BLOB_ID")
    @SequenceGenerator(name = "SEQ_FILES_BINARY_BLOB_ID", sequenceName = "SEQ_FILES_BINARY_BLOB_ID")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FILES_BINARY_BLOB_ID")
    private long            id;

    @Column(name = "NAME")
    private String          name;

    @Column(name = "DATA", columnDefinition="BLOB")
    private byte[] data;

    @Column(name = "UPDATED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
    }
}
