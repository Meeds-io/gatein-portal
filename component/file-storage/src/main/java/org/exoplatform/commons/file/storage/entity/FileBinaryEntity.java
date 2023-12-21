package org.exoplatform.commons.file.storage.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import jakarta.persistence.*;
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
    @SequenceGenerator(name = "SEQ_FILES_BINARY_BLOB_ID", sequenceName = "SEQ_FILES_BINARY_BLOB_ID", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ_FILES_BINARY_BLOB_ID")
    private Long            id;

    @Column(name = "NAME")
    private String          name;

    @Column(name = "DATA", columnDefinition="BLOB")
    private byte[] data;

    @Column(name = "UPDATED_DATE")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
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
