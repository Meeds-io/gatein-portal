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

import java.util.List;

import com.ibatis.common.util.PaginatedList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.Category;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.domain.Product;
import com.ibatis.jpetstore.persistence.DaoConfig;
import com.ibatis.jpetstore.persistence.iface.CategoryDao;
import com.ibatis.jpetstore.persistence.iface.ItemDao;
import com.ibatis.jpetstore.persistence.iface.ProductDao;

public class CatalogService {

    private CategoryDao categoryDao;
    private ItemDao itemDao;
    private ProductDao productDao;

    public CatalogService() {
        DaoManager daoManager = DaoConfig.getDaoManager();
        categoryDao = (CategoryDao) daoManager.getDao(CategoryDao.class);
        productDao = (ProductDao) daoManager.getDao(ProductDao.class);
        itemDao = (ItemDao) daoManager.getDao(ItemDao.class);
    }

    public CatalogService(CategoryDao categoryDao, ItemDao itemDao, ProductDao productDao) {
        this.categoryDao = categoryDao;
        this.itemDao = itemDao;
        this.productDao = productDao;
    }

    public List getCategoryList() {
        return categoryDao.getCategoryList();
    }

    public Category getCategory(String categoryId) {
        return categoryDao.getCategory(categoryId);
    }

    public Product getProduct(String productId) {
        return productDao.getProduct(productId);
    }

    public PaginatedList getProductListByCategory(String categoryId) {
        return productDao.getProductListByCategory(categoryId);
    }

    public PaginatedList searchProductList(String keywords) {
        return productDao.searchProductList(keywords);
    }

    public PaginatedList getItemListByProduct(String productId) {
        return itemDao.getItemListByProduct(productId);
    }

    public Item getItem(String itemId) {
        return itemDao.getItem(itemId);
    }

    public boolean isItemInStock(String itemId) {
        return itemDao.isItemInStock(itemId);
    }

}
