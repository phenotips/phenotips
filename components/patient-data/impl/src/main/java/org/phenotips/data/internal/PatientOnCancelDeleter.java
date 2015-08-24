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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientEditingCancelingEvent;
import org.phenotips.data.events.PatientEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Deletes the patient record when a {@link PatientEditingCancelingEvent} is received from a new patient record.
 *
 * @since 1.2RC1
 * @version $Id$
 */
@Component
@Named("patient-oncancel-deleter")
@Singleton
public class PatientOnCancelDeleter extends AbstractEventListener
{
    private static final String AUTOSAVE_COMMENT = "(Autosaved)";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;


    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public PatientOnCancelDeleter()
    {
        super("patient-oncancel-deleter", new PatientEditingCancelingEvent());
    }

    @Override
    public void onEvent(final Event event, final Object source, final Object data)
    {
        Patient patient = ((PatientEvent) event).getPatient();
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        try {
            XWikiDocument doc = wiki.getDocument(patient.getDocument(), context);
            if (isNew(doc, context)) {
                wiki.deleteDocument(doc, true, context);

                EntityReference defaultReference = new EntityReference(wiki.getDefaultPage(context),
                    EntityType.DOCUMENT, new EntityReference(wiki.getDefaultSpace(context), EntityType.SPACE));
                defaultReference = currentResolver.resolve(defaultReference, EntityType.DOCUMENT);
                String url = wiki.getURL(new DocumentReference(defaultReference), "view", context);
                context.getResponse().sendRedirect(url);
            }
        } catch (XWikiException ex) {
            logger.error("Could not delete patient. {}", ex.getMessage());
        } catch (IOException ex) {
            logger.error("Could not redirect user appropriately. {}", ex.getMessage());
        }
    }

    private boolean isNew(XWikiDocument patient, XWikiContext context)
    {
        // to be safe and not delete in case of error.
        boolean hasVersion = false;
        int versionCount = 0;
        try {
            List<String> versions = patient.getRevisions(new RevisionCriteria(), context);
            for (String version : versions) {
                XWikiRCSNodeInfo info = patient.getRevisionInfo(version, context);
                if (!StringUtils.equalsIgnoreCase(info.getComment().trim(), AUTOSAVE_COMMENT) && !info.isMinorEdit()) {
                    hasVersion = true;
                    versionCount += 1;
                }
            }
            return hasVersion && versionCount <= 1;
        } catch (XWikiException ex) {
            return false;
        }
    }
}
