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
package org.phenotips.measurements.internal;

import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

/**
 * Tests for the {@link ChestCircumferenceMeasurementHandler} component, and a few methods from the base @{link
 * {@link AbstractMeasurementHandler} class.
 *
 * @version $Id$
 * @since 1.4
 */
public class ChestCircumferenceTest
{
    /** Used to resolve vocabulary terms for associated phenotypes. */
    @Inject
    private VocabularyManager vocabularyManager;

    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(ChestCircumferenceMeasurementHandler.class);

    private ChestCircumferenceMeasurementHandler getComponent() throws ComponentLookupException
    {
        return (ChestCircumferenceMeasurementHandler) this.mocker.getComponentUnderTest();
    }

    @Test
    public void nameIsChest() throws ComponentLookupException
    {
        Assert.assertSame("chest", getComponent().getName());
    }

    @Test
    public void unitIsCM() throws ComponentLookupException
    {
        Assert.assertSame("cm", getComponent().getUnit());
    }

    @Test
    public void isComputed() throws ComponentLookupException
    {
        Assert.assertFalse(getComponent().isComputed());
    }

    @Test
    public void isNotDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(getComponent().isDoubleSided());
    }

    @Test
    public void hasNoChartConfiguration() throws ComponentLookupException
    {
        Assert.assertEquals(0, getComponent().getChartsConfigurations().size());
    }

    @Test
    public void checkAssociatedTerms() throws ComponentLookupException
    {
        Assert.assertTrue(getComponent().getAssociatedTerms(3.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(2.0)
            .contains(this.vocabularyManager.resolveTerm("HP:0005253")));
        Assert.assertEquals(1,getComponent().getAssociatedTerms(2.0).size());
        Assert.assertTrue(getComponent().getAssociatedTerms(-2.0)
            .contains(this.vocabularyManager.resolveTerm("HP:0000774")));
        Assert.assertEquals(1,getComponent().getAssociatedTerms(-2.0).size());
        Assert.assertTrue(getComponent().getAssociatedTerms(-3.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(null).isEmpty());
    }
}
