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
package com.ibatis.jpetstore.persistence;

import java.math.BigDecimal;

import com.ibatis.jpetstore.domain.DomainFixture;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.iface.OrderDao;

public class OrderDaoTest extends BasePersistenceTest {

    private OrderDao orderDao = (OrderDao) daoMgr.getDao(OrderDao.class);

    private static final String USERNAME = "NewUsername";
    private static final String SEQUENCE_NAME = "ordernum";

    public void testShouldInsertNewOrderWithLineItems() {
        Order expected = DomainFixture.newTestOrder();
        int nextId = 900001;
        expected.setOrderId(nextId);
        orderDao.insertOrder(expected);
        Order actual = orderDao.getOrder(nextId);
        assertNotNull(actual);
        assertEquals(1, actual.getLineItems().size());
        assertEquals(new BigDecimal("99.99"), actual.getTotalPrice());
    }

    public void testShouldListASingleOrderForAUser() {
        Order expected = DomainFixture.newTestOrder();
        int nextId = 900002;
        expected.setOrderId(nextId);
        expected.setUsername(USERNAME);
        orderDao.insertOrder(expected);
        assertEquals(1, orderDao.getOrdersByUsername(USERNAME).size());
    }

}
