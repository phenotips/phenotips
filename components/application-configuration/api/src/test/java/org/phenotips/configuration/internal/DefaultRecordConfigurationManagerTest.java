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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link RecordConfigurationManager} implementation, {@link DefaultRecordConfigurationManager}.
 *
 * @version $Id$
 */
public class DefaultRecordConfigurationManagerTest
{
    private static final String PATIENT_LABEL = "patient";

    @Rule
    public final MockitoComponentMockingRule<RecordConfigurationManager> mocker =
        new MockitoComponentMockingRule<>(
            DefaultRecordConfigurationManager.class);

    @Mock
    private RecordConfiguration config;

    @Mock
    private RecordConfigurationModule moduleOne;

    @Mock
    private RecordConfigurationModule moduleTwo;

    @Mock
    private RecordConfigurationModule moduleThree;

    @Mock
    private Provider<List<RecordConfigurationModule>> modules;

    private List<RecordConfigurationModule> moduleList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        Type listType = new DefaultParameterizedType(null, List.class,
            RecordConfigurationModule.class);
        Type providerType = new DefaultParameterizedType(null, Provider.class, listType);
        this.mocker.registerComponent(providerType, this.modules);
        when(this.moduleOne.supportsRecordType(PATIENT_LABEL)).thenReturn(true);
        when(this.moduleTwo.supportsRecordType(PATIENT_LABEL)).thenReturn(true);
        when(this.moduleThree.supportsRecordType(PATIENT_LABEL)).thenReturn(true);
    }

    @Test
    public void emptyConfigurationCreatedByDefault() throws ComponentLookupException
    {
        doReturn(new LinkedList<>()).when(this.modules).get();
        RecordConfiguration result = this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getAllSections().isEmpty());
    }

    @Test
    public void moduleOutputIsUsed() throws Exception
    {
        this.moduleList = Collections.singletonList(this.moduleOne);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(this.config);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @Test
    public void modulesAreCascaded() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo, this.moduleThree);
        doReturn(this.moduleList).when(this.modules).get();
        RecordConfiguration tempConfig = Mockito.mock(RecordConfiguration.class);
        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(null);
        when(this.moduleTwo.process(null)).thenReturn(tempConfig);
        when(this.moduleThree.process(tempConfig)).thenReturn(this.config);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
        InOrder order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree);
        order.verify(this.moduleOne).process(any(RecordConfiguration.class));
        order.verify(this.moduleTwo).process(null);
        order.verify(this.moduleThree).process(tempConfig);
    }

    @Test
    public void allModulesAreInvoked() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(this.config);
        when(this.moduleTwo.process(this.config)).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(null);
        when(this.moduleTwo.process(null)).thenReturn(this.config);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @Test
    public void exceptionsInModulesAreIgnored() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenThrow(new NullPointerException());
        when(this.moduleTwo.process(any(RecordConfiguration.class))).thenReturn(this.config);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getActiveConfigurationReturnsPatientRecordConfiguration() throws Exception
    {
        this.moduleList = Collections.singletonList(this.moduleOne);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(RecordConfiguration.class))).thenReturn(this.config);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getActiveConfiguration());
    }
}
