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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Provider;

import org.hibernate.HibernateException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link UserType}.
 *
 * @version $Id$
 * @since 1.4
 */
@NotThreadSafe
public class UserTypeTest
{
    private static ComponentManager cm;

    private static UserManager users;

    private static User user;

    @Mock
    private ResultSet rs;

    private String[] names = new String[] { "user" };

    private UserType type = new UserType();

    @Mock
    private PreparedStatement st;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setupCMR()
        throws ComponentLookupException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        synchronized (ComponentManagerRegistry.class) {
            Provider<ComponentManager> cmp;
            Field f = ReflectionUtils.getField(ComponentManagerRegistry.class, "cmProvider");
            f.setAccessible(true);
            cmp = (Provider<ComponentManager>) f.get(null);
            if (cmp == null) {
                cmp = Mockito.mock(Provider.class);
                ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", cmp);
                cm = Mockito.mock(ComponentManager.class);
                Mockito.when(cmp.get()).thenReturn(cm);
            } else {
                cm = cmp.get();
            }

            users = Mockito.mock(UserManager.class);
            Mockito.when(cm.getInstance(UserManager.class)).thenReturn(users);
            user = Mockito.mock(User.class);
            Mockito.when(users.getUser("wiki:XWiki.user", true)).thenReturn(user);
            Mockito.when(user.getId()).thenReturn("wiki:XWiki.user");
        }
    }

    @Before
    public void setup() throws SQLException
    {
        MockitoAnnotations.initMocks(this);
        Mockito.when(this.rs.getString("user")).thenReturn("wiki:XWiki.user");
    }

    @Test
    public void storesAsVarChar()
    {
        int[] types = this.type.sqlTypes();
        Assert.assertEquals(1, types.length);
        Assert.assertEquals(Types.VARCHAR, types[0]);
    }

    @Test
    public void handlesUser()
    {
        Assert.assertEquals(User.class, this.type.returnedClass());
    }

    @Test
    public void assembleResolvesStoredStrings() throws HibernateException, SQLException
    {
        User result = (User) this.type.assemble("wiki:XWiki.user", null);
        Assert.assertEquals(user, result);
    }

    @Test
    public void assembleReturnsNullForEmptyString() throws HibernateException, SQLException
    {
        Assert.assertNull(this.type.assemble("", null));
    }

    @Test
    public void assembleReturnsNullForOtherValues() throws HibernateException, SQLException
    {
        Assert.assertNull(this.type.assemble(true, null));
    }

    @Test
    public void getResolvesStoredStrings() throws HibernateException, SQLException
    {
        User result = (User) this.type.nullSafeGet(this.rs, this.names, null);
        Assert.assertEquals(user, result);
    }

    @Test
    public void disassembleSerializesReference() throws HibernateException, SQLException
    {
        Assert.assertEquals("wiki:XWiki.user", this.type.disassemble(UserTypeTest.user));
    }

    @Test
    public void setSerializesReference() throws HibernateException, SQLException
    {
        this.type.nullSafeSet(this.st, UserTypeTest.user, 0);
        Mockito.verify(this.st).setString(0, "wiki:XWiki.user");
    }

    @Test
    public void setStoresNullForNullValue() throws HibernateException, SQLException
    {
        this.type.nullSafeSet(this.st, null, 0);
        Mockito.verify(this.st).setNull(0, Types.VARCHAR);
    }

    @Test
    public void setStoresNullForOtherValues() throws HibernateException, SQLException
    {
        this.type.nullSafeSet(this.st, "reference", 0);
        Mockito.verify(this.st).setNull(0, Types.VARCHAR);
    }

    @Test
    public void isNotMutable()
    {
        Assert.assertFalse(this.type.isMutable());
    }

    @Test
    public void deepCopyJustReturnsValue()
    {
        Assert.assertSame(UserTypeTest.user, this.type.deepCopy(user));
    }

    @Test
    public void replaceJustReturnsOriginal()
    {
        Assert.assertSame(UserTypeTest.user, this.type.replace(user, "abc", null));
    }

    @Test
    public void equalsForwardsCall()
    {
        User other = Mockito.mock(User.class);
        Assert.assertTrue(this.type.equals(user, user));
        Assert.assertFalse(this.type.equals(other, user));
        Assert.assertFalse(this.type.equals("wiki:Space.Page", user));
        Assert.assertFalse(this.type.equals(user, "wiki:Space.Page"));
        Assert.assertFalse(this.type.equals("wiki:Space.Page", "wiki:Space.Page"));
    }

    @Test
    public void hashCodeForwardsCall()
    {
        Assert.assertEquals(user.hashCode(), this.type.hashCode(user));
        Assert.assertEquals(0, this.type.hashCode("str"));
    }
}
