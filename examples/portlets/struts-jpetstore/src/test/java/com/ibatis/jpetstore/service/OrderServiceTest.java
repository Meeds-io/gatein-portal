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

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import com.ibatis.common.util.PaginatedArrayList;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.DomainFixture;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.iface.ItemDao;
import com.ibatis.jpetstore.persistence.iface.OrderDao;
import com.ibatis.jpetstore.persistence.iface.SequenceDao;

public class OrderServiceTest extends MockObjectTestCase {

    public void testShouldCallGetOrderOnOrderDao() {

        Mock orderDao = mock(OrderDao.class);
        orderDao.expects(once()).method("getOrder").with(NOT_NULL).will(returnValue(new Order()));

        Mock daoManager = mock(DaoManager.class);
        daoManager.expects(once()).method("startTransaction").withNoArguments();

        daoManager.expects(once()).method("commitTransaction").withNoArguments();

        daoManager.expects(once()).method("endTransaction").withNoArguments();

        OrderService service = new OrderService((DaoManager) daoManager.proxy(), null, (OrderDao) orderDao.proxy(), null);
        service.getOrder(1);

    }

    public void testShouldCallGetOrdersByUsernameOnOrderDao() {
        Mock orderDao = mock(OrderDao.class);
        orderDao.expects(once()).method("getOrdersByUsername").with(NOT_NULL).will(returnValue(new PaginatedArrayList(5)));
        OrderService service = new OrderService(null, null, (OrderDao) orderDao.proxy(), null);
        service.getOrdersByUsername("j2ee");
    }

    public void testShouldCallInsertOrderOnOrderDao() {

        Mock seqDao = mock(SequenceDao.class);
        seqDao.expects(once()).method("getNextId").with(NOT_NULL).will(returnValue(1));

        Mock orderDao = mock(OrderDao.class);
        orderDao.expects(once()).method("insertOrder").with(NOT_NULL);

        Mock itemDao = mock(ItemDao.class);
        itemDao.expects(once()).method("updateAllQuantitiesFromOrder").with(NOT_NULL);

        Mock daoManager = mock(DaoManager.class);
        daoManager.expects(once()).method("startTransaction").withNoArguments();

        daoManager.expects(once()).method("commitTransaction").withNoArguments();

        daoManager.expects(once()).method("endTransaction").withNoArguments();

        OrderService service = new OrderService((DaoManager) daoManager.proxy(), (ItemDao) itemDao.proxy(),
                (OrderDao) orderDao.proxy(), (SequenceDao) seqDao.proxy());
        service.insertOrder(DomainFixture.newTestOrder());

    }

    public void testShouldCallGetNextIdOnSequenceDao() {

        Mock seqDao = mock(SequenceDao.class);
        seqDao.expects(once()).method("getNextId").with(NOT_NULL).will(returnValue(1));

        OrderService service = new OrderService(null, null, null, (SequenceDao) seqDao.proxy());
        service.getNextId("ordernum");

    }

}
