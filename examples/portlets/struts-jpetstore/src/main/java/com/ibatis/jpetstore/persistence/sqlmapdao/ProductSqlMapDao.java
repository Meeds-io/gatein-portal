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
package com.ibatis.jpetstore.persistence.sqlmapdao;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.ibatis.common.util.PaginatedList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.Product;
import com.ibatis.jpetstore.persistence.iface.ProductDao;

public class ProductSqlMapDao extends BaseSqlMapDao implements ProductDao {

    public ProductSqlMapDao(DaoManager daoManager) {
        super(daoManager);
    }

    public PaginatedList getProductListByCategory(String categoryId) {
        return queryForPaginatedList("getProductListByCategory", categoryId, PAGE_SIZE);
    }

    public Product getProduct(String productId) {
        return (Product) queryForObject("getProduct", productId);
    }

    public PaginatedList searchProductList(String keywords) {
        Object parameterObject = new ProductSearch(keywords);
        return queryForPaginatedList("searchProductList", parameterObject, PAGE_SIZE);
    }

    /* Inner Classes */

    public static class ProductSearch {
        private List keywordList = new ArrayList();

        public ProductSearch(String keywords) {
            StringTokenizer splitter = new StringTokenizer(keywords, " ", false);
            while (splitter.hasMoreTokens()) {
                keywordList.add("%" + splitter.nextToken() + "%");
            }
        }

        public List getKeywordList() {
            return keywordList;
        }
    }

}
