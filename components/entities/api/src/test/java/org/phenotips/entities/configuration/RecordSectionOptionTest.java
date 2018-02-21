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
package org.phenotips.entities.configuration;

import org.xwiki.component.manager.ComponentLookupException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link ConsentStatus}.
 *
 * @version $Id$
 */
public class RecordSectionOptionTest
{
    @Test
    public void valueOfWorks() throws ComponentLookupException
    {
        Assert.assertSame(RecordSectionOption.EXPANDED_BY_DEFAULT, RecordSectionOption.valueOf("EXPANDED_BY_DEFAULT"));
    }

    @Test
    public void toStringLowercases() throws ComponentLookupException
    {
        Assert.assertEquals("expanded_by_default", RecordSectionOption.EXPANDED_BY_DEFAULT.toString());
    }
}
