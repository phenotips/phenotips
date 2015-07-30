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
package org.phenotips.data.events;

import org.phenotips.data.Patient;

import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatientEditingCanceledEventTest
{
    @Mock
    private Patient patient;

    @Mock
    private User user;

    private DocumentReference pdoc = new DocumentReference("instance", "data", "P0000001");

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(this.patient.getDocument()).thenReturn(this.pdoc);
    }

    @Test
    public void getEventType()
    {
        PatientEditingCanceledEvent evt = new PatientEditingCanceledEvent();
        Assert.assertEquals("patientRecordEditingCanceled", evt.getEventType());

        evt = new PatientEditingCanceledEvent(this.patient, this.user);
        Assert.assertEquals("patientRecordEditingCanceled", evt.getEventType());
    }

    @Test
    public void getPatient()
    {
        PatientEditingCanceledEvent evt = new PatientEditingCanceledEvent();
        Assert.assertNull(evt.getPatient());

        evt = new PatientEditingCanceledEvent(this.patient, this.user);
        Assert.assertEquals(this.patient, evt.getPatient());
    }

    @Test
    public void getAuthor()
    {
        PatientEditingCanceledEvent evt = new PatientEditingCanceledEvent();
        Assert.assertNull(evt.getAuthor());

        evt = new PatientEditingCanceledEvent(this.patient, this.user);
        Assert.assertEquals(this.user, evt.getAuthor());
    }

    @Test
    public void matches()
    {
        PatientEditingCanceledEvent evt1 = new PatientEditingCanceledEvent();
        Assert.assertTrue(evt1.matches(evt1));

        PatientEditingCanceledEvent evt2 = new PatientEditingCanceledEvent(this.patient, this.user);
        Assert.assertTrue(evt1.matches(evt2));
        Assert.assertFalse(evt2.matches(evt1));

        Patient p2 = mock(Patient.class);
        when(p2.getDocument()).thenReturn(new DocumentReference("instance", "data", "P0000002"));
        PatientEditingCanceledEvent evt3 = new PatientEditingCanceledEvent(p2, this.user);
        Assert.assertTrue(evt1.matches(evt3));
        Assert.assertFalse(evt2.matches(evt3));
    }
}
