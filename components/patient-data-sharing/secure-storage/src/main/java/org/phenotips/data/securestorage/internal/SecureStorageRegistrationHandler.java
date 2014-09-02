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
package org.phenotips.data.securestorage.internal;

import org.phenotips.data.securestorage.LocalLoginToken;
import org.phenotips.data.securestorage.PatientPushedToInfo;
import org.phenotips.data.securestorage.PatientSourceServerInfo;
import org.phenotips.data.securestorage.RemoteLoginData;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Registers {@link LocalLoginToken storage entity} and {@link RemoteLoginData storage entity} in the Hibernate
 * configuration at startup, since Hibernate doesn't have a clean mechanism for auto-registering optional modular
 * entities at runtime.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("phenotips-data-secure-storage-registration")
@Singleton
public class SecureStorageRegistrationHandler implements EventListener
{
    /** The Hibernate session factory where the entity must be registered. */
    @Inject
    private HibernateSessionFactory sessionFactory;

    @Override
    public String getName()
    {
        return "phenotips-data-secure-storage-registration";
    }

    @Override
    public List<Event> getEvents()
    {
        return Collections.<Event>singletonList(new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.sessionFactory.getConfiguration().addAnnotatedClass(LocalLoginToken.class);
        this.sessionFactory.getConfiguration().addAnnotatedClass(RemoteLoginData.class);
        this.sessionFactory.getConfiguration().addAnnotatedClass(PatientSourceServerInfo.class);
        this.sessionFactory.getConfiguration().addAnnotatedClass(PatientPushedToInfo.class);
    }
}
