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

import com.ibatis.common.util.PaginatedList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.LineItem;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.DaoConfig;
import com.ibatis.jpetstore.persistence.iface.ItemDao;
import com.ibatis.jpetstore.persistence.iface.OrderDao;
import com.ibatis.jpetstore.persistence.iface.SequenceDao;

public class OrderService {

    private DaoManager daoManager;

    private ItemDao itemDao;
    private OrderDao orderDao;
    private SequenceDao sequenceDao;

    public OrderService() {
        daoManager = DaoConfig.getDaoManager();
        itemDao = (ItemDao) daoManager.getDao(ItemDao.class);
        sequenceDao = (SequenceDao) daoManager.getDao(SequenceDao.class);
        orderDao = (OrderDao) daoManager.getDao(OrderDao.class);
    }

    public OrderService(DaoManager daoManager, ItemDao itemDao, OrderDao orderDao, SequenceDao sequenceDao) {
        this.daoManager = daoManager;
        this.itemDao = itemDao;
        this.orderDao = orderDao;
        this.sequenceDao = sequenceDao;
    }

    public void insertOrder(Order order) {
        try {
            // Get the next id within a separate transaction
            order.setOrderId(getNextId("ordernum"));

            daoManager.startTransaction();

            itemDao.updateAllQuantitiesFromOrder(order);
            orderDao.insertOrder(order);

            daoManager.commitTransaction();
        } finally {
            daoManager.endTransaction();
        }
    }

    public Order getOrder(int orderId) {
        Order order = null;

        try {
            daoManager.startTransaction();

            order = orderDao.getOrder(orderId);

            for (int i = 0; i < order.getLineItems().size(); i++) {
                LineItem lineItem = (LineItem) order.getLineItems().get(i);
                lineItem.setItem(itemDao.getItem(lineItem.getItemId()));
            }

            daoManager.commitTransaction();
        } finally {
            daoManager.endTransaction();
        }

        return order;
    }

    public PaginatedList getOrdersByUsername(String username) {
        return orderDao.getOrdersByUsername(username);
    }

    public int getNextId(String key) {
        return sequenceDao.getNextId(key);
    }

}
