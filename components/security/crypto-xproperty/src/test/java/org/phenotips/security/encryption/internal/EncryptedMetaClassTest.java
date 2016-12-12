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

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

import static org.mockito.Mockito.when;

public class EncryptedMetaClassTest
{
    @Rule
    public MockitoComponentMockingRule<PropertyMetaClass> mocker =
        new MockitoComponentMockingRule<PropertyMetaClass>(EncryptedMetaClass.class);

    @Mock
    private PropertyClassInterface pci;

    @Test
    public void PropertyClassInterfaceTest() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        when(this.mocker.getComponentUnderTest().getInstance()).thenReturn(this.pci);
    }
}
