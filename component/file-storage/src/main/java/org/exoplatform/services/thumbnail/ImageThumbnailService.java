/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.services.thumbnail;

import org.exoplatform.commons.file.model.FileItem;

public interface ImageThumbnailService {

  /**
   * Retrieves a thumbnail by given width and height or creates a thumbnail
   * image and get it if not exist
   * 
   * @param file Image file
   * @param width target thumbnail width
   * @param height target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   */
  FileItem getOrCreateThumbnail(FileItem file, int width, int height) throws Exception;

  /**
   * Retrieves a thumbnail by given width and height or creates a thumbnail
   * image and get it if not exist
   * 
   * @param resizeSupplier if resizeSupplier is null then use {@link ImageResizeService}
   * @param file Image file
   * @param width target thumbnail width
   * @param height target thumbnail height
   * @return {@link FileItem}
   * @throws Exception
   */
  FileItem getOrCreateThumbnail(ImageResizeService resizeSupplier, FileItem file, int width, int height) throws Exception;

  void deleteThumbnails(Long fileId);
}
