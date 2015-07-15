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
package org.phenotips.studies.family.internal2;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.internal.PedigreeUtils;
import org.phenotips.studies.family.internal.export.XWikiFamilyExport;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringProperty;

import net.sf.json.JSON;

/**
 * XWiki implementation of Family.
 *
 * @version $Id$
 */
public class XWikiFamily implements Family
{
    private static final String WARNING = "warning";

    private static final String FAMILY_MEMBERS_FIELD = "members";

    private static PatientRepository patientRepository;

    private static XWikiFamilyPermissions familyPermissions;

    private static XWikiFamilyExport familyExport;

    @Inject
    private Logger logger;

    private XWikiDocument familyDocument;

    static {
        try {
            XWikiFamily.patientRepository =
                ComponentManagerRegistry.getContextComponentManager().getInstance(PatientRepository.class);
            XWikiFamily.familyPermissions =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiFamilyPermissions.class);
            XWikiFamily.familyExport =
                ComponentManagerRegistry.getContextComponentManager().getInstance(XWikiFamilyExport.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param familyDocument not-null document associated with the family
     */
    public XWikiFamily(XWikiDocument familyDocument)
    {
        this.familyDocument = familyDocument;
    }

    @Override
    public String getId()
    {
        return this.familyDocument.getDocumentReference().getName();
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.familyDocument.getDocumentReference();
    }

    @Override
    public List<String> getMembers()
    {
        BaseObject familyObject = this.familyDocument.getXObject(CLASS_REFERENCE);
        if (familyObject == null) {
            return new LinkedList<String>();
        }

        ListProperty xwikiRelativesList;
        try {
            xwikiRelativesList = (ListProperty) familyObject.get(FAMILY_MEMBERS_FIELD);
        } catch (XWikiException e) {
            this.logger.error("error reading family members: {}", e);
            return null;
        }
        if (xwikiRelativesList == null) {
            return new LinkedList<String>();
        }
        return xwikiRelativesList.getList();
    }

    @Override
    public synchronized boolean addMember(Patient patient)
    {
        XWikiContext context = getXContext();
        XWiki wiki = context.getWiki();
        DocumentReference patientReference = patient.getDocument();
        XWikiDocument patientDocument;
        try {
            patientDocument = wiki.getDocument(patientReference, context);
        } catch (XWikiException e) {
            this.logger.error("Could not add patient [{}] to family. Error getting patient document: {}",
                patient.getId(), e.getMessage());
            return false;
        }
        String patientAsString = patientReference.toString();

        // Add member to Xwiki family
        List<String> members = getMembers();
        if (!members.contains(patientAsString)) {
            members.add(patientAsString);
        } else {
            this.logger.info("Patient [{}] already a member of family [{}]. Not adding", patientAsString, getId());
            return false;
        }
        BaseObject familyObject = this.familyDocument.getXObject(Family.CLASS_REFERENCE);
        familyObject.set(FAMILY_MEMBERS_FIELD, members, context);

        XWikiFamily.familyPermissions.setFamilyPermissionsFromPatient(this.familyDocument, patientDocument);

        try {
            XWikiFamilyRepository.setFamilyReference(patientDocument, this.familyDocument, context);
        } catch (XWikiException e) {
            this.logger.error("Could not add patient [{}] to family. Error setting family reference: {}",
                patient.getId(), e.getMessage());
            return false;
        }

        PedigreeUtils.copyPedigree(patientDocument, this.familyDocument, context);

        try {
            wiki.saveDocument(this.familyDocument, context);
            wiki.saveDocument(patientDocument, context);
        } catch (XWikiException e) {
            this.logger.error("Could not save family/patient after adding: {}", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    // TODO
    public synchronized boolean removeMember(Patient patient)
    {
        return false;
    }

    @Override
    public boolean isMember(Patient patient)
    {
        List<String> members = getMembers();
        if (members == null) {
            return false;
        }
        String patientId = patient.getDocument().toString();
        return members.contains(patientId);
    }

    @Override
    public JSON getInformationAsJSON()
    {
        return XWikiFamily.familyExport.getInformationAsJSON(this);
    }

    @Override
    public Map<String, Map<String, String>> getMedicalReports()
    {
        Map<String, Map<String, String>> allFamilyLinks = new HashMap<>();

        for (String member : getMembers()) {
            Patient patient = XWikiFamily.patientRepository.getPatientById(member);
            allFamilyLinks.put(patient.getId(), XWikiFamily.familyExport.getMedicalReports(patient));
        }
        return allFamilyLinks;
    }

    @Override
    public String getExternalId()
    {
        BaseObject familyObject = this.familyDocument.getXObject(Family.CLASS_REFERENCE);
        StringProperty externalId = null;
        String externalIdString = null;
        try {
            externalId = (StringProperty) familyObject.get("external_id");
            if (externalId != null) {
                externalIdString = externalId.getValue();
            }
        } catch (XWikiException e) {
            this.logger.error("Error reading external id of family [{}]: [{}]", getId(), e.getMessage());
        }
        return externalIdString;
    }

    @Override
    public String getURL(String actions)
    {
        return this.familyDocument.getURL(actions, getXContext());
    }

    // ///////////////////////////////////////

    private XWikiContext getXContext()
    {
        Execution execution = null;
        try {
            execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
        } catch (ComponentLookupException ex) {
            // Should not happen
            return null;
        }
        XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        return context;
    }

    /*
     * Some pedigrees may contain sensitive information, which should be displayed on every edit of the pedigree. The
     * function returns a warning to display, or empty string
     */
    @Override
    public String getWarningMessage()
    {
        BaseObject familyObject = this.familyDocument.getXObject(Family.CLASS_REFERENCE);
        if (familyObject.getIntValue(WARNING) == 0) {
            return "";
        } else {
            return familyObject.getStringValue("warning_message");
        }
    }
}
