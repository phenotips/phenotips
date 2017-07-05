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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link ConsentStatus}.
 *
 * @version $Id$
 */
public class ConsentStatusTest
{
    @Test
    public void stringResolveToCorrectEnum() throws ComponentLookupException
    {
        Assert.assertSame(ConsentStatus.fromString("yes"), ConsentStatus.YES);
        Assert.assertSame(ConsentStatus.fromString("no"), ConsentStatus.NO);
        Assert.assertSame(ConsentStatus.fromString("not_set"), ConsentStatus.NOT_SET);
    }

    @Test
    public void incorrectStringDoesNotResolveToEnum() throws ComponentLookupException
    {
        Assert.assertNotSame(ConsentStatus.fromString("Yes"), ConsentStatus.YES);
        Assert.assertNotSame(ConsentStatus.fromString("nO"), ConsentStatus.NO);
        Assert.assertNotSame(ConsentStatus.fromString("notset"), ConsentStatus.NOT_SET);
    }
}
