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
package org.phenotips.integration.lims247.internal;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientDeletedEvent;
import org.phenotips.data.events.PatientEvent;
import org.phenotips.integration.lims247.Lims247AuthServiceImpl;
import org.phenotips.integration.lims247.LimsAuthentication;
import org.phenotips.integration.lims247.LimsServer;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;

/**
 * Announces LIMS of updates to patient phenotypes.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("lims247sync")
@Singleton
public class LimsSynchronizationEventListener extends AbstractEventListener
{
    /** The XClass used for storing patient data. */
    private static final EntityReference PATIENT_CLASS = new EntityReference("PatientClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The JSON key for the event type. */
    private static final String EVENT_KEY = "event";

    /** The JSON key for the patient identifier. */
    private static final String IDENTIFIER_KEY = "eid";

    /** The name of the XProperty holding the patient external identifier. */
    private static final String EXTERNAL_ID_PROPERTY_NAME = "external_id";

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** Authentication token manager. */
    @Inject
    private CSRFToken token;

    /** Does the actual communication with the LIMS servers. */
    @Inject
    private LimsServer server;

    @Inject
    private EntityReferenceResolver<String> referenceResolver;

    /** Default constructor, sets up the listener name and the list of events to subscribe to. */
    public LimsSynchronizationEventListener()
    {
        super("lims247sync", new PatientChangedEvent(), new PatientDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        Patient patient = ((PatientEvent) event).getPatient();
        if (StringUtils.isBlank(patient.getExternalId())) {
            return;
        }
        String eventType = "update";
        if (event instanceof PatientDeletedEvent) {
            eventType = "delete";
        } else if (((XWikiDocument) source).getOriginalDocument().isNew()) {
            eventType = "create";
        }
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        JSONObject payload = getPayload(eventType, patient.getExternalId(), context);
        List<BaseObject> servers = getRegisteredServers(context);
        if (servers != null && !servers.isEmpty()) {
            for (BaseObject serverConfiguration : servers) {
                notifyServer(payload, serverConfiguration, context);
            }
        }
    }

    /**
     * Prepare a JSON payload to send to the registered LIMS servers to notify of the change. The JSON looks like:
     *
     * <pre>
     * {
     *   "eid": "ExternalId123",
     *   "event": "update", // or "create" or "delete"
     *   "pn": "wiki name",
     *   "username": "uname",
     *   "auth_token": "strtoken"
     * }
     * </pre>
     *
     * The username and authentication token is taken either from the cached LIMS authentication, or from the XWiki user
     * logged in.
     *
     * @param event the original event that notified of the change
     * @param doc the modified document, a patient sheet document
     * @param context the current request context
     * @return a JSON object
     */
    private JSONObject getPayload(String eventType, String eid, XWikiContext context)
    {
        JSONObject result = new JSONObject();
        result.put(IDENTIFIER_KEY, eid);
        result.put(EVENT_KEY, eventType);

        LimsAuthentication auth =
            (LimsAuthentication) context.getRequest().getSession().getAttribute(Lims247AuthServiceImpl.SESSION_KEY);
        if (auth != null) {
            // FIXME Reuse this authentication only if the authentication server is the same as the target server
            result.put(LimsServer.INSTANCE_IDENTIFIER_KEY, context.getDatabase());
            result.put(LimsServer.USERNAME_KEY,
                this.referenceResolver.resolve(auth.getUser().getUser(), EntityType.DOCUMENT).getName());
            result.put(LimsServer.TOKEN_KEY, auth.getToken());
        } else if (context.getUserReference() != null) {
            result.put(LimsServer.INSTANCE_IDENTIFIER_KEY, context.getUserReference().getWikiReference().getName());
            result.put(LimsServer.USERNAME_KEY, context.getUserReference().getName());
            result.put(LimsServer.TOKEN_KEY, this.token.getToken());
        } else {
            result.put(LimsServer.INSTANCE_IDENTIFIER_KEY, context.getDatabase());
            result.put(LimsServer.USERNAME_KEY, "");
            result.put(LimsServer.TOKEN_KEY, "");
        }
        return result;
    }

    /**
     * Get all the LIMS servers configured in the current wiki.
     *
     * @param context the current request object
     * @return a list of {@link BaseObject XObjects} with LIMS server configurations, may be {@code null}
     */
    private List<BaseObject> getRegisteredServers(XWikiContext context)
    {
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc.getXObjects(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE,
                "LimsAuthServer"));
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Notify a remote LIMS instance that a patient's phenotype has changed.
     *
     * @param payload the JSON payload to send
     * @param serverConfiguration the XObject holding the LIMS server configuration
     * @param context the current request context
     */
    private void notifyServer(JSONObject payload, BaseObject serverConfiguration, XWikiContext context)
    {
        String pn = serverConfiguration.getStringValue(LimsServer.INSTANCE_IDENTIFIER_KEY);
        if (StringUtils.isNotBlank(pn)) {
            this.server.notify(payload, pn);
        }
    }
}
