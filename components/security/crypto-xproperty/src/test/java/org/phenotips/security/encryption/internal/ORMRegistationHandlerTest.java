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
package org.phenotips.security.encryption.internal;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Assert;
import org.junit.Before;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

import java.util.List;

import javax.inject.Inject;


public class ORMRegistationHandlerTest {
	
	@Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(ORMRegistrationHandler.class);

	@Mock
    private HibernateSessionFactory sessionFactory;
	
	private static final String HANDLER_NAME = "phenotips-encrypted-xproperty-orm-registration";
	
	@Before
	public void setUp()throws InitializationException
	{
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
    public void hasProperName() throws ComponentLookupException
    {
        Assert.assertEquals(HANDLER_NAME, this.mocker.getComponentUnderTest().getName());
    }
	
	@Test
    public void listensForApplicationStartedEvents() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertFalse(events.isEmpty());
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.iterator().next().matches(new ApplicationStartedEvent()));
      
    }

}
