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
package org.phenotips.panels.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelsPatientResourceImpl}.
 */
public class DefaultGenePanelsPatientResourceImplTest
{
    private static final String PATIENT_1 = "P0001";

    private static final String TEST = "test";

    @Rule
    public MockitoComponentMockingRule<DefaultGenePanelsPatientResourceImpl> mocker =
        new MockitoComponentMockingRule<>(DefaultGenePanelsPatientResourceImpl.class);

    @Mock
    private Patient patient1;

    @Mock
    private GenePanel genePanel;

    private DefaultGenePanelsPatientResourceImpl component;

    private Logger logger;

    private GenePanelFactory genePanelFactory;

    private PatientRepository repository;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager compManager = this.mocker.getInstance(ComponentManager.class, "context");
        final Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        final XWikiContext context = provider.get();
        when(compManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(context);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.genePanelFactory = this.mocker.getInstance(GenePanelFactory.class);
        this.repository = this.mocker.getInstance(PatientRepository.class, "secure");
    }

    @Test
    public void getPatientGeneCountsPatientIdIsNull()
    {
        final Response response = this.component.getPatientGeneCounts(null, false);
        verify(this.logger).error("No patient ID was provided.");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientGeneCountsPatientIdIsEmpty()
    {
        final Response response = this.component.getPatientGeneCounts(StringUtils.EMPTY, false);
        verify(this.logger).error("No patient ID was provided.");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientGeneCountsPatientIdIsBlank()
    {
        final Response response = this.component.getPatientGeneCounts(StringUtils.SPACE, false);
        verify(this.logger).error("No patient ID was provided.");
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientGeneCountsUserNotAuthorized()
    {
        when(this.repository.get(PATIENT_1)).thenThrow(new SecurityException());
        final Response response = this.component.getPatientGeneCounts(PATIENT_1, false);
        verify(this.logger).error("View access denied on patient record [{}]: {}", PATIENT_1, null);
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientGeneCountsPatientDoesNotExist()
    {
        when(this.repository.get(PATIENT_1)).thenReturn(null);
        final Response response = this.component.getPatientGeneCounts(PATIENT_1, false);
        verify(this.logger).error("Could not find patient with ID {}", PATIENT_1);
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void getPatientGeneCountsPanelIsEmpty()
    {
        when(this.repository.get(PATIENT_1)).thenReturn(this.patient1);
        when(this.genePanelFactory.build(this.patient1, false)).thenReturn(this.genePanel);
        when(this.genePanel.toJSON()).thenReturn(new JSONObject());
        final Response response = this.component.getPatientGeneCounts(PATIENT_1, false);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertTrue(new JSONObject().similar(response.getEntity()));
    }

    @Test
    public void getPatientGeneCountsPanelIsNotEmpty()
    {
        when(this.repository.get(PATIENT_1)).thenReturn(this.patient1);
        when(this.genePanelFactory.build(this.patient1, false)).thenReturn(this.genePanel);
        when(this.genePanel.toJSON()).thenReturn(new JSONObject().put(TEST, TEST));
        final Response response = this.component.getPatientGeneCounts(PATIENT_1, false);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertTrue(new JSONObject().put(TEST, TEST).similar(response.getEntity()));
    }
}
