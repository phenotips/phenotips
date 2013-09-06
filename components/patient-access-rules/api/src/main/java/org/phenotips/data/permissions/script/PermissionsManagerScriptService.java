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
package org.phenotips.data.permissions.script;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Named("permissions")
@Singleton
public class PermissionsManagerScriptService implements ScriptService
{
    @Inject
    private PermissionsManager manager;

    public Collection<Visibility> listVisibilityOptions()
    {
        return this.manager.listVisibilityOptions();
    }

    public Visibility resolveVisibility(String name)
    {
        return this.manager.resolveVisibility(name);
    }

    public Collection<AccessLevel> listAccessLevels()
    {
        return this.manager.listAccessLevels();
    }

    public AccessLevel resolveAccessLevel(String name)
    {
        return this.manager.resolveAccessLevel(name);
    }

    public PatientAccess getPatientAccess(Patient targetPatient)
    {
        return new SecurePatientAccess(this.manager.getPatientAccess(targetPatient), this.manager);
    }
}
