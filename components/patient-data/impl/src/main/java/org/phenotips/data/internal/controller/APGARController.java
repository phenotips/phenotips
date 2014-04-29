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

import java.util.LinkedList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles the APGAR scores in the patient record.
 */
@Component(roles = { PatientDataController.class })
@Named("apgar")
@Singleton
public class APGARController extends AbstractSimpleController
{
    @Override
    public String getName()
    {
        return "apgar";
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "apgar";
    }

    @Override protected List<String> getProperties()
    {
        List<String> list = new LinkedList<String>();
        list.add("apgar1");
        list.add("apgar5");
        return list;
    }
}
