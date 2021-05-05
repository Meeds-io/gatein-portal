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
