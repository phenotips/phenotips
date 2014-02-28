/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.push.script;

import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PushPatientScriptService} component.
 *
 * @version $Id$
 * @since 1.0M11
 */
public class PatientDataScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<PushPatientScriptService> mocker =
        new MockitoComponentMockingRule<PushPatientScriptService>(PushPatientScriptService.class);

    @Test
    public void sendPatientForwardsCalls() throws ComponentLookupException
    {
        //PushPatientData component = this.mocker.getInstance(PushPatientData.class);
        //when(component.sendPatient(...)).thenReturn(-1);
        //Assert.assertEquals(-1, this.mocker.getComponentUnderTest().sendPatient(new LinkedList<Patient>(), "nowhere"));
    }
}
