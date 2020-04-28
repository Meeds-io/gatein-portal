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

import org.apache.struts.beanaction.ActionContext;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import com.ibatis.jpetstore.domain.Cart;
import com.ibatis.jpetstore.domain.CartItem;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.service.CatalogService;

public class CartBeanTest extends MockObjectTestCase {

    public void testShouldSuccessfullyReturnFromViewCart() {
        Mock catalogServiceMock = mock(CatalogService.class);
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        assertEquals(AbstractBean.SUCCESS, bean.viewCart());
    }

    public void testShouldSwitchPagesBackAndForth() {
        Mock catalogServiceMock = mock(CatalogService.class);
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        assertEquals(AbstractBean.SUCCESS, bean.viewCart());

        Cart cart = new Cart();
        for (int i = 0; i < cart.getCartItemList().getPageSize() * 2; i++) {
            cart.getCartItemList().add(new Item());
        }
        bean.setCart(cart);
        bean.setPageDirection("next");
        assertEquals(AbstractBean.SUCCESS, bean.switchCartPage());
        assertEquals(1, cart.getCartItemList().getPageIndex());
        bean.setPageDirection("previous");
        assertEquals(AbstractBean.SUCCESS, bean.switchCartPage());
        assertEquals(0, cart.getCartItemList().getPageIndex());

    }

    public void testShouldClearAllCartData() {
        Mock catalogServiceMock = mock(CatalogService.class);
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        Cart cart = new Cart();
        bean.setCart(cart);
        bean.setWorkingItemId("not null");
        bean.setPageDirection("not null");
        bean.clear();
        assertFalse(cart == bean.getCart());
        assertNull(bean.getWorkingItemId());
        assertNull(bean.getPageDirection());
    }

    public void testShouldAddItemToCart() {
        Mock catalogServiceMock = mock(CatalogService.class);
        catalogServiceMock.expects(atLeastOnce()).method("isItemInStock").with(NOT_NULL).will(returnValue(true));
        Item item = new Item();
        item.setItemId("AnID");
        catalogServiceMock.expects(atLeastOnce()).method("getItem").with(NOT_NULL).will(returnValue(item));
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        bean.setWorkingItemId("SomeItem");
        assertEquals(AbstractBean.SUCCESS, bean.addItemToCart());
        CartItem cartItem = (CartItem) bean.getCart().getCartItemList().get(0);
        assertEquals(1, cartItem.getQuantity());
        assertEquals(AbstractBean.SUCCESS, bean.addItemToCart());
        assertEquals(2, cartItem.getQuantity());
    }

    public void testShouldFailToRemoveItemFromCart() {
        Mock catalogServiceMock = mock(CatalogService.class);
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        bean.setWorkingItemId("nonexistant");
        assertEquals(AbstractBean.FAILURE, bean.removeItemFromCart());
    }

    public void testShouldRemoveItemFromCart() {
        Mock catalogServiceMock = mock(CatalogService.class);
        catalogServiceMock.expects(atLeastOnce()).method("isItemInStock").with(NOT_NULL).will(returnValue(true));
        Item item = new Item();
        item.setItemId("AnID");
        catalogServiceMock.expects(atLeastOnce()).method("getItem").with(NOT_NULL).will(returnValue(item));
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        bean.setWorkingItemId("AnID");
        bean.addItemToCart();
        assertEquals(AbstractBean.SUCCESS, bean.removeItemFromCart());
    }

    public void testShouldUpdateCartQuantities() {
        Mock catalogServiceMock = mock(CatalogService.class);
        catalogServiceMock.expects(atLeastOnce()).method("isItemInStock").with(NOT_NULL).will(returnValue(true));
        Item item = new Item();
        item.setItemId("AnID");
        catalogServiceMock.expects(atLeastOnce()).method("getItem").with(NOT_NULL).will(returnValue(item));
        CartBean bean = new CartBean((CatalogService) catalogServiceMock.proxy());
        bean.setWorkingItemId("AnID");
        bean.addItemToCart();

        ActionContext.getActionContext().getParameterMap().put("AnID", "5");

        assertEquals(AbstractBean.SUCCESS, bean.updateCartQuantities());
        CartItem cartItem = (CartItem) bean.getCart().getCartItemList().get(0);
        assertEquals(5, cartItem.getQuantity());
    }

}
