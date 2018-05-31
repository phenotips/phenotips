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
package org.phenotips.data.internal.controller;

import org.phenotips.Constants;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles the parent's names.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { PatientDataController.class })
@Named("parentalNames")
@Singleton
public class ParentalNamesController extends AbstractSimpleController
{
    /** The XClass used for storing parental information. */
    public static final EntityReference CLASS_REFERENCE =
        new EntityReference("ParentalInformationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Override
    public String getName()
    {
        return "parentalNames";
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList("maternal_last_name", "maternal_first_name", "paternal_last_name", "paternal_first_name");
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "parental_names";
    }

    @Override
    protected EntityReference getStorageXClass()
    {
        return CLASS_REFERENCE;
    }
}
