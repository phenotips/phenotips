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
package org.phenotips.data;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link ConsentStatus}.
 *
 * @version $Id$
 */
public class FeatureMetadatumTest
{
    @Test
    public void classReferenceIsCorrect() throws ComponentLookupException
    {
        Assert.assertEquals("PhenotypeMetaClass", FeatureMetadatum.CLASS_REFERENCE.getName());
        Assert.assertEquals("PhenoTips", FeatureMetadatum.CLASS_REFERENCE.extractReference(EntityType.SPACE).getName());
        Assert.assertNull(FeatureMetadatum.CLASS_REFERENCE.extractReference(EntityType.WIKI));
    }
}
