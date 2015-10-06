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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ObstetricHistoryControllerTest {

    private static final Integer AGE_NON_ZERO = 1; // Arbitrary age.

    private static final Integer AGE_ZERO = 0;

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

    }

    @Test
    public void loadHandlesEmptyPatientTest(){
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));

        PatientData<Integer> testPatientData= this.obstetricHistoryController.load(this.patient);

        Assert.assertNull(testPatientData);
        verify(this.logger).debug("No data for patient [{}]", this.patientDocument);
    }

    @Test
    public void loadDefaultBehaviourTest(){
        doReturn(this.data).when(this.doc).getXObject(any(EntityReference.class));
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + GRAVIDA);
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + PARA);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + TERM);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + PRETERM);
        doReturn(AGE_ZERO).when(this.data).getIntValue(PREFIX + SAB);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + TAB);
        doReturn(AGE_NON_ZERO).when(this.data).getIntValue(PREFIX + LIVE_BIRTHS);
        
        PatientData<Integer> testPatientData = this.obstetricHistoryController.load(this.patient);

        Assert.assertEquals(testPatientData.getName(), "obstetric-history");
        Assert.assertTrue(testPatientData.get(TERM) == 1);
        Assert.assertTrue(testPatientData.get(PRETERM) == 1);
        Assert.assertTrue(testPatientData.get(TAB) == 1);
        Assert.assertTrue(testPatientData.get(LIVE_BIRTHS) == 1);
        Assert.assertEquals(testPatientData.size(), 4);

    }



}
