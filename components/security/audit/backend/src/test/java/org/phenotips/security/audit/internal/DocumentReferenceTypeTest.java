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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

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
 * Tests for {@link DocumentReferenceType}.
 *
 * @version $Id$
 * @since 1.4
 */
@NotThreadSafe
public class DocumentReferenceTypeTest
{
    private static ComponentManager cm;

    private static DocumentReferenceResolver<String> resolver;

    private static DocumentReference docReference = new DocumentReference("wiki", "Space", "Page");

    @Mock
    private ResultSet rs;

    private String[] names = new String[] { "reference" };

    private DocumentReferenceType type = new DocumentReferenceType();

    @Mock
    private PreparedStatement st;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setupCMR()
        throws ComponentLookupException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException
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
            resolver = Mockito.mock(DocumentReferenceResolver.class);
            Mockito.when(cm.getInstance(DocumentReferenceResolver.TYPE_STRING)).thenReturn(resolver);
            Mockito.when(resolver.resolve("wiki:Space.Page")).thenReturn(docReference);
        }
    }

    @Before
    public void setup() throws SQLException
    {
        MockitoAnnotations.initMocks(this);
        Mockito.when(this.rs.getString("reference")).thenReturn("wiki:Space.Page");
    }

    @Test
    public void storesAsVarChar()
    {
        int[] types = this.type.sqlTypes();
        Assert.assertEquals(1, types.length);
        Assert.assertEquals(Types.VARCHAR, types[0]);
    }

    @Test
    public void handlesDocumentReference()
    {
        Assert.assertEquals(DocumentReference.class, this.type.returnedClass());
    }

    @Test
    public void assembleResolvesStoredStrings() throws HibernateException, SQLException
    {
        DocumentReference result = (DocumentReference) this.type.assemble("wiki:Space.Page", null);
        Assert.assertEquals(docReference, result);
    }

    @Test
    public void assembleReturnsNullForEmptyString() throws HibernateException, SQLException
    {
        Assert.assertNull(this.type.assemble("", null));
    }

    @Test
    public void assembleReturnsNullForOtherValues() throws HibernateException, SQLException
    {
        Assert.assertNull(this.type.assemble(DocumentReferenceTypeTest.docReference, null));
    }

    @Test
    public void getResolvesStoredStrings() throws HibernateException, SQLException
    {
        DocumentReference result = (DocumentReference) this.type.nullSafeGet(this.rs, this.names, null);
        Assert.assertEquals(docReference, result);
    }

    @Test
    public void disassembleSerializesReference() throws HibernateException, SQLException
    {
        Assert.assertEquals("wiki:Space.Page", this.type.disassemble(DocumentReferenceTypeTest.docReference));
    }

    @Test
    public void setSerializesReference() throws HibernateException, SQLException
    {
        this.type.nullSafeSet(this.st, DocumentReferenceTypeTest.docReference, 0);
        Mockito.verify(this.st).setString(0, "wiki:Space.Page");
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
        Assert.assertSame(DocumentReferenceTypeTest.docReference, this.type.deepCopy(docReference));
    }

    @Test
    public void replaceJustReturnsOriginal()
    {
        Assert.assertSame(DocumentReferenceTypeTest.docReference, this.type.replace(docReference, "abc", null));
    }

    @Test
    public void equalsForwardsCall()
    {
        Assert.assertTrue(this.type.equals(new DocumentReference("wiki", "Space", "Page"), docReference));
        Assert.assertFalse(this.type.equals(new DocumentReference("wiki", "Space", "OtherPage"), docReference));
        Assert.assertFalse(this.type.equals("wiki:Space.Page", docReference));
        Assert.assertFalse(this.type.equals(docReference, "wiki:Space.Page"));
        Assert.assertFalse(this.type.equals("wiki:Space.Page", "wiki:Space.Page"));
    }

    @Test
    public void hashCodeForwardsCall()
    {
        Assert.assertEquals(docReference.hashCode(), this.type.hashCode(docReference));
        Assert.assertEquals(0, this.type.hashCode("str"));
    }
}
