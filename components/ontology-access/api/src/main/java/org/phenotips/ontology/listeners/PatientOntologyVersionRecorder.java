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
package org.phenotips.ontology.listeners;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangingEvent;
import org.phenotips.ontology.OntologyService;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Store versions (in the form name:String, version:String) in the patient record.
 *
 * @version $Id$
 */
@Component
@Named("ontology-version-recorder")
@Singleton
public class PatientOntologyVersionRecorder extends AbstractEventListener
{
    /** The name of the class where version info (name, version) is stored. */
    private static final EntityReference VERSION_RECORDER_REFERENCE = new EntityReference("OntologyVersionClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private static final String NAME_FIELD = "name";

    private static final String VERSION_FIELD = "version";

    /** Access to services that are needed to get the ontology version. */
    @Inject
    private Map<String, OntologyService> ontologies;

    @Inject
    private Execution execution;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientOntologyVersionRecorder()
    {
        super("ontology-version-recorder", new PatientChangingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        XWikiDocument doc = (XWikiDocument) source;

        BaseObject patientRecordObj = doc.getXObject(Patient.CLASS_REFERENCE);
        if (patientRecordObj == null) {
            return;
        }

        List<BaseObject> existingVersionObjects = doc.getXObjects(VERSION_RECORDER_REFERENCE);
        try {
            for (Map.Entry<String, String> versionType : this.getOntologiesVersions().entrySet()) {
                boolean skip = false;
                if (existingVersionObjects != null) {
                    for (BaseObject existingVersionObject : existingVersionObjects) {
                        String name = existingVersionObject.getStringValue(NAME_FIELD);
                        if (StringUtils.equalsIgnoreCase(name, versionType.getKey())) {
                            existingVersionObject.set(VERSION_FIELD, versionType.getValue(), context);
                            skip = true;
                            break;
                        }
                    }
                    if (skip) {
                        continue;
                    }
                }
                BaseObject versionObject = doc.newXObject(VERSION_RECORDER_REFERENCE, context);
                versionObject.set(NAME_FIELD, versionType.getKey(), context);
                versionObject.set(VERSION_FIELD, versionType.getValue(), context);
            }
        } catch (XWikiException ex) {
            // Storage Error. Shouldn't happen.
        }
    }

    /**
     * Modify this function to add more version types.
     *
     * @return Map of all the version types. Each entry becomes an object in the patient record.
     */
    public Map<String, String> getOntologiesVersions()
    {
        Map<String, String> result = new HashMap<>();

        for (Entry<String, OntologyService> ontology : this.ontologies.entrySet()) {
            String version = ontology.getValue().getVersion();
            if (StringUtils.isNotBlank(version)) {
                result.put(ontology.getKey(), version);
            }
        }
        return result;
    }
}
