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

import com.ibatis.jpetstore.domain.DomainFixture;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.domain.LineItem;
import com.ibatis.jpetstore.domain.Order;
import com.ibatis.jpetstore.persistence.iface.ItemDao;

public class ItemDaoTest extends BasePersistenceTest {

    private static final String MUTABLE_ITEM_ID = "EST-2";
    private static final String READ_ONLY_ITEM_ID = "EST-1";
    private static final String PRODUCT_ID = "FI-SW-01";

    private ItemDao itemDao = (ItemDao) daoMgr.getDao(ItemDao.class);

    public void testShouldFindItemByID() {
        assertNotNull(itemDao.getItem(READ_ONLY_ITEM_ID));
    }

    public void testShouldListTwoItemsForGivenProduct() {
        assertEquals(2, itemDao.getItemListByProduct(PRODUCT_ID).size());
    }

    public void testShouldVerifyItemIsInStock() {
        assertTrue("Expected item to be in stock.", itemDao.isItemInStock(READ_ONLY_ITEM_ID));
    }

    public void testShouldVerifyItemIsOutOfStock() {
        Order order = DomainFixture.newTestOrder();
        itemDao.updateAllQuantitiesFromOrder(order);
        assertFalse("Expected item to be out of stock.", itemDao.isItemInStock(MUTABLE_ITEM_ID));
    }

    public void testShouldUpdateInventoryForItem() {
        Item item = itemDao.getItem(MUTABLE_ITEM_ID);
        int inventory = item.getQuantity();
        Order order = DomainFixture.newTestOrder();
        inventory -= ((LineItem) order.getLineItems().get(0)).getQuantity();
        itemDao.updateAllQuantitiesFromOrder(order);
        item = itemDao.getItem(MUTABLE_ITEM_ID);
        assertEquals(inventory, item.getQuantity());
    }

}
