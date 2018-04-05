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
package org.phenotips.data.internal;

import org.phenotips.data.Cancer;
import org.phenotips.data.CancerQualifier;

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
import org.junit.Assert;
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
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link R71510PhenoTips3509DataMigration}.
 */
public class R71510PhenoTips3509DataMigrationTest
{
    private static final int VERSION = 71510;

    private static final String SERIALIZED_ENTITY = "serialized_entity";

    private static final String WIKI_ID = "wikiID";

    private static final String FAMILY_1 = "family1";

    private static final String FAMILY_2 = "family2";

    private static final String FAMILY_3 = "family3";

    private static final String PATIENT_1 = "P0000001";

    private static final String PATIENT_2 = "P0000002";

    private static final String CANCER_LABEL = "cancer";

    private static final String AFFECTED_LABEL = "affected";

    private static final String AGE_AT_DIAGNOSIS_LABEL = "ageAtDiagnosis";

    private static final String NUM_AGE_AT_DIAGNOSIS_LABEL = "numericAgeAtDiagnosis";

    private static final String PRIMARY_LABEL = "primary";

    private static final String LATERALITY_LABEL = "laterality";

    private static final String NOTES_LABEL = "notes";

    private static final String CANCER_1 = "HP:01";

    private static final String CANCER_2 = "HP:02";

    private static final String CANCER_3 = "Non standard cancer";

    private static final String ERROR_OBTAINING_DATA_FAMILY = "Error obtaining family document {}: [{}]";

    private static final String ERROR_OBTAINING_DATA_MSG = "Error number 0 in 0";

    private static final String ERROR_OBTAINING_DATA_PATIENT = "Error obtaining patient document {}: [{}]";

    private static final String ERROR_SAVING_DATA_PATIENT = "Could not save cancer data for patient with ID {}: [{}]";

    private static final String ERROR_SAVING_DATA_PATIENT_MSG = "Unable to reach the store for database wikiID";

    private static final String ERROR_UNEXPECTED_FAMILY = "An unexpected exception occurred when migrating family {}";

    private static final String ERROR_UNEXPECTED_PATIENT = "An unexpected exception occurred when migrating patient {}";

    private static final String PEDIGREE_1 = "{\"members\":[{\"properties\":{\"id\":\"P0000001\","
        + "\"cancers\":[{\"qualifiers\":[{\"numericAgeAtDiagnosis\":1,\"notes\":\"asdfg\",\"ageAtDiagnosis\":\"1\","
        + "\"cancer\":\"HP:01\",\"laterality\":\"l\",\"primary\":false},{\"numericAgeAtDiagnosis\":0,\"notes\":"
        + "\"asdf\",\"ageAtDiagnosis\":\"before_1\",\"cancer\":\"HP:01\",\"laterality\":\"r\",\"primary\":true}],"
        + "\"id\":\"HP:01\",\"label\":\"Cancer one\",\"affected\":true},{\"qualifiers\":[{"
        + "\"numericAgeAtDiagnosis\":2,\"ageAtDiagnosis\":\"2\",\"cancer\":\"HP:02\",\"laterality\":\"bi\","
        + "\"primary\":false}],\"id\":\"HP:02\",\"label\":\"Cancer two\",\"affected\":true},"
        + "{\"qualifiers\":[{\"numericAgeAtDiagnosis\":3,\"ageAtDiagnosis\":\"3\",\"cancer\":\"Non standard cancer\","
        + "\"laterality\":\"u\",\"primary\":true}],\"id\":\"Non standard cancer\",\"label\":\"Non standard cancer\","
        + "\"affected\":true}]}}]}";

    private static final String PEDIGREE_2 = "{\"members\":[{\"properties\":{\"cancers\":[]}}]}";

    private static final String PEDIGREE_3 = "{\"members\":[{\"properties\":{\"id\":\"P0000002\",\"cancers\":[]}}]}";

    private static final String PEDIGREE_CLASS_DATA_KEY = "data";

    @Rule
    public MockitoComponentMockingRule<HibernateDataMigration> mocker =
        new MockitoComponentMockingRule<>(R71510PhenoTips3509DataMigration.class, HibernateDataMigration.class,
            "R71510-PT-3509");

    @Mock
    private XWikiHibernateStore store;

    @Mock
    private Session session;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @Mock
    private Query query;

    @Mock
    private DocumentReference familyReference1;

    @Mock
    private DocumentReference familyReference2;

    @Mock
    private DocumentReference familyReference3;

    @Mock
    private DocumentReference patientReference1;

    @Mock
    private DocumentReference patientReference2;

    @Mock
    private XWikiDocument familyDoc1;

    @Mock
    private XWikiDocument familyDoc2;

    @Mock
    private XWikiDocument familyDoc3;

    @Mock
    private XWikiDocument patientDoc1;

    @Mock
    private XWikiDocument patientDoc2;

    @Mock
    private BaseObject famBaseObj1;

    @Mock
    private BaseObject famBaseObj2;

    @Mock
    private BaseObject famBaseObj3;

    @Mock
    private BaseObject cancerObj1;

    @Mock
    private BaseObject cancerObj2;

    @Mock
    private BaseObject cancerObj3;

    @Mock
    private BaseObject qualifierObj;


    private R71510PhenoTips3509DataMigration component;

    private Logger logger;

    private ComponentManager componentManager;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.mocker.registerMockComponent(ComponentManager.class);
        this.componentManager = this.mocker.getInstance(ComponentManager.class);
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate")).thenReturn(this.store);

        this.component = (R71510PhenoTips3509DataMigration) this.mocker.getComponentUnderTest();
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
        when(resolver.resolve(FAMILY_1)).thenReturn(this.familyReference1);
        when(resolver.resolve(FAMILY_2)).thenReturn(this.familyReference2);
        when(resolver.resolve(FAMILY_3)).thenReturn(this.familyReference3);

        when(this.xwiki.getDocument(this.familyReference1, this.context)).thenReturn(this.familyDoc1);
        when(this.xwiki.getDocument(this.familyReference2, this.context)).thenReturn(this.familyDoc2);
        when(this.xwiki.getDocument(this.familyReference3, this.context)).thenReturn(this.familyDoc3);

        when(this.familyDoc1.getXObject(any(EntityReference.class))).thenReturn(this.famBaseObj1);
        when(this.familyDoc2.getXObject(any(EntityReference.class))).thenReturn(this.famBaseObj2);
        when(this.familyDoc3.getXObject(any(EntityReference.class))).thenReturn(this.famBaseObj3);

        when(this.famBaseObj1.getStringValue(PEDIGREE_CLASS_DATA_KEY)).thenReturn(PEDIGREE_1);
        when(this.famBaseObj2.getStringValue(PEDIGREE_CLASS_DATA_KEY)).thenReturn(PEDIGREE_2);
        when(this.famBaseObj3.getStringValue(PEDIGREE_CLASS_DATA_KEY)).thenReturn(PEDIGREE_3);

        when(resolver.resolve(PATIENT_1)).thenReturn(this.patientReference1);
        when(resolver.resolve(PATIENT_2)).thenReturn(this.patientReference2);

        when(this.xwiki.getDocument(this.patientReference1, this.context)).thenReturn(this.patientDoc1);
        when(this.xwiki.getDocument(this.patientReference2, this.context)).thenReturn(this.patientDoc2);

        when(this.patientDoc1.newXObject(Cancer.CLASS_REFERENCE, this.context))
            .thenReturn(this.cancerObj1, this.cancerObj2, this.cancerObj3);

        // Return one qualifier BaseObject for all of them, since the qualifer data can potentially come in in any
        // order.
        when(this.patientDoc1.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenReturn(this.qualifierObj);
    }

    @Test
    public void doInHibernateFamiliesHaveNoXWikiDocument() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.familyReference1, this.context)).thenReturn(null);

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateXWikiExceptionForFamily() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.familyReference1, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.logger, times(1)).error(ERROR_OBTAINING_DATA_FAMILY, FAMILY_1, ERROR_OBTAINING_DATA_MSG);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateUnexpectedExceptionForFamily() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.xwiki.getDocument(this.familyReference1, this.context))
            .thenThrow(new RuntimeException(ERROR_OBTAINING_DATA_MSG));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(any(DocumentReference.class), any(XWikiContext.class));
        verify(this.logger, times(1)).error(ERROR_UNEXPECTED_FAMILY, FAMILY_1, ERROR_OBTAINING_DATA_MSG);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateXWikiExceptionForPatient() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_3));
        when(this.xwiki.getDocument(this.patientReference2, this.context)).thenThrow(new XWikiException());

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference3, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference2, this.context);
        verify(this.logger, times(1)).error(ERROR_OBTAINING_DATA_PATIENT, PATIENT_2, ERROR_OBTAINING_DATA_MSG);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateUnexpectedExceptionForPatient() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_3));
        when(this.xwiki.getDocument(this.patientReference2, this.context))
            .thenThrow(new RuntimeException(ERROR_OBTAINING_DATA_MSG));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference3, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference2, this.context);
        verify(this.logger, times(1)).error(ERROR_UNEXPECTED_PATIENT, PATIENT_2, ERROR_OBTAINING_DATA_MSG);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateDataMigrationExceptionForPatient() throws XWikiException, ComponentLookupException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.componentManager.getInstance(XWikiStoreInterface.class, "hibernate"))
            .thenThrow(new ComponentLookupException(ERROR_OBTAINING_DATA_MSG));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference1, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference1, this.context);
        verify(this.logger, times(1)).error(ERROR_SAVING_DATA_PATIENT, PATIENT_1, ERROR_SAVING_DATA_PATIENT_MSG);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernateFamilyHasNoMembers() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_1));
        when(this.famBaseObj1.getStringValue(PEDIGREE_CLASS_DATA_KEY)).thenReturn("{\"members\":[]}");

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference1, this.context);
        verify(this.logger, times(1)).debug("Family {} has no members", FAMILY_1);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void doInHibernatePatientHasNoCancers() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_3));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference3, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference2, this.context);
        verify(this.logger, times(1)).debug("Patient {} has no associated cancer data", PATIENT_2);
        verifyZeroInteractions(this.patientDoc2);
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void hibernateMigrateNoPatientForNode() throws XWikiException
    {
        when(this.query.list()).thenReturn(Collections.singletonList(FAMILY_2));

        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference2, this.context);
        verify(this.logger, times(1)).debug("Data is not associated with any patient");
        verifyNoMoreInteractions(this.xwiki);
    }

    @Test
    public void hibernateMigrateSuccessful() throws XWikiException
    {
        this.component.doInHibernate(this.session);

        verify(this.xwiki, times(1)).getDocument(this.familyReference1, this.context);
        verify(this.xwiki, times(1)).getDocument(this.familyReference2, this.context);
        verify(this.xwiki, times(1)).getDocument(this.familyReference3, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference1, this.context);
        verify(this.xwiki, times(1)).getDocument(this.patientReference2, this.context);
        verify(this.logger, times(1)).debug("Data is not associated with any patient");

        verify(this.cancerObj1, times(1)).set(CANCER_LABEL, CANCER_1, this.context);
        verify(this.cancerObj1, times(1)).set(AFFECTED_LABEL, 1, this.context);

        verify(this.cancerObj2, times(1)).set(CANCER_LABEL, CANCER_2, this.context);
        verify(this.cancerObj2, times(1)).set(AFFECTED_LABEL, 1, this.context);

        verify(this.cancerObj3, times(1)).set(CANCER_LABEL, CANCER_3, this.context);
        verify(this.cancerObj3, times(1)).set(AFFECTED_LABEL, 1, this.context);

        verify(this.qualifierObj, times(2)).set(CANCER_LABEL, CANCER_1, this.context);
        verify(this.qualifierObj, times(1)).set(CANCER_LABEL, CANCER_2, this.context);
        verify(this.qualifierObj, times(1)).set(CANCER_LABEL, CANCER_3, this.context);

        verify(this.qualifierObj, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, "before_1", this.context);
        verify(this.qualifierObj, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, "1", this.context);
        verify(this.qualifierObj, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, "2", this.context);
        verify(this.qualifierObj, times(1)).set(AGE_AT_DIAGNOSIS_LABEL, "3", this.context);

        verify(this.qualifierObj, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, 0, this.context);
        verify(this.qualifierObj, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, 1, this.context);
        verify(this.qualifierObj, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, 2, this.context);
        verify(this.qualifierObj, times(1)).set(NUM_AGE_AT_DIAGNOSIS_LABEL, 3, this.context);

        verify(this.qualifierObj, times(2)).set(PRIMARY_LABEL, 1, this.context);
        verify(this.qualifierObj, times(2)).set(PRIMARY_LABEL, 0, this.context);

        verify(this.qualifierObj, times(1)).set(LATERALITY_LABEL, "l", this.context);
        verify(this.qualifierObj, times(1)).set(LATERALITY_LABEL, "r", this.context);
        verify(this.qualifierObj, times(1)).set(LATERALITY_LABEL, "bi", this.context);
        verify(this.qualifierObj, times(1)).set(LATERALITY_LABEL, "u", this.context);

        verify(this.qualifierObj, times(1)).set(NOTES_LABEL, "asdf", this.context);
        verify(this.qualifierObj, times(1)).set(NOTES_LABEL, "asdfg", this.context);

        verifyNoMoreInteractions(this.xwiki, this.cancerObj1, this.cancerObj2, this.cancerObj3, this.qualifierObj);
    }

    @Test
    public void getDescription()
    {
        Assert.assertEquals("Copy pedigree cancer data to patient sheet", this.component.getDescription());
    }

    @Test
    public void getVersion()
    {
        Assert.assertEquals(0, new XWikiDBVersion(VERSION).compareTo(this.component.getVersion()));
    }

    /** Non empty name. */
    @Test
    public void getName()
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.component.getName()));
    }
}
