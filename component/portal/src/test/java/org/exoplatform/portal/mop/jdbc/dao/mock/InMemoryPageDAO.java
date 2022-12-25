/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package org.exoplatform.portal.mop.jdbc.dao.mock;

import java.util.List;

import org.gatein.api.page.PageQuery;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.portal.jdbc.entity.PageEntity;
import org.exoplatform.portal.mop.jdbc.dao.PageDAO;
import org.exoplatform.portal.mop.page.PageKey;

public class InMemoryPageDAO implements PageDAO {

  @Override
  public Long count() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageEntity find(Long id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PageEntity> findAll() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageEntity create(PageEntity entity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void createAll(List<PageEntity> entities) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PageEntity update(PageEntity entity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void updateAll(List<PageEntity> entities) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void delete(PageEntity entity) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteAll(List<PageEntity> entities) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteAll() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public PageEntity findByKey(PageKey pageKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ListAccess<PageEntity> findByQuery(PageQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteByOwner(long id) {
    // TODO Auto-generated method stub
    
  }

}
