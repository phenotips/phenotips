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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * A sample primary entity connections manager implementation used for tests.
 *
 * @version $Id$
 */
@Component
@Named("person-belongsTo-family/secure")
@Singleton
public class SecurePersonBelongsToFamilyConnectionsManager
    extends AbstractSecureIncomingPrimaryEntityConnectionsManager<Person, Family>
    implements PrimaryEntityConnectionsManager<Person, Family>
{
    @Inject
    @Named("Person")
    private PrimaryEntityManager<Person> personsManager;

    @Inject
    @Named("Family")
    private PrimaryEntityManager<Family> familiesManager;

    @Override
    public void initialize() throws InitializationException
    {
        super.subjectsManager = this.personsManager;
        super.objectsManager = this.familiesManager;
    }
}
