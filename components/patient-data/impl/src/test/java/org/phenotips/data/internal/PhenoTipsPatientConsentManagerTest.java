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

import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PhenoTipsPatientConsentManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<ConsentManager> mocker =
        new MockitoComponentMockingRule<ConsentManager>(PhenoTipsPatientConsentManager.class);

    /** Sets up initialization of the component with the given {@code baseObjects}. */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private DocumentModelBridge setUpInitialization(List<BaseObject> baseObjects) throws Exception
    {
        DocumentReferenceResolver resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentReference configDocRefMock = mock(DocumentReference.class);
        DocumentModelBridge configDoc = mock(XWikiDocument.class);

        doReturn(configDocRefMock).when(resolver).resolve(any(EntityReference.class));
        doReturn(configDoc).when(dab).getDocument(configDocRefMock);
        doReturn(baseObjects).when((XWikiDocument) configDoc).getXObjects(any(EntityReference.class));

        return configDoc;
    }

    private ConsentConfigurationMocks setUpInitializationWithConfigurationMocks() throws Exception
    {
        ConsentConfigurationMocks mocks = new ConsentConfigurationMocks();
        List<BaseObject> baseObjects = new LinkedList<>();
        DocumentModelBridge configDoc = this.setUpInitialization(baseObjects);

        baseObjects.add(mocks.consentConfig1);
        baseObjects.add(mocks.consentConfig2);
        baseObjects.add(mocks.consentConfig3);

        doReturn(ConsentConfigurationMocks.TEST_ID1).when(mocks.consentConfig1)
            .getStringValue(ConsentConfigurationMocks.ID_KEY);
        doReturn(ConsentConfigurationMocks.TEST_LABEL1).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.LABEL_KEY), anyString(),
            Matchers.eq(mocks.consentConfig1), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_DESCRIPTION1).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.DESCRIPTION_KEY), anyString(),
            Matchers.eq(mocks.consentConfig1), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_REQUIRED1).when(mocks.consentConfig1)
            .getIntValue(ConsentConfigurationMocks.REQUIRED_KEY);
        doReturn(ConsentConfigurationMocks.TEST_AFFECTS1).when(mocks.consentConfig1)
            .getIntValue(ConsentConfigurationMocks.AFFECTS_FIELDS_KEY);
        doReturn(mocks.formFields1).when(mocks.consentConfig1).getListValue(ConsentConfigurationMocks.FIELDS_KEY);

        doReturn(ConsentConfigurationMocks.TEST_ID2).when(mocks.consentConfig2)
            .getStringValue(ConsentConfigurationMocks.ID_KEY);
        doReturn(ConsentConfigurationMocks.TEST_LABEL2).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.LABEL_KEY), anyString(),
            Matchers.eq(mocks.consentConfig2), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_DESCRIPTION2).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.DESCRIPTION_KEY), anyString(),
            Matchers.eq(mocks.consentConfig2), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_REQUIRED2).when(mocks.consentConfig2)
            .getIntValue(ConsentConfigurationMocks.REQUIRED_KEY);
        doReturn(ConsentConfigurationMocks.TEST_AFFECTS2).when(mocks.consentConfig2)
            .getIntValue(ConsentConfigurationMocks.AFFECTS_FIELDS_KEY);
        doReturn(mocks.formFields2).when(mocks.consentConfig2).getListValue(ConsentConfigurationMocks.FIELDS_KEY);

        doReturn(ConsentConfigurationMocks.TEST_ID3).when(mocks.consentConfig3)
            .getStringValue(ConsentConfigurationMocks.ID_KEY);
        doReturn(ConsentConfigurationMocks.TEST_LABEL3).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.LABEL_KEY), anyString(),
            Matchers.eq(mocks.consentConfig3), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_DESCRIPTION3).when((XWikiDocument) configDoc).display(
            Matchers.eq(ConsentConfigurationMocks.DESCRIPTION_KEY), anyString(),
            Matchers.eq(mocks.consentConfig3), any(XWikiContext.class));
        doReturn(ConsentConfigurationMocks.TEST_REQUIRED3).when(mocks.consentConfig3)
            .getIntValue(ConsentConfigurationMocks.REQUIRED_KEY);
        doReturn(ConsentConfigurationMocks.TEST_AFFECTS3).when(mocks.consentConfig3)
            .getIntValue(ConsentConfigurationMocks.AFFECTS_FIELDS_KEY);
        doReturn(mocks.formFields3).when(mocks.consentConfig3).getListValue(ConsentConfigurationMocks.FIELDS_KEY);

        return mocks;
    }

    private class ConsentConfigurationMocks
    {
        private static final String ID_KEY = "id";

        private static final String LABEL_KEY = "label";

        private static final String DESCRIPTION_KEY = "description";

        private static final String REQUIRED_KEY = "required";

        private static final String FIELDS_KEY = "fields";

        private static final String AFFECTS_FIELDS_KEY = "affectsFields";

        private static final String TEST_ID1 = "id1";

        private static final String TEST_LABEL1 = "clean label";

        private static final String TEST_DESCRIPTION1 = "description";

        private static final int TEST_AFFECTS1 = 1;

        private static final int TEST_REQUIRED1 = 1;

        private static final boolean TEST_REQUIRED_BOOL = true;

        private static final String TEST_ID2 = "id2";

        private static final String TEST_LABEL2 = "non <div>clean</div> <p>label</p>";

        private static final String TEST_LABEL2_EXPECTED = "non clean label";

        private static final String TEST_DESCRIPTION2 = "";

        private static final int TEST_AFFECTS2 = 0;

        private static final int TEST_REQUIRED2 = 0;

        private static final boolean TEST_REQUIRED2_BOOL = false;

        private static final String TEST_ID3 = "id3";

        private static final String TEST_LABEL3 = "blah";

        private static final String TEST_DESCRIPTION3 = "Long description with a link [[link>>http://abc.com]]";

        private static final int TEST_AFFECTS3 = 1;

        private static final int TEST_REQUIRED3 = 1;

        private static final boolean TEST_REQUIRED3_BOOL = true;

        private static final int NUM_CONSENTS = 3;

        private final List<String> formFields1 = Arrays.asList("field1", "field2", "field3");

        private final List<String> formFields2 = null;

        private final List<String> formFields3 = new LinkedList<>();

        private BaseObject consentConfig1 = mock(BaseObject.class);

        private BaseObject consentConfig2 = mock(BaseObject.class);

        private BaseObject consentConfig3 = mock(BaseObject.class);
    }

    @Test
    public void testEmptyInitialization() throws Exception
    {
        this.setUpInitialization(new LinkedList<BaseObject>());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getSystemConsents().isEmpty());
    }

    @Test
    public void testNullConfigurationsInitialization() throws Exception
    {
        setUpInitialization(null);
        Assert.assertTrue(this.mocker.getComponentUnderTest().getSystemConsents().isEmpty());
    }

    /**
     * For testing normal initialization, when there are consents configured in the system. Also tests for label strings
     * being properly cleaned.
     */
    @Test
    public void testConfiguredInitialization() throws Exception
    {
        ConsentConfigurationMocks mocks = this.setUpInitializationWithConfigurationMocks();

        Assert.assertSame(this.mocker.getComponentUnderTest().getSystemConsents().size(),
            ConsentConfigurationMocks.NUM_CONSENTS);

        for (Consent consent : this.mocker.getComponentUnderTest().getSystemConsents()) {
            if (ConsentConfigurationMocks.TEST_ID1.equals(consent.getId())) {
                Assert.assertSame(consent.getLabel(), ConsentConfigurationMocks.TEST_LABEL1);
                Assert.assertSame(consent.getDescription(), ConsentConfigurationMocks.TEST_DESCRIPTION1);
                Assert.assertSame(consent.isRequired(), ConsentConfigurationMocks.TEST_REQUIRED_BOOL);
                Assert.assertSame(consent.getFields().size(), mocks.formFields1.size());
            } else if (ConsentConfigurationMocks.TEST_ID2.equals(consent.getId())) {
                Assert
                    .assertTrue(StringUtils.equals(consent.getLabel(), ConsentConfigurationMocks.TEST_LABEL2_EXPECTED));
                Assert.assertSame(consent.isRequired(), ConsentConfigurationMocks.TEST_REQUIRED2_BOOL);
                // expect to get null instead of empty descriptions
                Assert.assertSame(consent.getDescription(), null);
                Assert.assertSame(consent.getFields(), null);
            } else if (ConsentConfigurationMocks.TEST_ID3.equals(consent.getId())) {
                Assert.assertTrue(StringUtils.equals(consent.getLabel(), ConsentConfigurationMocks.TEST_LABEL3));
                Assert.assertSame(consent.isRequired(), ConsentConfigurationMocks.TEST_REQUIRED3_BOOL);
                Assert.assertSame(consent.getDescription(), ConsentConfigurationMocks.TEST_DESCRIPTION3);
                Assert.assertSame(consent.getFields().size(), 0);
            } else {
                Assert.fail("Found unexpected consent");
            }
        }
    }

    /**
     * @param consentIds the ids that are granted in the XWiki patient record
     */
    private void testPatientConsents(List<String> consentIds) throws Exception
    {
        String patientId = "pid";

        this.setUpInitializationWithConfigurationMocks();
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        PatientRepository repository = this.mocker.getInstance(PatientRepository.class);
        Patient patient = mock(Patient.class);
        DocumentReference patientRef = mock(DocumentReference.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);
        BaseObject idsHolder = mock(BaseObject.class);

        doReturn(patient).when(repository).get(patientId);
        doReturn(patientRef).when(patient).getDocumentReference();
        doReturn(patientDoc).when(dab).getDocument(patientRef);
        doReturn(idsHolder).when((XWikiDocument) patientDoc).getXObject(any(EntityReference.class));
        doReturn(consentIds).when(idsHolder).getListValue(anyString());

        Set<Consent> consents = this.mocker.getComponentUnderTest().getMissingConsentsForPatient(patientId);
        Assert.assertNotNull(consents);

        if (consentIds == null) {
            Assert.assertTrue(consents.size() == ConsentConfigurationMocks.NUM_CONSENTS);
        } else {
            // make sure the returned set of consents matches exactly the list of granted consents:
            // no granted are missing and no extra are present
            Assert.assertTrue(consents.size() == (ConsentConfigurationMocks.NUM_CONSENTS - consentIds.size()));
            for (Consent consent : consents) {
                Assert.assertFalse(consentIds.contains(consent.getId()));
                Assert.assertFalse(consent.isGranted());
            }
        }
    }

    @Test
    public void testLoadingConsentFromPatientWithNoConsents() throws Exception
    {
        this.testPatientConsents(new LinkedList<String>());
    }

    @Test
    public void testLoadingConsentsFromPatientWithSomeGranted() throws Exception
    {
        List<String> ids = new LinkedList<>();
        ids.add(ConsentConfigurationMocks.TEST_ID1);

        this.testPatientConsents(ids);
    }

    @Test
    public void testLoadingConsentsFromPatientWithNullList() throws Exception
    {
        this.testPatientConsents(null);
    }

    @Test
    public void testLoadingConsentsWithNullPatient() throws Exception
    {
        this.setUpInitializationWithConfigurationMocks();
        Patient patient = null;
        Assert.assertNull(this.mocker.getComponentUnderTest().getMissingConsentsForPatient(patient));
    }

    @Test
    public void testHasConsent() throws Exception
    {
        String patientId = "pid";

        List<String> consentIds = new LinkedList<>();
        consentIds.add(ConsentConfigurationMocks.TEST_ID1);

        this.setUpInitializationWithConfigurationMocks();
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        PatientRepository repository = this.mocker.getInstance(PatientRepository.class);
        Patient patient = mock(Patient.class);
        DocumentReference patientRef = mock(DocumentReference.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);
        BaseObject idsHolder = mock(BaseObject.class);

        doReturn(patient).when(repository).get(patientId);
        doReturn(patientRef).when(patient).getDocumentReference();
        doReturn(patientDoc).when(dab).getDocument(patientRef);
        doReturn(idsHolder).when((XWikiDocument) patientDoc).getXObject(any(EntityReference.class));
        doReturn(consentIds).when(idsHolder).getListValue(anyString());

        Assert.assertTrue(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.TEST_ID1));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.TEST_ID2));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.TEST_ID3));
    }

    private void setUpSettingConsents(BaseObject idsHolder, Patient patient, DocumentModelBridge patientDoc,
        XWikiContext context, XWiki wiki) throws Exception
    {
        this.setUpInitializationWithConfigurationMocks();

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(
            new DefaultParameterizedType((Type) null, Provider.class, new Type[] { XWikiContext.class }));
        DocumentReference patientRef = mock(DocumentReference.class);

        doReturn(patientRef).when(patient).getDocumentReference();
        doReturn(patientDoc).when(dab).getDocument(patientRef);
        doReturn(idsHolder).when((XWikiDocument) patientDoc).getXObject(any(EntityReference.class));
        doReturn(context).when(contextProvider).get();
        doReturn(wiki).when(context).getWiki();
    }

    @Test
    public void testSettingConsentsOnARecord() throws Exception
    {
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(idsHolder, patient, patientDoc, context, wiki);

        List<String> granted = new LinkedList<>();
        granted.add(ConsentConfigurationMocks.TEST_ID1);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, granted);

        verify(idsHolder, times(1)).set(eq("granted"), eq(granted), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }

    @Test
    public void testSettingConsentsOnARecordWithoutPriorXObject() throws Exception
    {
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(null, patient, patientDoc, context, wiki);

        doReturn(idsHolder).when((XWikiDocument) patientDoc)
            .newXObject(any(EntityReference.class), any(XWikiContext.class));

        List<String> granted = new LinkedList<>();
        granted.add(ConsentConfigurationMocks.TEST_ID1);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, granted);

        verify((XWikiDocument) patientDoc, times(1)).newXObject(any(EntityReference.class), any(XWikiContext.class));
        verify(idsHolder, times(1)).set(eq("granted"), eq(granted), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }

    @Test
    public void testSettingConsentsOnARecordWithNonExistentConsents() throws Exception
    {
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(idsHolder, patient, patientDoc, context, wiki);

        List<String> existingIds = new LinkedList<>();
        List<String> testIds = new LinkedList<>();
        existingIds.add(ConsentConfigurationMocks.TEST_ID1);
        existingIds.add(ConsentConfigurationMocks.TEST_ID2);
        testIds.add("id_nonexistent");
        testIds.addAll(existingIds);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, testIds);

        verify(idsHolder, times(1)).set(eq("granted"), eq(existingIds), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }
}
