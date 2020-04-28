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

import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.Account;
import com.ibatis.jpetstore.persistence.iface.AccountDao;

public class AccountSqlMapDao extends BaseSqlMapDao implements AccountDao {

    public AccountSqlMapDao(DaoManager daoManager) {
        super(daoManager);
    }

    public Account getAccount(String username) {
        return (Account) queryForObject("getAccountByUsername", username);
    }

    public Account getAccount(String username, String password) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        return (Account) queryForObject("getAccountByUsernameAndPassword", account);
    }

    public void insertAccount(Account account) {
        update("insertAccount", account);
        update("insertProfile", account);
        update("insertSignon", account);
    }

    public void updateAccount(Account account) {
        update("updateAccount", account);
        update("updateProfile", account);

        if (account.getPassword() != null && account.getPassword().length() > 0) {
            update("updateSignon", account);
        }
    }

}
