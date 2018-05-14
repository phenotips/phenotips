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

import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.lang.reflect.Type;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * A sample primary entity manager implementation used for tests.
 *
 * @version $Id$
 */
@Component
@Named("Person")
@Singleton
public class PersonsManager extends AbstractPrimaryEntityManager<Person> implements PrimaryEntityManager<Person>
{
    public static final Type TYPE = new DefaultParameterizedType(null, PrimaryEntityManager.class, Person.class);

    private static final EntityReference DEFAULT_DATA_SPACE = new EntityReference("Persons", EntityType.SPACE);

    @Override
    public EntityReference getDataSpace()
    {
        return DEFAULT_DATA_SPACE;
    }

    @Override
    public String getType()
    {
        return "persons";
    }
}
