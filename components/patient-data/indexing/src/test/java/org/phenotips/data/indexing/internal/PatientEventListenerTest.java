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
package org.phenotips.data.indexing.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.data.events.PatientEvent;
import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PatientEventListenerTest
{

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(PatientEventListener.class);

    @Mock
    private PatientIndexer patientIndexer;

    @Mock
    private Patient patient;

    private EventListener eventListener;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.eventListener = this.mocker.getComponentUnderTest();
        this.patientIndexer = this.mocker.getInstance(PatientIndexer.class);
    }

    @Test
    public void deletePatientTest()
    {
        PatientEvent patientDeleteEvent = mock(PatientDeletedEvent.class);
        doReturn(this.patient).when(patientDeleteEvent).getPatient();

        this.eventListener.onEvent(patientDeleteEvent, mock(Object.class), mock(Object.class));
        verify(this.patientIndexer).delete(this.patient);
    }

    @Test
    public void indexPatientTest()
    {
        PatientEvent patientEvent = mock(PatientEvent.class);
        doReturn(this.patient).when(patientEvent).getPatient();

        this.eventListener.onEvent(patientEvent, mock(Object.class), mock(Object.class));
        verify(this.patientIndexer).index(this.patient);
    }
}
