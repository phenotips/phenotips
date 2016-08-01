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
    private Patient p1;

    @Mock
    private Patient p2;

    @Mock
    private Patient p3;

    private DocumentReference currentUser = new DocumentReference("xwiki", "XWiki", "jdoe");

    private DocumentReference p1Reference = new DocumentReference("xwiki", "data", "P01");

    private DocumentReference p2Reference = new DocumentReference("xwiki", "data", "P02");

    private DocumentReference p3Reference = new DocumentReference("xwiki", "data", "P03");

    @Mock
    private AuthorizationManager access;

    @Mock
    private DocumentAccessBridge bridge;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        when(this.bridge.getCurrentUserReference()).thenReturn(this.currentUser);
        when(this.p1.getDocument()).thenReturn(this.p1Reference);
        when(this.p2.getDocument()).thenReturn(this.p2Reference);
        when(this.p3.getDocument()).thenReturn(this.p3Reference);
    }

    @Test
    public void emptyInputGivesEmptyIterator() throws ComponentLookupException
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
    public void onlyRestrictedEntriesGivesEmptyIterator() throws ComponentLookupException
    {
        List<Patient> input = new LinkedList<>();

        input.add(this.p1);
        input.add(this.p2);
        input.add(this.p3);

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
    public void basicFunctionalityTests() throws ComponentLookupException
    {
        List<Patient> input = new LinkedList<>();
        input.add(this.p1);
        input.add(this.p2);
        input.add(this.p3);
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.p1Reference)).thenReturn(false);
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.p2Reference)).thenReturn(true);
        when(this.access.hasAccess(Right.VIEW, this.currentUser, this.p3Reference)).thenReturn(true);

        SecurePatientIterator iterator = new SecurePatientIterator(input.iterator(), this.access, this.bridge);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertSame(this.p2, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertSame(this.p3, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }
}
