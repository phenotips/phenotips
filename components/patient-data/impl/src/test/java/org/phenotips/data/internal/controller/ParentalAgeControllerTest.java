package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.junit.Assert;
import org.mockito.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ParentalAgeControllerTest {

    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

    private static final Integer AGE_NON_ZERO = 25; // Arbitrary age.

    private static final Integer AGE_ZERO = 0;

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

        doReturn(AGE_ZERO).when(data).getIntValue(MATERNAL_AGE);
        doReturn(AGE_ZERO).when(data).getIntValue(PATERNAL_AGE);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertNull(testData);

    }

    @Test
    public void loadHandlesExceptions(){

    }

    @Test
    public void saveDefaultBehaviourTest(){

    }

    @Test
    public void saveHandlesExceptions(){

    }
}
