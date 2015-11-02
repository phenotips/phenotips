package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientDataController;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;

import static org.mockito.Mockito.doReturn;

public class ParentalAgeControllerTest {

    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

    @Rule
    MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
            new MockitoComponentMockingRule<PatientDataController<Integer>>(ParentalAgeController.class);

    @Mock
    private Logger logger;

    private Provider<XWikiContext> provider;

    private XWikiContext xWikiContext;

    private DocumentAccessBridge documentAccessBridge;

    private DocumentReference patientDocument;

    private ParentalAgeController parentalAgeController;

    private XWiki xwiki;

    private Patient patient;

    private XWikiDocument doc;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.parentalAgeController = (ParentalAgeController) mocker.getComponentUnderTest();
        this.logger = mocker.getMockedLogger();
        this.patientDocument = new DocumentReference("xwiki", "patient", "0000001");

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = this.provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        doReturn(this.patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(this.patientDocument);

    }

    @Test
    public void loadDefaultBehaviourTest(){

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
