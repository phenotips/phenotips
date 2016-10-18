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
package org.phenotips.recordLocking.internal;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of the {@link org.phenotips.recordLocking.PatientRecordLockManager}.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Singleton
public class DefaultPatientRecordLockManager implements org.phenotips.recordLocking.PatientRecordLockManager
{
    /** The XClass used for lock objects. */
    private EntityReference lockClassReference = new EntityReference("PatientLock", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** Allows checking of access rights on a patient. */
    @Inject
    private PermissionsManager pm;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("manage")
    private AccessLevel manageAccessLevel;

    @Override
    public boolean lockPatientRecord(Patient patient)
    {
        try {
            XWikiDocument patientDocument = this.getPatientDocument(patient);
            if (!this.isDocumentLocked(patientDocument) && this.hasLockingPermission(patient)) {
                XWikiContext context = this.contextProvider.get();
                XWiki xwiki = context.getWiki();
                patientDocument.createXObject(this.lockClassReference, context);
                xwiki.saveDocument(patientDocument, "Locked patient record", true,
                    context);
                return true;
            } else {
                return false;
            }
        } catch (XWikiException | NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean unlockPatientRecord(Patient patient)
    {
        try {
            XWikiDocument patientDocument = this.getPatientDocument(patient);
            if (this.isDocumentLocked(patientDocument) && this.hasLockingPermission(patient)) {
                XWikiContext context = this.contextProvider.get();
                XWiki xwiki = context.getWiki();
                patientDocument.removeXObjects(this.lockClassReference);
                xwiki.saveDocument(patientDocument, "Unlocked patient record", true,
                    context);
                return true;
            } else {
                return false;
            }
        } catch (XWikiException | NullPointerException e) {
            return false;
        }
    }

    @Override
    public boolean isLocked(Patient patient)
    {
        XWikiDocument document = this.getPatientDocument(patient);
        return isDocumentLocked(document);
    }

    private XWikiDocument getPatientDocument(Patient patient)
    {
        XWikiContext context = this.contextProvider.get();
        XWiki xwiki = context.getWiki();
        DocumentReference patientDocumentReference = patient.getDocumentReference();

        try {
            return xwiki.getDocument(patientDocumentReference, context);
        } catch (XWikiException e) {
            return null;
        }
    }

    private boolean hasLockingPermission(Patient patient)
    {
        PatientAccess patientAccess = this.pm.getPatientAccess(patient);
        return patientAccess.hasAccessLevel(this.manageAccessLevel);
    }

    private boolean isDocumentLocked(XWikiDocument document)
    {
        BaseObject lock = document.getXObject(this.lockClassReference);
        return lock != null;
    }

}
