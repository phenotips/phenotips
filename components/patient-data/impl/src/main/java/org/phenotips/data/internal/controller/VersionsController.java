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
import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Exposes the version of the ontologies used for creating the patient record, as well as the current PhenoTips version.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("versions")
@Singleton
public class VersionsController extends AbstractSimpleController
    implements PatientDataController<String>
{
    /** The XClass used for storing version data of different ontologies. */
    private static final EntityReference ONTOLOGY_VERSION_CLASS_REFERENCE =
        new EntityReference("OntologyVersionClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String CONTROLLER_NAME = "versions";

    @Inject
    private Logger logger;

    @Override
    public PatientData<String> load(Patient patient)
    {
        Map<String, String> versions = new LinkedHashMap<>();

        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            addOntologyVersions(doc, versions);
            addPhenoTipsVersion(versions);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return new DictionaryPatientData<>(getName(), versions);
    }

    /**
     * Reads all the {@code PhenoTips.OntologyVersionClass} objects from the patient document.
     *
     * @param doc the document storing the patient data
     */
    private void addOntologyVersions(XWikiDocument doc, Map<String, String> versions)
    {
        List<BaseObject> ontologyVersionObjects = doc.getXObjects(ONTOLOGY_VERSION_CLASS_REFERENCE);
        if (ontologyVersionObjects == null) {
            return;
        }

        for (BaseObject versionObject : ontologyVersionObjects) {
            if (versionObject == null) {
                continue;
            }
            String versionType = versionObject.getStringValue("name");
            String versionString = versionObject.getStringValue("version");
            if (StringUtils.isNotEmpty(versionType) && StringUtils.isNotEmpty(versionString)) {
                versions.put(versionType + "_version", versionString);
            }
        }
    }

    /**
     * Gets the PhenoTips version from the XWiki Distribution Manager and adds it to the patient data being loaded.
     *
     * @param versions the list of version data being constructed
     */
    private void addPhenoTipsVersion(Map<String, String> versions)
    {
        try {
            CoreExtensionRepository coreExtensionRepository =
                ComponentManagerRegistry.getContextComponentManager().getInstance(CoreExtensionRepository.class);
            Version v = coreExtensionRepository.getCoreExtension("org.phenotips:patient-data-api").getId().getVersion();
            versions.put("phenotips_version", v.toString());
        } catch (ComponentLookupException ex) {
            // Shouldn't happen
            this.logger.error("Could not find DistributionManager component");
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        // unlike all other controllers, there is no field name controlling version information
        // so for this particular controller a special getEnablingFieldName() property is used
        // which, if included in the list of fields, enables version data to be dumped into JSON
        // (note that we may sometimes want to omit this data when presenting results for the end users)
        if (selectedFieldNames == null || selectedFieldNames.contains(getEnablingFieldName())) {
            super.writeJSON(patient, json, null);
        }
    }

    @Override
    protected List<String> getProperties()
    {
        // Not used, since there's a custom load method
        return Collections.emptyList();
    }

    @Override
    public String getName()
    {
        return CONTROLLER_NAME;
    }

    @Override
    protected String getJsonPropertyName()
    {
        return "meta";
    }

    /**
     * Unlike all other controllers, there is no field name controlling presence of version information in JSON output.
     * This method returns a name which can be used instead.
     *
     * @return a name which can be included in the list of enabled fields to enable version info in JSON output
     */
    public static String getEnablingFieldName()
    {
        return CONTROLLER_NAME;
    }
}
