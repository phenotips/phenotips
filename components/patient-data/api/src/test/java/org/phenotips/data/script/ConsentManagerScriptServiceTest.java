package org.phenotips.data.script;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

public class ConsentManagerScriptServiceTest {

    @Rule
    public final MockitoComponentMockingRule<ConsentManagerScriptService> mocker = new MockitoComponentMockingRule<>(
            ConsentManagerScriptService.class);

    private ConsentManager consentManager;

    @Mock
    private Patient patient;

    @Mock
    private JSONArray arr;

    @Mock
    private Consent consent1;

    @Mock
    private Consent consent2;

    @Before
    public void setUp() throws ComponentLookupException {
        MockitoAnnotations.initMocks(this);
        this.consentManager = this.mocker.getInstance(ConsentManager.class);
    }

    @Test
    public void hasConsentTest() throws ComponentLookupException {
        when(this.consentManager.hasConsent("P0123456", "consent")).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent"));
    }

    @Test
    public void hasNoConsentTest() throws ComponentLookupException {
        when(this.consentManager.hasConsent("P0123456", "consent")).thenReturn(false);
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasConsent("P0123456", "consent"));
    }

    @Test
    public void getAllConsentsForPatientTest() throws ComponentLookupException {
        Set<Consent> consents = new LinkedHashSet<>();
        consents.add(consent1);
        consents.add(consent2);

        when(this.consentManager.toJSON(consents)).thenReturn(this.arr);
        // Debug this assertion
        Assert.assertNotSame(this.arr,
                this.mocker.getComponentUnderTest().getAllConsentsForPatient(Patient.JSON_KEY_ID));
    }

    @Test
    public void invalidPatientId() throws ComponentLookupException {
        Set<Consent> consents = new LinkedHashSet<>();
        consents.add(consent1);
        consents.add(consent2);

        when(this.consentManager.toJSON(consents)).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getAllConsentsForPatient(Patient.JSON_KEY_ID));
    }

    @Test
    public void getConsentsForPatientWithException() throws ComponentLookupException {
        Set<Consent> consents = new LinkedHashSet<>();
        Assert.assertTrue(consents.isEmpty());

        when(this.consentManager.getAllConsentsForPatient(patient)).thenThrow(new NullPointerException());
        Assert.assertNull(this.mocker.getComponentUnderTest().getAllConsentsForPatient(Patient.JSON_KEY_ID));
    }

}
