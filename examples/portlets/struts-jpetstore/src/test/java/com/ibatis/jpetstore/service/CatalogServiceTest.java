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
package com.ibatis.jpetstore.service;

import java.util.ArrayList;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import com.ibatis.common.util.PaginatedArrayList;
import com.ibatis.jpetstore.domain.Category;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.domain.Product;
import com.ibatis.jpetstore.persistence.iface.CategoryDao;
import com.ibatis.jpetstore.persistence.iface.ItemDao;
import com.ibatis.jpetstore.persistence.iface.ProductDao;

public class CatalogServiceTest extends MockObjectTestCase {

    public void testShouldCallGetCategoryOnCategoryDao() {

        Mock mock = mock(CategoryDao.class);

        mock.expects(once()).method("getCategory").with(NOT_NULL).will(returnValue(new Category()));

        CatalogService service = new CatalogService((CategoryDao) mock.proxy(), null, null);
        service.getCategory("DOGS");

    }

    public void testShouldCallGetCategoryListOnCategoryDao() {

        Mock mock = mock(CategoryDao.class);

        mock.expects(once()).method("getCategoryList").withNoArguments().will(returnValue(new ArrayList()));

        CatalogService service = new CatalogService((CategoryDao) mock.proxy(), null, null);
        service.getCategoryList();

    }

    public void testShouldCallGetItemOnItemDao() {
        Mock mock = mock(ItemDao.class);

        mock.expects(once()).method("getItem").with(NOT_NULL).will(returnValue(new Item()));

        CatalogService service = new CatalogService(null, (ItemDao) mock.proxy(), null);
        service.getItem("EST-1");

    }

    public void testShouldCallGetItemListByProductOnItemDao() {

        Mock mock = mock(ItemDao.class);

        mock.expects(once()).method("getItemListByProduct").with(NOT_NULL).will(returnValue(new PaginatedArrayList(5)));

        CatalogService service = new CatalogService(null, (ItemDao) mock.proxy(), null);
        service.getItemListByProduct("FI-SW-01");

    }

    public void testShouldCallGetProductOnProductDao() {

        Mock mock = mock(ProductDao.class);

        mock.expects(once()).method("getProduct").with(NOT_NULL).will(returnValue(new Product()));

        CatalogService service = new CatalogService(null, null, (ProductDao) mock.proxy());
        service.getProduct("FI-SW-01");

    }

    public void testShouldCallGetProductListByCategoryOnProductDao() {

        Mock mock = mock(ProductDao.class);

        mock.expects(once()).method("getProductListByCategory").with(NOT_NULL).will(returnValue(new PaginatedArrayList(5)));

        CatalogService service = new CatalogService(null, null, (ProductDao) mock.proxy());
        service.getProductListByCategory("DOGS");

    }

    public void testShouldFindProductIsInStock() {
        Mock mock = mock(ItemDao.class);

        mock.expects(once()).method("isItemInStock").with(NOT_NULL).will(returnValue(true));

        CatalogService service = new CatalogService(null, (ItemDao) mock.proxy(), null);

        assertTrue("Expected item to be in stock.", service.isItemInStock("EST-1"));

    }

    public void testCallSearchProductsOnProductDao() {
        Mock mock = mock(ProductDao.class);

        mock.expects(once()).method("searchProductList").with(NOT_NULL).will(returnValue(new PaginatedArrayList(5)));

        CatalogService service = new CatalogService(null, null, (ProductDao) mock.proxy());
        service.searchProductList("dog");

    }

}
