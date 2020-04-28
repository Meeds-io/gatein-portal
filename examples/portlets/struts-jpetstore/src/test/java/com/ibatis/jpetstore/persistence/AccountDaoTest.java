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

import com.ibatis.jpetstore.domain.Account;
import com.ibatis.jpetstore.domain.DomainFixture;
import com.ibatis.jpetstore.persistence.iface.AccountDao;

public class AccountDaoTest extends BasePersistenceTest {

    private AccountDao acctDao = (AccountDao) daoMgr.getDao(AccountDao.class);

    public void testShouldFindDefaultUserAccountByUsername() throws Exception {
        Account acct = acctDao.getAccount("j2ee");
        assertNotNull(acct);
    }

    public void testShouldFindDefaultUserAccountByUsernameAndPassword() throws Exception {
        Account acct = acctDao.getAccount("j2ee", "j2ee");
        assertNotNull(acct);
    }

    public void testShouldInsertNewAccount() throws Exception {
        Account acct = DomainFixture.newTestAccount();
        acctDao.insertAccount(acct);
        acct = acctDao.getAccount("cbegin");
        assertNotNull(acct);
    }

    public void testShouldUpdateAccountEmailAddress() throws Exception {
        String newEmail = "new@email.com";
        Account acct = acctDao.getAccount("j2ee");
        acct.setEmail(newEmail);
        acctDao.updateAccount(acct);
        acct = acctDao.getAccount("j2ee");
        assertNotNull(acct);
        assertEquals(newEmail, acct.getEmail());
    }

}
