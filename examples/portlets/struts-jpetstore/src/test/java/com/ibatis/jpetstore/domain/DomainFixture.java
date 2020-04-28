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
package com.ibatis.jpetstore.domain;

import java.math.BigDecimal;
import java.util.Date;


public class DomainFixture {

    public static Account newTestAccount() {
        Account account = new Account();

        account.setUsername("cbegin");
        account.setPassword("PASSWORD");

        account.setFirstName("Clinton");
        account.setLastName("Begin");

        account.setAddress1("123 Some Street");
        account.setAddress2("Apt B");
        account.setCity("Calgary");
        account.setState("AB");
        account.setCountry("Canada");
        account.setZip("90210");

        account.setEmail("someone@somewhere.com");
        account.setPhone("403.555.5555");

        account.setLanguagePreference("ENGLISH");
        account.setBannerName("DOGS");
        account.setBannerOption(true);
        account.setFavouriteCategoryId("DOGS");
        account.setListOption(true);
        account.setStatus("ACTIVE");

        return account;
    }

    public static Order newTestOrder() {
        Item item = new Item();
        item.setItemId("EST-2");

        LineItem lineItem = new LineItem();
        lineItem.setQuantity(100001);
        lineItem.setItem(item);
        lineItem.setItemId(item.getItemId());
        lineItem.setUnitPrice(new BigDecimal("99.99"));

        Order order = new Order();
        order.addLineItem(lineItem);

        order.setBillAddress1("123 Some Street");
        order.setBillAddress2("Apt B");
        order.setBillCity("Calgary");
        order.setBillCountry("Canada");
        order.setBillState("AB");
        order.setBillToFirstName("Clinton");
        order.setBillToLastName("Begin");
        order.setBillZip("12345");

        order.setShipAddress1("123 Some Street");
        order.setShipAddress2("Apt B");
        order.setShipCity("Calgary");
        order.setShipCountry("Canada");
        order.setShipState("AB");
        order.setShipToFirstName("Clinton");
        order.setShipToLastName("Begin");
        order.setShipZip("12345");

        order.setCardType("VISA");
        order.setCreditCard("1234-1123-1123");
        order.setExpiryDate("11/02");
        order.setLocale("CA");
        order.setCourier("B");
        order.setOrderDate(new Date());
        order.setStatus("A");
        order.setUsername("j2ee");
        order.setTotalPrice(new BigDecimal("99.99"));

        return order;
    }

}
