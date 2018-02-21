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
public class RecordElementOptionTest
{
    @Test
    public void valueOfWorks() throws ComponentLookupException
    {
        Assert.assertSame(RecordElementOption.READ_ONLY, RecordElementOption.valueOf("READ_ONLY"));
        Assert.assertSame(RecordElementOption.WRITE_ONCE, RecordElementOption.valueOf("WRITE_ONCE"));
        Assert.assertSame(RecordElementOption.SOFT_MANDATORY, RecordElementOption.valueOf("SOFT_MANDATORY"));
        Assert.assertSame(RecordElementOption.HARD_MANDATORY, RecordElementOption.valueOf("HARD_MANDATORY"));
        Assert.assertSame(RecordElementOption.PREREQUESTED, RecordElementOption.valueOf("PREREQUESTED"));
        Assert.assertSame(RecordElementOption.SOFT_UNIQUE, RecordElementOption.valueOf("SOFT_UNIQUE"));
        Assert.assertSame(RecordElementOption.HARD_UNIQUE, RecordElementOption.valueOf("HARD_UNIQUE"));
        Assert.assertSame(RecordElementOption.PER_USER_UNIQUENESS, RecordElementOption.valueOf("PER_USER_UNIQUENESS"));
    }

    @Test
    public void toStringLowercases() throws ComponentLookupException
    {
        Assert.assertEquals("read_only", RecordElementOption.READ_ONLY.toString());
    }
}
