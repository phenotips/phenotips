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
package org.phenotips.entities.spi;

import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.entities.PrimaryEntityManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A sample primary entity connections manager implementation used for tests. This connection tests:
 * <ul>
 * <li>the ability to override the XClass and XProperty used for storing the connection</li>
 * <li>the ability to store the connection in another primary entity XObject</li>
 * <li>the simple property aspect of a connection, using {@link #get} and {@link #set} as the primary access
 * methods</li>
 * </ul>
 *
 * @version $Id$
 */
@Component
@Named("person-has-job")
@Singleton
public class PersonHasJobConnectionsManager
    extends AbstractOutgoingPrimaryEntityConnectionsManager<Person, Job>
    implements PrimaryEntityConnectionsManager<Person, Job>
{
    public static final String REFERENCE_PROPERTY = "job";

    @Inject
    @Named("Person")
    private PrimaryEntityManager<Person> personsManager;

    @Inject
    @Named("Job")
    private PrimaryEntityManager<Job> jobsManager;

    @Override
    public void initialize() throws InitializationException
    {
        super.subjectsManager = this.personsManager;
        super.objectsManager = this.jobsManager;
    }

    @Override
    public boolean connect(Person subject, Job object)
    {
        if (!ObjectUtils.allNotNull(subject, object)) {
            throw new IllegalArgumentException();
        }

        return storeConnection(subject, object, subject.getXDocument(), object.getDocumentReference(), true);
    }

    @Override
    public boolean disconnect(Person subject, Job object)
    {
        if (!ObjectUtils.allNotNull(subject, object)) {
            throw new IllegalArgumentException();
        }
        return deleteConnection(subject, object, subject.getXDocument(), object.getDocumentReference(), true);
    }

    @Override
    protected EntityReference getConnectionXClass()
    {
        return Person.CLASS_REFERENCE;
    }

    @Override
    protected String getReferenceProperty()
    {
        return REFERENCE_PROPERTY;
    }
}
