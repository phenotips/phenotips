package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
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

public class ParentalAgeControllerTest {

    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

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

        doReturn(this.patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(this.patientDocument);

    }

    @Test
    public void loadEmptyPatientTest(){
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));
        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertNull(testData);
    }

    @Test
    public void loadDefaultBehaviourTest(){

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);


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
