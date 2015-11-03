package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.junit.Assert;
import org.mockito.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ParentalAgeControllerTest {

    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

    private static final Integer AGE_NON_ZERO = 25; // Arbitrary age.

    private static final Integer AGE_ZERO = 0;

    public static final EntityReference CLASS_REFERENCE =
            new EntityReference("ParentalInformationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
            new MockitoComponentMockingRule<PatientDataController<Integer>>(ParentalAgeController.class);

    @Mock
    private Logger logger;

    private Provider<XWikiContext> provider;

    private XWikiContext xWikiContext;

    @Mock
    private DocumentAccessBridge documentAccessBridge;

    private DocumentReference patientDocument = new DocumentReference("xwiki", "patient", "0000001");

    private ParentalAgeController parentalAgeController;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.parentalAgeController = (ParentalAgeController) mocker.getComponentUnderTest();
        this.logger = mocker.getMockedLogger();

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = this.provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        doReturn(this.patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(this.patientDocument);

    }

    @Test
    public void loadEmptyPatientTest(){
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));
        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertNull(testData);
        verify(this.logger).debug("No parental information for patient [{}]", this.patientDocument);
    }

    @Test
    public void loadMaternalAndPaternalAgeNonZero(){
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));

        doReturn(AGE_NON_ZERO).when(data).getIntValue(MATERNAL_AGE);
        doReturn(AGE_NON_ZERO).when(data).getIntValue(PATERNAL_AGE);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);

        Assert.assertEquals("parentalAge", testData.getName());
        Assert.assertTrue(testData.get(MATERNAL_AGE) == 25);
        Assert.assertTrue(testData.get(PATERNAL_AGE) == 25);

    }

    @Test
    public void loadMaternalAndPaternalAgeZero(){
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));
        doReturn(AGE_ZERO).when(data).getIntValue(MATERNAL_AGE);
        doReturn(AGE_ZERO).when(data).getIntValue(PATERNAL_AGE);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertNull(testData);

    }

    @Test
    public void loadHandlesExceptions() throws Exception {
        Exception testException = new Exception("Test Exception");
        doThrow(testException).when(this.documentAccessBridge).getDocument(this.patientDocument);

        this.parentalAgeController.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", testException.getMessage());
    }

    @Test
    public void saveEmptyPatientTest() throws XWikiException {
        PatientData<Integer> patientData = mock(PatientData.class);
        doReturn(patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(false).when(patientData).isNamed();
        this.parentalAgeController.save(this.patient);
        verifyNoMoreInteractions(this.doc);
        verify(this.xWikiContext.getWiki(), never()).saveDocument(this.doc,
                "Updated parental age from JSON", true, this.xWikiContext);
    }

    @Test
    public void saveDefaultBehaviourTest() throws XWikiException {
        PatientData<Integer> patientData = mock(PatientData.class);
        BaseObject data = mock(BaseObject.class);
        doReturn(patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(true).when(patientData).isNamed();
        doReturn(data).when(this.doc).getXObject(CLASS_REFERENCE, true, this.xWikiContext);

        doReturn(AGE_NON_ZERO).when(patientData).get(MATERNAL_AGE);
        doReturn(AGE_NON_ZERO).when(patientData).get(PATERNAL_AGE);

        this.parentalAgeController.save(this.patient);

        verify(this.xWikiContext.getWiki()).saveDocument(this.doc,
                "Updated parental age from JSON", true, this.xWikiContext);
    }

    @Test
    public void saveHandlesExceptions() throws Exception {
        Exception testException = new Exception("Test Exception");
        doThrow(testException).when(this.documentAccessBridge).getDocument(this.patientDocument);

        this.parentalAgeController.save(this.patient);
        verify(this.logger).error("Failed to save parental age: [{}]", testException.getMessage());

    }

    @Test
    public void readJSONDefaultBehaviourTest(){

    }

    @Test
    public void writeJSONDefaultBehaviourTest(){

    }

}
