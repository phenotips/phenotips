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
package org.phenotips.security.encryption.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.objects.BaseStringProperty;

public class EncryptedPropertyTest
{
    @Rule
    public MockitoComponentMockingRule<BaseStringProperty> mocker =
        new MockitoComponentMockingRule<BaseStringProperty>(EncryptedProperty.class);

    @Test
    public void getsNullValueTest() throws ComponentLookupException
    {
        String val = null;

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getValue());
    }

    @Test
    public void setsNullValueTest() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().setValue(null);
    }
}
