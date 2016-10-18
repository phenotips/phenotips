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

import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatientChangedEventTest
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
        when(this.patient.getDocumentReference()).thenReturn(this.pdoc);
    }

    @Test
    public void getEventType()
    {
        PatientChangedEvent evt = new PatientChangedEvent();
        Assert.assertEquals("patientRecordChanged", evt.getEventType());

        evt = new PatientChangedEvent(this.patient, this.user);
        Assert.assertEquals("patientRecordChanged", evt.getEventType());
    }

    @Test
    public void getPatient()
    {
        PatientChangedEvent evt = new PatientChangedEvent();
        Assert.assertNull(evt.getPatient());

        evt = new PatientChangedEvent(this.patient, this.user);
        Assert.assertEquals(this.patient, evt.getPatient());
    }

    @Test
    public void getAuthor()
    {
        PatientChangedEvent evt = new PatientChangedEvent();
        Assert.assertNull(evt.getAuthor());

        evt = new PatientChangedEvent(this.patient, this.user);
        Assert.assertEquals(this.user, evt.getAuthor());
    }

    @Test
    public void matches()
    {
        PatientChangedEvent evt1 = new PatientChangedEvent();
        Assert.assertTrue(evt1.matches(evt1));

        PatientChangedEvent evt2 = new PatientChangedEvent(this.patient, this.user);
        Assert.assertTrue(evt1.matches(evt2));
        Assert.assertFalse(evt2.matches(evt1));

        Patient p2 = mock(Patient.class);
        when(p2.getDocumentReference()).thenReturn(new DocumentReference("instance", "data", "P0000002"));
        PatientChangedEvent evt3 = new PatientChangedEvent(p2, this.user);
        Assert.assertTrue(evt1.matches(evt3));
        Assert.assertFalse(evt2.matches(evt3));

        PatientChangingEvent evt4 = new PatientChangingEvent(this.patient, this.user);
        Assert.assertFalse(evt1.matches(evt4));
        Assert.assertFalse(evt2.matches(evt4));

        DocumentUpdatedEvent evt5 = new DocumentUpdatedEvent(this.pdoc);
        Assert.assertFalse(evt1.matches(evt5));
        Assert.assertFalse(evt2.matches(evt5));
    }
}
