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
package com.ibatis.jpetstore.presentation;

import com.ibatis.common.util.PaginatedList;
import com.ibatis.jpetstore.domain.Category;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.domain.Product;
import com.ibatis.jpetstore.service.CatalogService;

public class CatalogBean extends AbstractBean {

    private CatalogService catalogService;

    private String keyword;
    private String pageDirection;

    private String categoryId;
    private Category category;
    private PaginatedList categoryList;

    private String productId;
    private Product product;
    private PaginatedList productList;

    private String itemId;
    private Item item;
    private PaginatedList itemList;

    public CatalogBean() {
        this(new CatalogService());
    }

    public CatalogBean(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void setPageDirection(String pageDirection) {
        this.pageDirection = pageDirection;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public PaginatedList getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(PaginatedList categoryList) {
        this.categoryList = categoryList;
    }

    public PaginatedList getProductList() {
        return productList;
    }

    public void setProductList(PaginatedList productList) {
        this.productList = productList;
    }

    public PaginatedList getItemList() {
        return itemList;
    }

    public void setItemList(PaginatedList itemList) {
        this.itemList = itemList;
    }

    public String viewCategory() {
        if (categoryId != null) {
            productList = catalogService.getProductListByCategory(categoryId);
            category = catalogService.getCategory(categoryId);
        }
        return SUCCESS;
    }

    public String viewProduct() {
        if (productId != null) {
            itemList = catalogService.getItemListByProduct(productId);
            product = catalogService.getProduct(productId);
        }
        return SUCCESS;
    }

    public String viewItem() {
        item = catalogService.getItem(itemId);
        product = item.getProduct();
        return SUCCESS;
    }

    public String searchProducts() {
        if (keyword == null || keyword.length() < 1) {
            setMessage("Please enter a keyword to search for, then press the search button.");
            return FAILURE;
        } else {
            productList = catalogService.searchProductList(keyword.toLowerCase());
            return SUCCESS;
        }
    }

    public String switchProductListPage() {
        if ("next".equals(pageDirection)) {
            productList.nextPage();
        } else if ("previous".equals(pageDirection)) {
            productList.previousPage();
        }
        return SUCCESS;
    }

    public String switchItemListPage() {
        if ("next".equals(pageDirection)) {
            itemList.nextPage();
        } else if ("previous".equals(pageDirection)) {
            itemList.previousPage();
        }
        return SUCCESS;
    }

    public void clear() {
        keyword = null;
        pageDirection = null;

        categoryId = null;
        category = null;
        categoryList = null;

        productId = null;
        product = null;
        productList = null;

        itemId = null;
        item = null;
        itemList = null;
    }

}
