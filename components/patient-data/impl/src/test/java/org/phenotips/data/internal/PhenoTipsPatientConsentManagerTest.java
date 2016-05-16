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

import org.apache.commons.codec.binary.StringUtils;
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

    /** Sets up initialization of the component with the given `baseObjects` */
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

    @SuppressWarnings("static-access")
    private ConsentConfigurationMocks setUpInitializationWithConfigurationMocks() throws Exception
    {
        ConsentConfigurationMocks mocks = new ConsentConfigurationMocks();
        List<BaseObject> baseObjects = new LinkedList<>();
        DocumentModelBridge configDoc = this.setUpInitialization(baseObjects);

        baseObjects.add(mocks.consentConfig1);
        baseObjects.add(mocks.consentConfig2);
        baseObjects.add(mocks.consentConfig3);

        doReturn(mocks.id1).when(mocks.consentConfig1).getStringValue(mocks.idKey);
        doReturn(mocks.label1).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.labelKey), anyString(),
            Matchers.eq(mocks.consentConfig1), any(XWikiContext.class));
        doReturn(mocks.description1).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.descriptionKey), anyString(),
                Matchers.eq(mocks.consentConfig1), any(XWikiContext.class));
        doReturn(mocks.req1).when(mocks.consentConfig1).getIntValue(mocks.requiredKey);
        doReturn(mocks.affects1).when(mocks.consentConfig1).getIntValue(mocks.affectsFieldsKey);
        doReturn(mocks.formFields1).when(mocks.consentConfig1).getListValue(mocks.fieldsKey);

        doReturn(mocks.id2).when(mocks.consentConfig2).getStringValue(mocks.idKey);
        doReturn(mocks.label2).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.labelKey), anyString(),
            Matchers.eq(mocks.consentConfig2), any(XWikiContext.class));
        doReturn(mocks.description2).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.descriptionKey), anyString(),
                Matchers.eq(mocks.consentConfig2), any(XWikiContext.class));
        doReturn(mocks.req2).when(mocks.consentConfig2).getIntValue(mocks.requiredKey);
        doReturn(mocks.affects2).when(mocks.consentConfig2).getIntValue(mocks.affectsFieldsKey);
        doReturn(mocks.formFields2).when(mocks.consentConfig2).getListValue(mocks.fieldsKey);

        doReturn(mocks.id3).when(mocks.consentConfig3).getStringValue(mocks.idKey);
        doReturn(mocks.label3).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.labelKey), anyString(),
            Matchers.eq(mocks.consentConfig3), any(XWikiContext.class));
        doReturn(mocks.description3).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.descriptionKey), anyString(),
                Matchers.eq(mocks.consentConfig3), any(XWikiContext.class));
        doReturn(mocks.req3).when(mocks.consentConfig3).getIntValue(mocks.requiredKey);
        doReturn(mocks.affects3).when(mocks.consentConfig3).getIntValue(mocks.affectsFieldsKey);
        doReturn(mocks.formFields3).when(mocks.consentConfig3).getListValue(mocks.fieldsKey);

        return mocks;
    }

    private class ConsentConfigurationMocks
    {
        static final String idKey = "id";
        static final String labelKey = "label";
        static final String descriptionKey = "description";
        static final String requiredKey = "required";
        static final String fieldsKey = "fields";
        static final String affectsFieldsKey = "affectsFields";

        static final String id1 = "id1";
        static final String label1 = "clean label";
        static final String description1 = "description";
        final List<String> formFields1 = Arrays.asList("field1", "field2", "field3");
        Integer affects1 = 1;
        Integer req1 = 1;
        Boolean req1B = true;

        static final String id2 = "id2";
        static final String label2 = "non <div>clean</div> <p>label</p>";
        static final String label2expected = "non clean label";
        static final String description2 = "";
        final List<String> formFields2 = null;
        Integer affects2 = 0;
        Integer req2 = 0;
        Boolean req2B = false;

        static final String id3 = "id3";
        static final String label3 = "blah";
        static final String description3 = "Long description with a link [[link>>http://abc.com]]";
        final List<String> formFields3 = new LinkedList<String>();
        Integer affects3 = 1;
        Integer req3 = 1;
        Boolean req3B = true;

        BaseObject consentConfig1 = mock(BaseObject.class);
        BaseObject consentConfig2 = mock(BaseObject.class);
        BaseObject consentConfig3 = mock(BaseObject.class);

        public static final int NUM_CONSENTS = 3;
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
     * For testing normal initialization, when there are consents configured in the system. Also tests for
     * label strings being properly cleaned.
     */
    @SuppressWarnings("static-access")
    @Test
    public void testConfiguredInitialization() throws Exception
    {
        ConsentConfigurationMocks mocks = this.setUpInitializationWithConfigurationMocks();

        Assert.assertSame(this.mocker.getComponentUnderTest().getSystemConsents().size(), mocks.NUM_CONSENTS);

        for (Consent consent : this.mocker.getComponentUnderTest().getSystemConsents()) {
            if (mocks.id1.equals(consent.getId())) {
                Assert.assertSame(consent.getLabel(), mocks.label1);
                Assert.assertSame(consent.getDescription(), mocks.description1);
                Assert.assertSame(consent.isRequired(), mocks.req1B);
                Assert.assertSame(consent.getFields().size(), mocks.formFields1.size());
            } else if (mocks.id2.equals(consent.getId())) {
                Assert.assertTrue(StringUtils.equals(consent.getLabel(), mocks.label2expected));
                Assert.assertSame(consent.isRequired(), mocks.req2B);
                Assert.assertSame(consent.getDescription(), null);  // expect to get null instead of empty descriptions
                Assert.assertSame(consent.getFields(), null);
            } else if (mocks.id3.equals(consent.getId())) {
                Assert.assertTrue(StringUtils.equals(consent.getLabel(), mocks.label3));
                Assert.assertSame(consent.isRequired(), mocks.req3B);
                Assert.assertSame(consent.getDescription(), mocks.description3);
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

        doReturn(patient).when(repository).getPatientById(patientId);
        doReturn(patientRef).when(patient).getDocument();
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
            for (Consent consent : consents)
            {
                Assert.assertFalse(consentIds.contains(consent.getId()));
                Assert.assertFalse(consent.isGranted());
            }
        }
    }

    @Test
    public void testLoadingConsentFromPatient_hasNone() throws Exception
    {
        this.testPatientConsents(new LinkedList<String>());
    }

    @Test
    public void testLoadingConsentsFromPatient_someGranted() throws Exception
    {
        List<String> ids = new LinkedList<>();
        ids.add(ConsentConfigurationMocks.id1);

        this.testPatientConsents(ids);
    }

    @Test
    public void testLoadingConsentsFromPatient_nullList() throws Exception
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
        consentIds.add(ConsentConfigurationMocks.id1);

        this.setUpInitializationWithConfigurationMocks();
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        PatientRepository repository = this.mocker.getInstance(PatientRepository.class);
        Patient patient = mock(Patient.class);
        DocumentReference patientRef = mock(DocumentReference.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);
        BaseObject idsHolder = mock(BaseObject.class);

        doReturn(patient).when(repository).getPatientById(patientId);
        doReturn(patientRef).when(patient).getDocument();
        doReturn(patientDoc).when(dab).getDocument(patientRef);
        doReturn(idsHolder).when((XWikiDocument) patientDoc).getXObject(any(EntityReference.class));
        doReturn(consentIds).when(idsHolder).getListValue(anyString());

        Assert.assertTrue(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.id1));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.id2));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent(patient, ConsentConfigurationMocks.id3));
    }

    private void setUpSettingConsents(BaseObject idsHolder, Patient patient, DocumentModelBridge patientDoc,
        XWikiContext context, XWiki wiki) throws Exception
    {
        this.setUpInitializationWithConfigurationMocks();

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(
            new DefaultParameterizedType((Type) null, Provider.class, new Type[] { XWikiContext.class }));
        DocumentReference patientRef = mock(DocumentReference.class);

        doReturn(patientRef).when(patient).getDocument();
        doReturn(patientDoc).when(dab).getDocument(patientRef);
        doReturn(idsHolder).when((XWikiDocument) patientDoc).getXObject(any(EntityReference.class));
        doReturn(context).when(contextProvider).get();
        doReturn(wiki).when(context).getWiki();
    }

    @SuppressWarnings("static-access")
    @Test
    public void testSettingConsentsOnARecord_normal() throws Exception
    {
        ConsentConfigurationMocks consentMocks = new ConsentConfigurationMocks();
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(idsHolder, patient, patientDoc, context, wiki);

        List<String> granted = new LinkedList<>();
        granted.add(consentMocks.id1);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, granted);

        verify(idsHolder, times(1)).set(eq("granted"), eq(granted), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }

    @SuppressWarnings("static-access")
    @Test
    public void testSettingConsentsOnARecord_xwikiObjectDoesNotExist() throws Exception
    {
        ConsentConfigurationMocks consentMocks = new ConsentConfigurationMocks();
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(null, patient, patientDoc, context, wiki);

        doReturn(idsHolder).when((XWikiDocument) patientDoc)
            .newXObject(any(EntityReference.class), any(XWikiContext.class));

        List<String> granted = new LinkedList<>();
        granted.add(consentMocks.id1);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, granted);

        verify((XWikiDocument) patientDoc, times(1)).newXObject(any(EntityReference.class), any(XWikiContext.class));
        verify(idsHolder, times(1)).set(eq("granted"), eq(granted), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }

    @SuppressWarnings("static-access")
    @Test
    public void testSettingConsentsOnARecord_nonExistentConsents() throws Exception
    {
        ConsentConfigurationMocks consentMocks = new ConsentConfigurationMocks();
        BaseObject idsHolder = mock(BaseObject.class);
        Patient patient = mock(Patient.class);
        XWikiContext context = mock(XWikiContext.class);
        XWiki wiki = mock(XWiki.class);
        DocumentModelBridge patientDoc = mock(XWikiDocument.class);

        this.setUpSettingConsents(idsHolder, patient, patientDoc, context, wiki);

        List<String> existingIds = new LinkedList<>();
        List<String> testIds = new LinkedList<>();
        existingIds.add(consentMocks.id1);
        existingIds.add(consentMocks.id2);
        testIds.add("id_nonexistent");
        testIds.addAll(existingIds);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, testIds);

        verify(idsHolder, times(1)).set(eq("granted"), eq(existingIds), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }
}
