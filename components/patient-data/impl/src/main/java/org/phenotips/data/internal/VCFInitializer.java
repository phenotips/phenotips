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

import org.phenotips.Constants;
import org.phenotips.data.events.PatientCreatingEvent;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Adds a {@code PhenoTips.VCF} XObject to newly created patient records.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("patient-vcf-initializer")
@Singleton
public class VCFInitializer extends AbstractEventListener
{
    /** The XClass used to store the owner in the patient record. */
    private static final EntityReference CLASS_REFERENCE = new EntityReference("VCF", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public VCFInitializer()
    {
        super("patient-vcf-initializer", new PatientCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        doc.getXObject(CLASS_REFERENCE, true, context);
        this.logger.debug("Added initial VCF object for patient [{}]", doc.getDocumentReference());
    }
}
