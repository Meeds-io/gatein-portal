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
package org.exoplatform.commons.file.services;

import org.exoplatform.commons.file.resource.FileSystemResourceProvider;
import org.exoplatform.commons.file.resource.BinaryProvider;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

/**
 *
 */
public class FileSystemBinaryProviderTest {

  @Rule
  public TemporaryFolder         folder    = new TemporaryFolder();

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Before
  public void setup() throws Exception {
  }

  @Test
  public void shouldReadBinary() throws Exception {

  }

  @Test
  public void shouldWriteBinary() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());
    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream(new byte[] {}));
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());

    // Then
    java.io.File createdFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertTrue(createdFile.exists());
  }

  @Test
  public void shouldWriteBinaryWhenFileAlreadyExistsAndBinaryHasChanged() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());
    java.io.File createdFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertTrue(createdFile.exists());
    file.setChecksum(new ByteArrayInputStream("test-updated".getBytes()));
    fileResourceProvider.put(file.getFileInfo().getChecksum(), file.getAsStream());

    // Then
    java.io.File updatedFile = fileResourceProvider.getFile(file.getFileInfo().getChecksum());
    assertThat(updatedFile.getAbsolutePath(), is(not(createdFile.getAbsolutePath())));
  }

  @Test
  public void shouldNotWriteBinaryWhenFileAlreadyExistsAndBinaryHasNotChanged() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    fileResourceProvider.put(file);
    java.io.File createdFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertTrue(createdFile.exists());
    fileResourceProvider.put(file);

    // Then
    java.io.File updatedFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertEquals(updatedFile.getAbsolutePath(), createdFile.getAbsolutePath());
    // TODO need to verify also that it does not effectively write
    // verify(fileInfoDataStorage, times(1)).update(any(FileInfoEntity.class));
  }

  @Test
  public void shouldDeleteBinary() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    fileResourceProvider.put(file);
    java.io.File createdFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertTrue(createdFile.exists());
    fileResourceProvider.remove(file.getFileInfo());

    // Then
    java.io.File deletedFile = new java.io.File(fileResourceProvider.getFilePath(file.getFileInfo()));
    assertFalse(deletedFile.exists());
  }

  @Test
  public void shouldThrowExceptionWhenDeletingABinaryWhichDoesNotExist() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileItem file = new FileItem(1L, "file1", "", null, 1, null, "", false, new ByteArrayInputStream("test".getBytes()));
    exception.expect(FileNotFoundException.class);
    fileResourceProvider.remove(file.getFileInfo());
  }

  @Test
  public void shouldReturnPathWhenChecksumIsValid() throws Exception {
    // Given
    FileSystemResourceProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileInfo fileInfo = new FileInfo(1L, "file1", "", null, 1, null, "", "d41d8cd98f00b204e9800998ecf8427e", false);
    String path = fileResourceProvider.getFilePath(fileInfo);

    // Then
    assertEquals(folder.getRoot().getPath() + "/d/4/1/d/8/c/d/9/d41d8cd98f00b204e9800998ecf8427e", path);
  }

  @Test
  public void shouldReturnNullWhenChecksumIsNotValid() throws Exception {
    // Given
    BinaryProvider fileResourceProvider = new FileSystemResourceProvider(folder.getRoot().getPath());

    // When
    FileInfo fileInfo = new FileInfo(1L, "file1", "", null, 1, null, "", "", false);
    String path = fileResourceProvider.getFilePath(fileInfo);

    // Then
    assertNull(path);
  }
}
