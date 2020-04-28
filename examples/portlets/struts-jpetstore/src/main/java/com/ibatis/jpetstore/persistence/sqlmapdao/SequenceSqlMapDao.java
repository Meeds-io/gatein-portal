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

import com.ibatis.dao.client.DaoException;
import com.ibatis.dao.client.DaoManager;
import com.ibatis.jpetstore.domain.Sequence;
import com.ibatis.jpetstore.persistence.iface.SequenceDao;

public class SequenceSqlMapDao extends BaseSqlMapDao implements SequenceDao {

    public SequenceSqlMapDao(DaoManager daoManager) {
        super(daoManager);
    }

    /**
     * This is a generic sequence ID generator that is based on a database table called 'SEQUENCE', which contains two columns
     * (NAME, NEXTID).
     * <br>
     * This approach should work with any database.
     *
     * @param name The name of the sequence.
     * @return The Next ID @
     */
    public synchronized int getNextId(String name) {
        Sequence sequence = new Sequence(name, -1);

        sequence = (Sequence) queryForObject("getSequence", sequence);
        if (sequence == null) {
            throw new DaoException("Error: A null sequence was returned from the database (could not get next " + name
                    + " sequence).");
        }
        Object parameterObject = new Sequence(name, sequence.getNextId() + 1);
        update("updateSequence", parameterObject);

        return sequence.getNextId();
    }

}
