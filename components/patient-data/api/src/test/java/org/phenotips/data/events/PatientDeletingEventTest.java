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

public class PatientDeletingEventTest
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
        PatientDeletingEvent evt = new PatientDeletingEvent();
        Assert.assertEquals("patientRecordDeleting", evt.getEventType());

        evt = new PatientDeletingEvent(this.patient, this.user);
        Assert.assertEquals("patientRecordDeleting", evt.getEventType());
    }

    @Test
    public void getPatient()
    {
        PatientDeletingEvent evt = new PatientDeletingEvent();
        Assert.assertNull(evt.getPatient());

        evt = new PatientDeletingEvent(this.patient, this.user);
        Assert.assertEquals(this.patient, evt.getPatient());
    }

    @Test
    public void getAuthor()
    {
        PatientDeletingEvent evt = new PatientDeletingEvent();
        Assert.assertNull(evt.getAuthor());

        evt = new PatientDeletingEvent(this.patient, this.user);
        Assert.assertEquals(this.user, evt.getAuthor());
    }

    @Test
    public void matches()
    {
        PatientDeletingEvent evt1 = new PatientDeletingEvent();
        Assert.assertTrue(evt1.matches(evt1));

        PatientDeletingEvent evt2 = new PatientDeletingEvent(this.patient, this.user);
        Assert.assertTrue(evt1.matches(evt2));
        Assert.assertFalse(evt2.matches(evt1));

        Patient p2 = mock(Patient.class);
        when(p2.getDocumentReference()).thenReturn(new DocumentReference("instance", "data", "P0000002"));
        PatientDeletingEvent evt3 = new PatientDeletingEvent(p2, this.user);
        Assert.assertTrue(evt1.matches(evt3));
        Assert.assertFalse(evt2.matches(evt3));

        PatientDeletedEvent evt4 = new PatientDeletedEvent(this.patient, this.user);
        Assert.assertFalse(evt1.matches(evt4));
        Assert.assertFalse(evt2.matches(evt4));

        DocumentDeletingEvent evt5 = new DocumentDeletingEvent(this.pdoc);
        Assert.assertFalse(evt1.matches(evt5));
        Assert.assertFalse(evt2.matches(evt5));
    }

    @Test
    public void cancel()
    {
        PatientDeletingEvent evt = new PatientDeletingEvent();
        Assert.assertFalse(evt.isCanceled());
        Assert.assertNull(evt.getReason());

        evt.cancel();
        Assert.assertTrue(evt.isCanceled());
        Assert.assertNull(evt.getReason());

        evt.cancel("Because");
        Assert.assertTrue(evt.isCanceled());
        Assert.assertEquals("Because", evt.getReason());

        evt.cancel("Reason!");
        Assert.assertTrue(evt.isCanceled());
        Assert.assertEquals("Reason!", evt.getReason());

        evt.cancel();
        Assert.assertTrue(evt.isCanceled());
        Assert.assertNull(evt.getReason());
    }
}
