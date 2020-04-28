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

import java.util.Iterator;
import java.util.Map;

import org.apache.struts.beanaction.ActionContext;

import com.ibatis.jpetstore.domain.Cart;
import com.ibatis.jpetstore.domain.CartItem;
import com.ibatis.jpetstore.domain.Item;
import com.ibatis.jpetstore.service.CatalogService;

public class CartBean extends AbstractBean {

    private CatalogService catalogService;

    private Cart cart = new Cart();
    private String workingItemId;
    private String pageDirection;

    public CartBean() {
        this(new CatalogService());
    }

    public CartBean(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public String getWorkingItemId() {
        return workingItemId;
    }

    public void setWorkingItemId(String workingItemId) {
        this.workingItemId = workingItemId;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void setPageDirection(String pageDirection) {
        this.pageDirection = pageDirection;
    }

    public String addItemToCart() {
        if (cart.containsItemId(workingItemId)) {
            cart.incrementQuantityByItemId(workingItemId);
        } else {
            // isInStock is a "real-time" property that must be updated
            // every time an item is added to the cart, even if other
            // item details are cached.
            boolean isInStock = catalogService.isItemInStock(workingItemId);
            Item item = catalogService.getItem(workingItemId);
            cart.addItem(item, isInStock);
        }

        return SUCCESS;
    }

    public String removeItemFromCart() {

        Item item = cart.removeItemById(workingItemId);

        if (item == null) {
            setMessage("Attempted to remove null CartItem from Cart.");
            return FAILURE;
        } else {
            return SUCCESS;
        }
    }

    public String updateCartQuantities() {
        Map parameterMap = ActionContext.getActionContext().getParameterMap();

        Iterator cartItems = getCart().getAllCartItems();
        while (cartItems.hasNext()) {
            CartItem cartItem = (CartItem) cartItems.next();
            String itemId = cartItem.getItem().getItemId();
            try {
                int quantity = Integer.parseInt((String) parameterMap.get(itemId));
                getCart().setQuantityByItemId(itemId, quantity);
                if (quantity < 1) {
                    cartItems.remove();
                }
            } catch (Exception e) {
                // ignore parse exceptions on purpose
            }
        }

        return SUCCESS;
    }

    public String switchCartPage() {
        if ("next".equals(pageDirection)) {
            cart.getCartItemList().nextPage();
        } else if ("previous".equals(pageDirection)) {
            cart.getCartItemList().previousPage();
        }
        return SUCCESS;
    }

    public String viewCart() {
        return SUCCESS;
    }

    public void clear() {
        cart = new Cart();
        workingItemId = null;
        pageDirection = null;
    }

}
