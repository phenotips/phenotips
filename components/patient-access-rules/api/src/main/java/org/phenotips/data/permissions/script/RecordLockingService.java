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
package org.phenotips.data.permissions.script;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * A service to add or remove a lock object to a record. The lock object will remove edit rights for all users on the
 * document.
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("recordLocking")
@Singleton
public class RecordLockingService implements ScriptService
{
    /** The XClass used for lock objects. */
    private EntityReference lockClassReference = new EntityReference("PatientLock", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** Allows retrieval of Patient from ID*/
    @Inject
    PatientRepository pr;

    /** Allows checking of access rights on a patient*/
    @Inject
    PermissionsManager pm;

    /**
     * Sets or removes a lock from a record. Locks remove edit rights from all users.
     * @param patientId The record to be locked
     * @param lock "true" to create a lock, "false" to remove
     */
    public void setRecordLock (String patientId, Boolean lock)
    {
        if (patientId == null || lock == null) {
            return;
        }

        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWiki xwiki = context.getWiki();

        Patient patient = this.pr.getPatientById(patientId);
        if (patient == null) {
            return;
        }

        try {
            DocumentReference patientDocumentReference = patient.getDocument();
            XWikiDocument patientDocument = xwiki.getDocument(patientDocumentReference, context);
            PatientAccess patientAccess = this.pm.getPatientAccess(patient);
            Boolean hasLockingPermission = patientAccess.hasAccessLevel(new ManageAccessLevel());

            //Edit the lock if the user has locking permission
            if (hasLockingPermission) {
                BaseObject previousLockObject = patientDocument.getXObject(lockClassReference);

                if (lock && previousLockObject == null) {
                    patientDocument.createXObject(lockClassReference, context);
                } else if (!lock && previousLockObject != null) {
                    patientDocument.removeXObjects(lockClassReference);
                }

                xwiki.saveDocument(patientDocument, context);
            }
        } catch (XWikiException e) {
            return;
        }
    }
}
