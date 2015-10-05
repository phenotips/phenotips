package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
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
import static org.mockito.Mockito.verify;

public class ObstetricHistoryControllerTest {

    private static final String PREFIX = "pregnancy_history__";

    private static final String GRAVIDA = "gravida";

    private static final String PARA = "para";

    private static final String TERM = "term";

    private static final String PRETERM = "preterm";

    private static final String SAB = "sab";

    private static final String TAB = "tab";

    private static final String LIVE_BIRTHS = "births";

    @Rule
    public MockitoComponentMockingRule<PatientDataController> mocker =
            new MockitoComponentMockingRule<PatientDataController>(ObstetricHistoryController.class);

    private DocumentAccessBridge documentAccessBridge;

    private ObstetricHistoryController obstetricHistoryController;

    private DocumentReference patientDocument;

    @Mock
    private Patient patient;

    @Mock
    private Logger logger;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private Provider<XWikiContext> provider;

    private XWikiContext xWikiContext;

    @Mock
    private XWiki xwiki;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.patientDocument = new DocumentReference("wiki", "patient", "00000001");
        this.obstetricHistoryController =
                (ObstetricHistoryController) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        this.provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        doReturn(this.patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(this.patientDocument);
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));

    }

    @Test
    public void loadHandlesEmptyPatientTest(){

        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));

        PatientData<Integer> testPatientData= this.obstetricHistoryController.load(this.patient);

        Assert.assertNull(testPatientData);
        verify(this.logger).debug("No data for patient [{}]", this.patientDocument);

    }

}
