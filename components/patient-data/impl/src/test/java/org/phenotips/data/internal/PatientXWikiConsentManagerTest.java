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
import org.phenotips.data.ConsentStatus;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.codec.binary.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
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

public class PatientXWikiConsentManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<ConsentManager> mocker =
        new MockitoComponentMockingRule<ConsentManager>(PatientXWikiConsentManager.class);

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

        doReturn(mocks.id1).when(mocks.consentConfig1).getStringValue(mocks.idKey);
        doReturn(mocks.descr1).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.descriptionKey), anyString(),
            Matchers.eq(mocks.consentConfig1), any(XWikiContext.class));
        doReturn(mocks.req1).when(mocks.consentConfig1).getIntValue(mocks.requiredKey);
        doReturn(mocks.id2).when(mocks.consentConfig2).getStringValue(mocks.idKey);
        doReturn(mocks.descr2).when((XWikiDocument) configDoc).display(Matchers.eq(mocks.descriptionKey), anyString(),
            Matchers.eq(mocks.consentConfig2), any(XWikiContext.class));
        doReturn(mocks.req2).when(mocks.consentConfig2).getIntValue(mocks.requiredKey);

        return mocks;
    }

    private class ConsentConfigurationMocks
    {
        static final String idKey = "id";

        static final String descriptionKey = "description";

        static final String requiredKey = "required";

        static final String id1 = "id1";

        static final String descr1 = "clean description";

        Integer req1 = 1;

        Boolean req1B = true;

        static final String id2 = "id2";

        static final String descr2 = "non <div>clean</div> <p>description</p>";

        static final String descr2C = "non clean description";

        Integer req2 = 0;

        Boolean req2B = false;

        BaseObject consentConfig1 = mock(BaseObject.class);

        BaseObject consentConfig2 = mock(BaseObject.class);
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
     * For testing normal initialization, when there are consents configured in the system. Also tests for description
     * strings being properly cleaned.
     */
    @SuppressWarnings("static-access")
    @Test
    public void testConfiguredInitialization() throws Exception
    {
        ConsentConfigurationMocks mocks = this.setUpInitializationWithConfigurationMocks();

        Consent consent1 = this.mocker.getComponentUnderTest().getSystemConsents().get(0);
        Assert.assertSame(consent1.getId(), mocks.id1);
        Assert.assertSame(consent1.getDescription(), mocks.descr1);
        Assert.assertSame(consent1.isRequired(), mocks.req1B);
        Consent consent2 = this.mocker.getComponentUnderTest().getSystemConsents().get(1);
        Assert.assertSame(consent2.getId(), mocks.id2);
        Assert.assertTrue(StringUtils.equals(consent2.getDescription(), mocks.descr2C));
        Assert.assertSame(consent2.isRequired(), mocks.req2B);
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

        List<Consent> consents = this.mocker.getComponentUnderTest().loadConsentsFromPatient(patientId);
        Assert.assertFalse(consents.isEmpty());
        for (Consent consent : consents)
        {
            if (consentIds != null && consentIds.contains(consent.getId())) {
                Assert.assertSame(consent.getStatus(), ConsentStatus.YES);
            } else {
                Assert.assertSame(consent.getStatus(), ConsentStatus.NO);
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
        /* if there is no patient, it means that there are no consents granted. */
        Assert.assertFalse(this.mocker.getComponentUnderTest().loadConsentsFromPatient(patient).isEmpty());
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
        testIds.add("id3");
        testIds.addAll(existingIds);

        this.mocker.getComponentUnderTest().setPatientConsents(patient, testIds);

        verify(idsHolder, times(1)).set(eq("granted"), eq(existingIds), eq(context));
        verify(wiki, times(1)).saveDocument(eq((XWikiDocument) patientDoc), anyString(), eq(true), eq(context));
    }

    @Test
    public void testJson() throws ComponentLookupException
    {
        JSONObject j1 = new JSONObject("{j1: 1}");
        JSONObject j2 = new JSONObject("{j2: 2}");
        Consent c1 = mock(Consent.class);
        doReturn(j1).when(c1).toJson();
        Consent c2 = mock(Consent.class);
        doReturn(j2).when(c2).toJson();
        List<Consent> consents = new LinkedList<>();
        consents.add(c1);
        consents.add(c2);

        JSONArray json = this.mocker.getComponentUnderTest().toJson(consents);
        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() == 2);
        boolean found1 = false;
        boolean found2 = false;
        for (int i = 0; i < json.length(); ++i) {
            JSONObject o = json.getJSONObject(i);
            if (o.similar(j1)) {
                found1 = true;
            } else if (o.similar(j2)) {
                found2 = true;
            }
        }
        Assert.assertTrue(found1);
        Assert.assertTrue(found2);
    }
}
