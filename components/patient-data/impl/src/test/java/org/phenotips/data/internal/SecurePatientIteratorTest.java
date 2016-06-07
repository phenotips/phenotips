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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link SecurePatientRepository} component.
 *
 * @version $Id$
 * @since 1.3M1
 */
public class SecurePatientIteratorTest
{
    @Mock
    private Patient patient;

    private DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "jdoe");

    private DocumentReference patientReference = new DocumentReference("xwiki", "data", "P0123456");

    @Mock
    private AuthorizationManager access;

    @Mock
    private DocumentAccessBridge bridge;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        when(this.bridge.getCurrentUserReference()).thenReturn(this.currentUser);
        when(this.patient.getDocument()).thenReturn(this.patientReference);
    }

    @Test
    public void emptyIteratorTests() throws ComponentLookupException
    {
        List<Patient> input = new LinkedList<>();

        SecurePatientIterator iterator = new SecurePatientIterator(input.iterator(), this.access, this.bridge);
        Assert.assertFalse(iterator.hasNext());
        boolean exception = false;
        try {
            iterator.next();
        } catch (NoSuchElementException ex) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void nonEmptyIteratorTest() throws ComponentLookupException
    {
        List<Patient> input = new LinkedList<>();

        Patient p1 = mock(Patient.class);
        input.add(p1);
        Patient p2 = mock(Patient.class);
        input.add(p2);
        Patient p3 = mock(Patient.class);
        input.add(p3);

        SecurePatientIterator iterator = new SecurePatientIterator(input.iterator(), this.access, this.bridge);
        Assert.assertFalse(iterator.hasNext());
        boolean exception = false;
        try {
            iterator.next();
        } catch (NoSuchElementException ex) {
            exception = true;
        }
        Assert.assertTrue(exception);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeThrowsUnsupportedOperationException() throws UnsupportedOperationException
    {
        List<Patient> input = new LinkedList<>();
        SecurePatientIterator iterator = new SecurePatientIterator(input.iterator(), this.access, this.bridge);
        iterator.remove();
    }

    @Test
    public void findNextPatientTest() throws ComponentLookupException
    {
        List<Patient> input = new LinkedList<>();
        Patient p1 = mock(Patient.class);
        when(p1.getDocument()).thenReturn(mock(DocumentReference.class));
        when(this.access.hasAccess(
            Right.VIEW, this.currentUser, p1.getDocument())).thenReturn(false);
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(p2.getDocument()).thenReturn(mock(DocumentReference.class));
        when(this.access.hasAccess(
            Right.VIEW, this.currentUser, p2.getDocument())).thenReturn(true);
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(p3.getDocument()).thenReturn(mock(DocumentReference.class));
        when(this.access.hasAccess(
            Right.VIEW, this.currentUser, p3.getDocument())).thenReturn(true);
        input.add(p3);

        SecurePatientIterator iterator = new SecurePatientIterator(input.iterator(), this.access, this.bridge);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertSame(p2, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertSame(p3, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }
}
