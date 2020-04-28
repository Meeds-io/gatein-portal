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

import com.ibatis.common.util.PaginatedList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.LineItem;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.iface.OrderDao;

public class OrderSqlMapDao extends BaseSqlMapDao implements OrderDao {

    public OrderSqlMapDao(DaoManager daoManager) {
        super(daoManager);
    }

    public PaginatedList getOrdersByUsername(String username) {
        return queryForPaginatedList("getOrdersByUsername", username, 10);
    }

    public Order getOrder(int orderId) {
        Order order = null;
        Object parameterObject = new Integer(orderId);
        order = (Order) queryForObject("getOrder", parameterObject);
        order.setLineItems(queryForList("getLineItemsByOrderId", new Integer(order.getOrderId())));
        return order;
    }

    public void insertOrder(Order order) {
        insert("insertOrder", order);
        insert("insertOrderStatus", order);
        for (int i = 0; i < order.getLineItems().size(); i++) {
            LineItem lineItem = (LineItem) order.getLineItems().get(i);
            lineItem.setOrderId(order.getOrderId());
            insert("insertLineItem", lineItem);
        }
    }

}
