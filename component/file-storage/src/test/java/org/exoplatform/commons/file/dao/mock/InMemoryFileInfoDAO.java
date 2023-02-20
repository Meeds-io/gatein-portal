/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.commons.file.dao.mock;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.file.storage.dao.FileInfoDAO;
import org.exoplatform.commons.file.storage.entity.FileInfoEntity;
import org.exoplatform.jpa.mock.AbstractInMemoryDAO;

public class InMemoryFileInfoDAO extends AbstractInMemoryDAO<FileInfoEntity> implements FileInfoDAO {

  @Override
  public List<FileInfoEntity> findDeletedFiles(Date date) {
    return entities.values()
                   .stream()
                   .filter(entity -> entity.isDeleted() && (entity.getUpdatedDate() != null && entity.getUpdatedDate().before(date)))
                   .toList();
  }

  @Override
  public List<FileInfoEntity> findFilesByChecksum(String checksum) {
    return entities.values()
                   .stream()
                   .filter(entity -> StringUtils.equals(checksum, entity.getChecksum()))
                   .toList();
  }

  @Override
  public List<FileInfoEntity> findAllByPage(int offset, int limit) {
    return entities.values()
                   .stream()
                   .skip(offset)
                   .limit(limit)
                   .toList();
  }

}
