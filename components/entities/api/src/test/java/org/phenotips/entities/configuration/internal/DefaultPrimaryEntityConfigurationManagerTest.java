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
package org.phenotips.entities.configuration.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.PrimaryEntityConfigurationManager;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationModule;

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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PrimaryEntityConfigurationManager} implementation,
 * {@link DefaultPrimaryEntityConfigurationManager}.
 *
 * @version $Id$
 */
public class DefaultPrimaryEntityConfigurationManagerTest
{
    private static final String PATIENT_LABEL = "patient";

    @Rule
    public final MockitoComponentMockingRule<PrimaryEntityConfigurationManager> mocker =
        new MockitoComponentMockingRule<>(DefaultPrimaryEntityConfigurationManager.class);

    @Mock
    private PrimaryEntityConfigurationBuilder configBuilder;

    @Mock
    private PrimaryEntityConfiguration config;

    @Mock
    private PrimaryEntityConfigurationModule moduleOne;

    @Mock
    private PrimaryEntityConfigurationModule moduleTwo;

    @Mock
    private PrimaryEntityConfigurationModule moduleThree;

    @Mock
    private PrimaryEntityConfigurationModule moduleFour;

    @Mock
    private Provider<List<PrimaryEntityConfigurationModule>> modules;

    @Mock
    private PrimaryEntity entity;

    @Mock
    private PrimaryEntityManager<PrimaryEntity> manager;

    private PrimaryEntityResolver resolver;

    private List<PrimaryEntityConfigurationModule> moduleList;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(this.configBuilder.build()).thenReturn(this.config);
        Type listType = new DefaultParameterizedType(null, List.class,
            PrimaryEntityConfigurationModule.class);
        Type providerType = new DefaultParameterizedType(null, Provider.class, listType);
        this.mocker.registerComponent(providerType, this.modules);
        when(this.moduleOne.supportsEntityType(PATIENT_LABEL)).thenReturn(true);
        when(this.moduleOne.supportsRecord(this.entity)).thenReturn(true);
        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity)))
            .thenReturn(this.configBuilder);
        when(this.moduleTwo.supportsEntityType(PATIENT_LABEL)).thenReturn(true);
        when(this.moduleTwo.supportsRecord(this.entity)).thenReturn(true);
        when(this.moduleTwo.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        when(this.moduleTwo.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity)))
            .thenReturn(this.configBuilder);
        when(this.moduleThree.supportsEntityType(PATIENT_LABEL)).thenReturn(false);
        when(this.moduleThree.supportsRecord(this.entity)).thenReturn(false);
        when(this.moduleThree.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        when(this.moduleThree.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity)))
            .thenReturn(this.configBuilder);
        when(this.moduleFour.supportsEntityType(PATIENT_LABEL)).thenReturn(true);
        when(this.moduleFour.supportsRecord(this.entity)).thenReturn(true);
        when(this.moduleFour.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        when(this.moduleFour.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity)))
            .thenReturn(this.configBuilder);
        when(this.entity.getTypeId()).thenReturn(PATIENT_LABEL);
        this.resolver = this.mocker.getInstance(PrimaryEntityResolver.class);
        Mockito.doReturn(this.manager).when(this.resolver).getEntityManager(PATIENT_LABEL);
    }

    @Test
    public void emptyConfigurationCreatedByDefault() throws ComponentLookupException
    {
        doReturn(new LinkedList<>()).when(this.modules).get();
        PrimaryEntityConfiguration result = this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getRecordConfiguration().getAllSections().isEmpty());
    }

    @Test
    public void moduleOutputIsUsed() throws Exception
    {
        this.moduleList = Collections.singletonList(this.moduleOne);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @Test
    public void modulesAreCascaded() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo, this.moduleThree, this.moduleFour);
        doReturn(this.moduleList).when(this.modules).get();
        PrimaryEntityConfigurationBuilder tempConfig = Mockito.mock(PrimaryEntityConfigurationBuilder.class);
        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(null);
        when(this.moduleTwo.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(tempConfig);
        when(this.moduleFour.process(tempConfig)).thenReturn(this.configBuilder);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
        InOrder order = Mockito.inOrder(this.moduleOne, this.moduleTwo, this.moduleThree, this.moduleFour);
        order.verify(this.moduleOne).process(any(PrimaryEntityConfigurationBuilder.class));
        order.verify(this.moduleTwo).process(any(PrimaryEntityConfigurationBuilder.class));
        order.verify(this.moduleThree, Mockito.never()).process(any(PrimaryEntityConfigurationBuilder.class));
        order.verify(this.moduleFour).process(tempConfig);
    }

    @Test
    public void allModulesAreInvokedForEntityType() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(this.configBuilder);
        when(this.moduleTwo.process(this.configBuilder)).thenReturn(null);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class))).thenReturn(null);
        when(this.moduleTwo.process(null)).thenReturn(this.configBuilder);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @Test
    public void allModulesAreInvokedForRecord() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity)))
            .thenReturn(this.configBuilder);
        when(this.moduleTwo.process(this.configBuilder, this.entity)).thenReturn(null);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(this.entity));

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class), same(this.entity))).thenReturn(null);
        when(this.moduleTwo.process(null, this.entity)).thenReturn(this.configBuilder);

        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(this.entity));
    }

    @Test
    public void exceptionsInModulesAreIgnored() throws Exception
    {
        this.moduleList = Arrays.asList(this.moduleOne, this.moduleTwo);
        doReturn(this.moduleList).when(this.modules).get();

        when(this.moduleOne.process(any(PrimaryEntityConfigurationBuilder.class)))
            .thenThrow(new NullPointerException());
        Assert.assertSame(this.config, this.mocker.getComponentUnderTest().getConfiguration(PATIENT_LABEL));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullEntityTypeIsRefused() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().getConfiguration((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRecordIsRefused() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().getConfiguration((PrimaryEntity) null);
    }
}
