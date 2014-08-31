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
package org.phenotips.data.internal.controller;

import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Has only one field corresponding to whether the patient is affected or not.
 *
 * @version $Id$
 * @since 1.0RC2
 */
@Component(roles = { PatientDataController.class })
@Named("isClinicallyNormal")
@Singleton
public class IsNormalController extends AbstractComplexController<Integer>
{
    @Override
    public String getName()
    {
        return "isClinicallyNormal";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return getName();
    }

    @Override
    protected List<String> getProperties()
    {
        return Arrays.asList("unaffected");
    }

    @Override
    protected List<String> getBooleanFields()
    {
        return getProperties();
    }

    @Override
    protected List<String> getCodeFields()
    {
        return Collections.emptyList();
    }
}
