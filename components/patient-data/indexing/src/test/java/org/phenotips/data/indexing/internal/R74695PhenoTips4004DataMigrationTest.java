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
package org.phenotips.data.indexing.internal;

import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

public class R74695PhenoTips4004DataMigrationTest
{
    @Rule
    public MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<>(R74695PhenoTips4004DataMigration.class,
            HibernateDataMigration.class);

    @Mock
    private Session session;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void reindexIsCalledDuringMigration()
        throws XWikiException, HibernateException, ComponentLookupException
    {
        ((HibernateCallback<?>) this.mocker.getComponentUnderTest()).doInHibernate(this.session);
        Mockito.verify(this.mocker.<PatientIndexer>getInstance(PatientIndexer.class)).reindex();
    }

    @Test
    public void reindexErrorsAreNotPropagated()
        throws XWikiException, HibernateException, ComponentLookupException
    {
        PatientIndexer indexer = this.mocker.<PatientIndexer>getInstance(PatientIndexer.class);
        Mockito.doThrow(new NullPointerException()).when(indexer).reindex();
        Assert.assertNull(((HibernateCallback<?>) this.mocker.getComponentUnderTest()).doInHibernate(this.session));
    }

    @Test
    public void correctVersionIsUsed() throws ComponentLookupException
    {
        Assert.assertEquals(74695, this.mocker.getComponentUnderTest().getVersion().getVersion());
    }

    @Test
    public void hasCorrectDescription() throws ComponentLookupException
    {
        Assert.assertEquals("Trigger re-indexing for all patients.",
            this.mocker.getComponentUnderTest().getDescription());
    }
}
