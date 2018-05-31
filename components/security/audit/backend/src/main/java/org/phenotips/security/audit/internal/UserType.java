/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.security.audit.internal;

import org.phenotips.components.ComponentManagerRegistry;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;

/**
 * Custom Hibernate type used for storing user references in a database.
 *
 * @version $Id$
 * @since 1.4
 */
public class UserType implements org.hibernate.usertype.UserType
{
    @Override
    public int[] sqlTypes()
    {
        return new int[] { Types.VARCHAR };
    }

    @Override
    public Class<?> returnedClass()
    {
        return User.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException
    {
        if (User.class.isInstance(x) && User.class.isInstance(y)) {
            return x.equals(y);
        }
        return false;
    }

    @Override
    public int hashCode(Object x) throws HibernateException
    {
        if (User.class.isInstance(x)) {
            User reference = (User) x;
            return reference.hashCode();
        }
        return 0;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException
    {
        String serializedReference = rs.getString(names[0]);
        return resolve(serializedReference);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException
    {
        if (User.class.isInstance(value)) {
            st.setString(index, ((User) value).getId());
        } else {
            st.setNull(index, Types.VARCHAR);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException
    {
        return ((User) value).getId();
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException
    {
        if (String.class.isInstance(cached)) {
            return resolve((String) cached);
        }
        return null;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException
    {
        return original;
    }

    private User resolve(String serializedReference)
    {
        if (StringUtils.isNotEmpty(serializedReference)) {
            try {
                UserManager resolver =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(UserManager.class);
                return resolver.getUser(serializedReference, true);
            } catch (ComponentLookupException ex) {
                // This really shouldn't happen...
            }
        }
        return null;
    }
}
