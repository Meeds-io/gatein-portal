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

import java.util.HashMap;
import java.util.Map;

import com.ibatis.common.util.PaginatedList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.domain.LineItem;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.iface.ItemDao;

public class ItemSqlMapDao extends BaseSqlMapDao implements ItemDao {

    public ItemSqlMapDao(DaoManager daoManager) {
        super(daoManager);
    }

    public void updateAllQuantitiesFromOrder(Order order) {
        for (int i = 0; i < order.getLineItems().size(); i++) {
            LineItem lineItem = (LineItem) order.getLineItems().get(i);
            String itemId = lineItem.getItemId();
            Integer increment = new Integer(lineItem.getQuantity());
            Map param = new HashMap(2);
            param.put("itemId", itemId);
            param.put("increment", increment);
            update("updateInventoryQuantity", param);
        }
    }

    public boolean isItemInStock(String itemId) {
        Integer i = (Integer) queryForObject("getInventoryQuantity", itemId);
        return (i != null && i.intValue() > 0);
    }

    public PaginatedList getItemListByProduct(String productId) {
        return queryForPaginatedList("getItemListByProduct", productId, PAGE_SIZE);
    }

    public Item getItem(String itemId) {
        Integer i = (Integer) queryForObject("getInventoryQuantity", itemId);
        Item item = (Item) queryForObject("getItem", itemId);
        item.setQuantity(i.intValue());
        return item;
    }

}
