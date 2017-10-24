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
package org.phenotips.studies.family.migrations;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;

import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link R71507henoTips3423DataMigration}.
 */
public class R71508PhenoTips3424DataMigrationTest
{
    private static final String ERROR_UPDATING_DATA = "Error updating pedigree data format for document {}: [{}]";

    private static final String SERIALIZED_ENTITY = "serialized_entity";

    private static final String WIKI_ID = "wikiID";

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String FAMILY_1 = "family1";

    // a sample pedigree in the auto-generated format created by old family-studies data conversion
    private static final String PEDIGREE_SIMPLE_JSON_DATA = ("{`data`:[{`id`:2},{`id`:3},{`mother`:`3`,`phenotipsId`:"
        + "`P0000001`,`sex`:`U`,`father`:`2`,`id`:1},{`mother`:3,`proband`:true,`phenotipsId`:`P0000003`,`father`:2,`"
        + "id`:4},{`id`:5},{`mother`:5,`phenotipsId`:`P0000002`,`father`:1,`id`:6}]}").replace('`', '"');

    private static final String PEDIGREE_MIGRATED_DATA = ("{`relationships`:[{`children`:[{`id`:6}],`members`:[1,5],`"
        + "id`:1},{`children`:[{`id`:1},{`id`:4}],`members`:[2,3],`id`:2}],`members`:[{`id`:2,`pedigreeProperties`:{`"
        + "gender`:`U`}},{`id`:3,`pedigreeProperties`:{`gender`:`U`}},{`id`:1,`pedigreeProperties`:{`gender`:`U`,`phe"
        + "notipsId`:`P0000001`}},{`id`:4,`pedigreeProperties`:{`gender`:`U`,`phenotipsId`:`P0000003`}},{`id`:5,`pedi"
        + "greeProperties`:{`gender`:`U`}},{`id`:6,`pedigreeProperties`:{`gender`:`U`,`phenotipsId`:`P0000002`}}],`JS"
        + "ON_version`:`1.0`,`proband`:4}").replace('`', '"');

    private static final String PEDIGREE_NEW_FORMAT_DATA = PEDIGREE_MIGRATED_DATA;

    @Rule
    public MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<>(R71508PhenoTips3424DataMigration.class, HibernateDataMigration.class,
            "R71508-PT-3424");

    @Mock
    private Session session;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Query query;

    @Mock
    private DocumentReference documentReference1;

    @Mock
    private BaseObject pedigreeBaseObject1;

    @Mock
    private XWikiDocument xDocument1;

    @Mock
    private XWikiHibernateStore store;

    private R71508PhenoTips3424DataMigration component;

    private Logger logger;

    private ComponentManager componentManager;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.mocker.registerMockComponent(ComponentManager.class);
        this.componentManager = this.mocker.getInstance(ComponentManager.class);
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(this.store);

        this.component = (R71508PhenoTips3424DataMigration) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final EntityReferenceSerializer<String> serializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(serializer.serialize(any(EntityReference.class))).thenReturn(SERIALIZED_ENTITY);

        final Execution execution = this.mocker.getInstance(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty(anyString())).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.context.getWikiId()).thenReturn(WIKI_ID);

        when(this.session.createQuery(anyString())).thenReturn(this.query);

        final DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve(FAMILY_1)).thenReturn(this.documentReference1);

        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenReturn(this.xDocument1);

        when(this.xDocument1.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject1);

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_SIMPLE_JSON_DATA);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamiliesHaveNoXWikiDocument() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamiliesHaveNoPedigreeBaseObjects() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xDocument1.getXObject(any(EntityReference.class))).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));

        verifyNoMoreInteractions(this.xwiki, this.xDocument1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasNullData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasInvalidData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn("I'm not JSON");

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.logger, times(1)).error(eq(ERROR_UPDATING_DATA), eq(FAMILY_1), any());
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenPedigreeIsAlreadyConverted() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY))
            .thenReturn(PEDIGREE_NEW_FORMAT_DATA);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateBehavesAsExpectedForSimplePedigree() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));

        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), eq(PEDIGREE_MIGRATED_DATA),
            eq(this.context));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument1, this.context, false);
    }
}
