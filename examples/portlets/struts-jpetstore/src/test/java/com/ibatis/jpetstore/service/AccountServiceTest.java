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

import com.ibatis.jpetstore.domain.Account;
import com.ibatis.jpetstore.domain.DomainFixture;
import com.ibatis.jpetstore.persistence.iface.AccountDao;

public class AccountServiceTest extends MockObjectTestCase {

    public void testShouldVerifyGetAccountIsCalledByUsername() {
        Mock mock = mock(AccountDao.class);

        mock.expects(once()).method("getAccount").with(NOT_NULL).will(returnValue(new Account()));

        AccountService accountService = new AccountService((AccountDao) mock.proxy());
        accountService.getAccount("cbegin");
    }

    public void testShouldVerifyGetAccountIsCalledByUsernameAndPassword() {
        Mock mock = mock(AccountDao.class);

        mock.expects(once()).method("getAccount").with(NOT_NULL, NOT_NULL).will(returnValue(new Account()));

        AccountService accountService = new AccountService((AccountDao) mock.proxy());
        accountService.getAccount("cbegin", "PASSWORD");
    }

    public void testShouldVerifyInsertAccountIsCalled() {
        Mock mock = mock(AccountDao.class);

        mock.expects(once()).method("insertAccount").with(NOT_NULL);

        AccountService accountService = new AccountService((AccountDao) mock.proxy());
        accountService.insertAccount(DomainFixture.newTestAccount());
    }

    public void testShouldVerifyUpdateAccountIsCalled() {
        Mock mock = mock(AccountDao.class);

        mock.expects(once()).method("updateAccount").with(NOT_NULL);

        AccountService accountService = new AccountService((AccountDao) mock.proxy());
        accountService.updateAccount(DomainFixture.newTestAccount());
    }

}
