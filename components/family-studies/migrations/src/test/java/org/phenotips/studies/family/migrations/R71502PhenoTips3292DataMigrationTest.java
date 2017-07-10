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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link R71502PhenoTips3292DataMigration}.
 */
public class R71502PhenoTips3292DataMigrationTest
{
    private static final String XWIKI_DOC_ERR_LOG = "Error obtaining or saving data to document {}: [{}]";

    private static final String DATA_MIGRATION_ERR_LOG = "Error when saving the XWiki document {}: [{}]";

    private static final String JSON_ERR_LOG = "Error updating JSON for document {} : [{}]";

    private static final String UNEXPECTED_ERR_LOG = "Unexpected migration error on document {}: [{}]";

    private static final String XWIKI_DOC_ERR_MSG = "Error number 0 in 0";

    private static final String DATA_MIGRATION_ERR_MSG = "Unable to reach the store for database wikiID";

    private static final String JSON_ERR_MSG = "A JSONObject text must begin with '{' at 1 [character 2 line 1]";

    private static final String SERIALIZED_ENTITY = "serialized_entity";

    private static final String WIKI_ID = "wikiID";

    private static final String PEDIGREECLASS_JSONDATA_KEY = "data";

    private static final String FAMILY_1 = "family1";

    private static final String FAMILY_2 = "family2";

    private static final String FAMILY_3 = "family3";

    private static final String PEDIGREE_1_DATA = "{\"GG\": [{\"prop\": {\"phenotipsId\": \"P0000001\"},\"id\": 0},"
        + "{\"prop\": {},\"id\": 1},{\"prop\": {\"phenotipsId\": \"P0000002\",\"cancers\": {\"Breast\": "
        + "{\"numericAgeAtDiagnosis\": 50,\"notes\": \"\",\"ageAtDiagnosis\": \"50\",\"affected\": true}}},\"id\": 2},"
        + "{\"prop\": {},\"id\": 3},{\"prop\": {\"phenotipsId\": \"P0000003\",\"cancers\": {\"Ovarian\": "
        + "{\"numericAgeAtDiagnosis\": 25,\"notes\": \"\",\"ageAtDiagnosis\": \"25\",\"affected\": true}}},\"id\": 4}],"
        + "\"settings\": {\"colors\": {\"cancers\": {\"Breast\": \"#e267a3\",\"Ovarian\": \"#9370DB\"}}}}";

    private static final String PEDIGREE_2_DATA = "{\"GG\": [{\"prop\": {\"phenotipsId\": \"P0000004\"},\"id\": 0},"
        + "{\"prop\": {},\"id\": 1},{\"prop\": {\"phenotipsId\": \"P0000005\",\"cancers\": {}},\"id\": 2},{\"prop\": "
        + "{},\"id\": 3},{\"prop\": {\"phenotipsId\": \"P0000006\",\"cancers\": {}},\"id\": 4}],\"settings\": "
        + "{\"colors\": {\"cancers\": {}}}}";

    private static final String PEDIGREE_3_DATA = "{\"GG\": [{\"prop\": {\"phenotipsId\": \"P0000007\",\"cancers\": "
        + "{\"Kidney\": {\"numericAgeAtDiagnosis\": 38,\"notes\": \"\",\"ageAtDiagnosis\": \"38\",\"affected\": "
        + "false}}},\"id\": 0}],\"settings\": {\"colors\": {\"cancers\": {\"Kidney\": \"#e267a3\"}}}}";

    private static final String PEDIGREE_1_MIGRATED_DATA = "{\"GG\": [{\"prop\": {\"phenotipsId\": \"P0000001\"},"
        + "\"id\": 0},{\"prop\": {},\"id\": 1},{\"prop\": {\"phenotipsId\": \"P0000002\",\"cancers\": "
        + "[{\"id\":\"HP:0100013\",\"label\":\"Breast\",\"affected\":true,\"qualifiers\":[{\"ageAtDiagnosis\":"
        + "\"50\",\"numericAgeAtDiagnosis\":50,\"primary\":true,\"notes\":\"\"}]}]},\"id\": 2},{\"prop\": {},\"id\": 3}"
        + ",{\"prop\": {\"phenotipsId\": \"P0000003\",\"cancers\": [{\"id\":\"HP:0100615\",\"label\":\"Ovarian\","
        + "\"affected\":true,\"qualifiers\":[{\"ageAtDiagnosis\":\"25\",\"numericAgeAtDiagnosis\":25,\"primary\":true,"
        + "\"notes\":\"\"}]}]},\"id\": 4}],\"settings\": {\"colors\": {\"cancers\": {\"HP:0100013\": \"#e267a3\","
        + "\"HP:0100615\": \"#9370DB\"}}}}";

    private static final String PEDIGREE_2_MIGRATED_DATA = "{\"GG\": [{\"prop\": {\"phenotipsId\": \"P0000004\"},"
        + "\"id\": 0},{\"prop\": {},\"id\": 1},{\"prop\": {\"phenotipsId\": \"P0000005\",\"cancers\": []},\"id\": 2},"
        + "{\"prop\": {},\"id\": 3},{\"prop\": {\"phenotipsId\": \"P0000006\",\"cancers\": []},\"id\": 4}]"
        + ",\"settings\": {\"colors\": {\"cancers\": {}}}}";

    private static final String PEDIGREE_3_MIGRATED_DATA =
        "{\"GG\":[{\"prop\":{\"phenotipsId\":\"P0000007\",\"cancers\":[{\"id\":\"HP:0009726\",\"label\":\"Kidney\","
            + "\"affected\":false,\"qualifiers\":[{\"ageAtDiagnosis\":\"38\",\"numericAgeAtDiagnosis\":38,"
            + "\"primary\":true,\"notes\":\"\"}]}]},\"id\": 0}],\"settings\":{\"colors\":{\"cancers\":"
            + "{\"HP:0009726\":\"#e267a3\"}}}}";

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
    private DocumentReference documentReference2;

    @Mock
    private DocumentReference documentReference3;

    @Mock
    private BaseObject pedigreeBaseObject1;

    @Mock
    private BaseObject pedigreeBaseObject2;

    @Mock
    private BaseObject pedigreeBaseObject3;

    @Mock
    private XWikiDocument xDocument1;

    @Mock
    private XWikiDocument xDocument2;

    @Mock
    private XWikiDocument xDocument3;

    @Mock
    private XWikiHibernateStore store;

    private R71502PhenoTips3292DataMigration component;

    private Logger logger;

    private ComponentManager componentManager;

    @Rule
    public MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<>(R71502PhenoTips3292DataMigration.class, HibernateDataMigration.class,
            "R71502-PT-3292");

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.mocker.registerMockComponent(ComponentManager.class);
        this.componentManager = this.mocker.getInstance(ComponentManager.class);
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(this.store);

        this.component = (R71502PhenoTips3292DataMigration) this.mocker.getComponentUnderTest();
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
        when(this.query.list()).thenReturn(Arrays.asList(FAMILY_1, FAMILY_2, FAMILY_3));

        final DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolver.resolve(FAMILY_1)).thenReturn(this.documentReference1);
        when(resolver.resolve(FAMILY_2)).thenReturn(this.documentReference2);
        when(resolver.resolve(FAMILY_3)).thenReturn(this.documentReference3);

        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenReturn(this.xDocument1);
        when(this.xwiki.getDocument(this.documentReference2, this.context)).thenReturn(this.xDocument2);
        when(this.xwiki.getDocument(this.documentReference3, this.context)).thenReturn(this.xDocument3);

        when(this.xDocument1.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject1);
        when(this.xDocument2.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject2);
        when(this.xDocument3.getXObject(any(EntityReference.class))).thenReturn(this.pedigreeBaseObject3);

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_1_DATA);
        when(this.pedigreeBaseObject2.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_2_DATA);
        when(this.pedigreeBaseObject3.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(PEDIGREE_3_DATA);
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
    public void doInHibernateDoesNothingWhenXWikiExceptionIsThrownForEachFamily() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.documentReference1, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.logger, times(1)).error(XWIKI_DOC_ERR_LOG, FAMILY_1, XWIKI_DOC_ERR_MSG);
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
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasEmptyData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(StringUtils.EMPTY);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateDoesNothingWhenFamilyPedigreeHasBlankData() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        when(this.pedigreeBaseObject1.getStringValue(PEDIGREECLASS_JSONDATA_KEY)).thenReturn(StringUtils.SPACE);

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

        verify(this.logger, times(1)).error(JSON_ERR_LOG, FAMILY_1, JSON_ERR_MSG);
        verifyNoMoreInteractions(this.xwiki, this.xDocument1, this.pedigreeBaseObject1);
    }

    @Test
    public void doInHibernateReplacesObjectByArrayWhenCancersObjectIsPresentButEmpty() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_2));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument2, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject2, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject2, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor.capture(),
            eq(this.context));
        final String captured = captor.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_2_MIGRATED_DATA).similar(new JSONObject(captured)));
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument2, context, false);
    }

    @Test
    public void doInHibernateBehavesAsExpectedForPedigreeWithOnePatientWithOneCancer() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_3));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument3, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject3, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject3, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor.capture(),
            eq(this.context));
        final String captured = captor.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_3_MIGRATED_DATA).similar(new JSONObject(captured)));

        verify(this.xDocument3, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument3, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument3, context, false);
    }

    @Test
    public void doInHibernateBehavesAsExpectedForPedigreeWithSeveralPatientsWithWithCancers() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor.capture(),
            eq(this.context));
        final String captured = captor.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_1_MIGRATED_DATA).similar(new JSONObject(captured)));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument1, context, false);
    }

    @Test
    public void doInHibernateDoesNothingAndContinuesMigratingOnXWikiException()
        throws XWikiException
    {
        when(this.xwiki.getDocument(this.documentReference2, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(3)).getDocument(any(DocumentReference.class), eq(this.context));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.xDocument2, never()).getXObject(any(EntityReference.class));
        verify(this.xDocument3, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verify(this.pedigreeBaseObject3, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor1.capture(),
            eq(this.context));
        final String captured1 = captor1.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_1_MIGRATED_DATA).similar(new JSONObject(captured1)));

        final ArgumentCaptor<String> captor3 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject3, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor3.capture(),
            eq(this.context));
        final String captured3 = captor3.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_3_MIGRATED_DATA).similar(new JSONObject(captured3)));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.logger, times(1)).error(XWIKI_DOC_ERR_LOG, FAMILY_2, XWIKI_DOC_ERR_MSG);

        verify(this.xDocument3, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument3, times(1)).setMinorEdit(true);

        verify(this.store, times(1)).saveXWikiDoc(this.xDocument1, context, false);
        verify(this.store, never()).saveXWikiDoc(this.xDocument2, context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument3, context, false);
    }

    @Test
    public void doInHibernateDoesNothingAndContinuesMigratingWhenOnDataMigrationException()
        throws ComponentLookupException, XWikiException
    {
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate"))
            .thenThrow(new ComponentLookupException(":("))
            .thenReturn(this.store);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(3)).getDocument(any(DocumentReference.class), eq(this.context));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.xDocument2, times(1)).getXObject(any(EntityReference.class));
        verify(this.xDocument3, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verify(this.pedigreeBaseObject2, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verify(this.pedigreeBaseObject3, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor1.capture(),
            eq(this.context));
        final String captured1 = captor1.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_1_MIGRATED_DATA).similar(new JSONObject(captured1)));

        final ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject2, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor2.capture(),
            eq(this.context));
        final String captured2 = captor2.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_2_MIGRATED_DATA).similar(new JSONObject(captured2)));

        final ArgumentCaptor<String> captor3 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject3, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor3.capture(),
            eq(this.context));
        final String captured3 = captor3.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_3_MIGRATED_DATA).similar(new JSONObject(captured3)));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.logger, times(1)).error(DATA_MIGRATION_ERR_LOG, FAMILY_1, DATA_MIGRATION_ERR_MSG);

        verify(this.xDocument3, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument3, times(1)).setMinorEdit(true);

        verify(this.store, never()).saveXWikiDoc(this.xDocument1, context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument2, context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument3, context, false);
    }

    @Test
    public void doInHibernateDoesNothingAndContinuesMigratingWhenUnexpectedExceptionOccurs()
        throws ComponentLookupException, XWikiException
    {
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate"))
            .thenThrow(new RuntimeException())
            .thenReturn(this.store);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(3)).getDocument(any(DocumentReference.class), eq(this.context));
        verify(this.xDocument1, times(1)).getXObject(any(EntityReference.class));
        verify(this.xDocument2, times(1)).getXObject(any(EntityReference.class));
        verify(this.xDocument3, times(1)).getXObject(any(EntityReference.class));
        verify(this.pedigreeBaseObject1, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verify(this.pedigreeBaseObject2, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);
        verify(this.pedigreeBaseObject3, times(1)).getStringValue(PEDIGREECLASS_JSONDATA_KEY);

        final ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject1, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor1.capture(),
            eq(this.context));
        final String captured1 = captor1.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_1_MIGRATED_DATA).similar(new JSONObject(captured1)));

        final ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject2, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor2.capture(),
            eq(this.context));
        final String captured2 = captor2.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_2_MIGRATED_DATA).similar(new JSONObject(captured2)));

        final ArgumentCaptor<String> captor3 = ArgumentCaptor.forClass(String.class);
        verify(this.pedigreeBaseObject3, times(1)).set(eq(PEDIGREECLASS_JSONDATA_KEY), captor3.capture(),
            eq(this.context));
        final String captured3 = captor3.getValue();
        Assert.assertTrue(new JSONObject(PEDIGREE_3_MIGRATED_DATA).similar(new JSONObject(captured3)));

        verify(this.xDocument1, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument1, times(1)).setMinorEdit(true);

        verify(this.logger, times(1)).error(UNEXPECTED_ERR_LOG, FAMILY_1, null);

        verify(this.xDocument3, times(1)).setComment(this.component.getDescription());
        verify(this.xDocument3, times(1)).setMinorEdit(true);

        verify(this.store, never()).saveXWikiDoc(this.xDocument1, context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument2, context, false);
        verify(this.store, times(1)).saveXWikiDoc(this.xDocument3, context, false);
    }

    @Test
    public void getDescriptionBehavesAsExpected()
    {
        Assert.assertEquals("Migrate pedigree data (link cancers to their HPO identifiers)",
            this.component.getDescription());
    }

    @Test
    public void getVersion()
    {
        Assert.assertEquals(0, new XWikiDBVersion(71502).compareTo(this.component.getVersion()));
    }

    /** Non empty name. */
    @Test
    public void getName() throws Exception
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.component.getName()));
    }

    /**
     * Always executes. When the initial database version is greater than this migrator's, the manager decides not to
     * run.
     */
    @Test
    public void shouldExecute() throws Exception
    {
        Assert.assertTrue(this.component.shouldExecute(new XWikiDBVersion(54589)));
        Assert.assertTrue(this.component.shouldExecute(new XWikiDBVersion(54591)));
    }
}
