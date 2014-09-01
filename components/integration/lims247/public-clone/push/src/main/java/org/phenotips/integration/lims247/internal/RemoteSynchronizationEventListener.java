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

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Pushes updated (or deleted) patient records to remote PhenoTips instances.
 *
 * @version $Id$
 */
@Component
@Named("pushDataToPublicClone")
@Singleton
public class RemoteSynchronizationEventListener implements EventListener
{
    /** The content type of the data sent in a request. */
    private static final ContentType REQUEST_CONTENT_TYPE = ContentType.create(
        ContentType.APPLICATION_XML.getMimeType(), Consts.UTF_8);

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** HTTP client used for communicating with the remote server. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    @Override
    public String getName()
    {
        return "pushDataToPublicClone";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentDeletedEvent) {
            handleDelete((XWikiDocument) source);
        } else {
            handleUpdate((XWikiDocument) source);
        }
    }

    private void handleUpdate(XWikiDocument doc)
    {
        try {
            if (!isPatient(doc)) {
                return;
            }
            this.logger.debug("Pushing updated document [{}]", doc.getDocumentReference());
            XWikiContext context = getXContext();
            String payload = doc.toXML(true, false, true, false, context);
            List<BaseObject> servers = getRegisteredServers(context);
            if (servers != null && !servers.isEmpty()) {
                for (BaseObject serverConfiguration : servers) {
                    submitData(payload, serverConfiguration);
                }
            }
        } catch (XWikiException ex) {
            this.logger.warn("Failed to serialize changed document: {}", ex.getMessage());
        }
    }

    private void handleDelete(XWikiDocument doc)
    {
        if (!isPatient(doc.getOriginalDocument())) {
            return;
        }
        this.logger.debug("Pushing deleted document [{}]", doc.getDocumentReference());
        XWikiContext context = getXContext();
        List<BaseObject> servers = getRegisteredServers(context);
        if (servers != null && !servers.isEmpty()) {
            for (BaseObject serverConfiguration : servers) {
                deleteData(doc.getDocumentReference().toString(), serverConfiguration);
            }
        }
    }

    /**
     * Check if the modified document is a patient record.
     *
     * @param doc the modified document
     * @return {@code true} if the document contains a PatientClass object and a non-empty external identifier,
     *         {@code false} otherwise
     */
    private boolean isPatient(XWikiDocument doc)
    {
        BaseObject o = doc.getXObject(Patient.CLASS_REFERENCE);
        return (o != null && !StringUtils.equals("PatientTemplate", doc.getDocumentReference().getName()));
    }

    /**
     * Get all the trusted remote instances where data should be sent that are configured in the current instance.
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
            return prefsDoc
                .getXObjects(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "RemoteClone"));
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Send the changed document to a remote PhenoTips instance.
     *
     * @param doc the serialized document to send
     * @param serverConfiguration the XObject holding the remote server configuration
     */
    private void submitData(String doc, BaseObject serverConfiguration)
    {
        // FIXME This should be asynchronous; reimplement!
        HttpPost method = null;
        try {
            String submitURL = getSubmitURL(serverConfiguration);
            if (StringUtils.isNotBlank(submitURL)) {
                this.logger.debug("Pushing updated document to [{}]", submitURL);
                method = new HttpPost(submitURL);
                method.setEntity(new StringEntity(doc, REQUEST_CONTENT_TYPE));
                this.client.execute(method).close();
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to notify remote server of patient update: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * Notify a remote PhenoTips instance of a deleted document.
     *
     * @param doc the name of the deleted document
     * @param serverConfiguration the XObject holding the remote server configuration
     */
    private void deleteData(String doc, BaseObject serverConfiguration)
    {
        // FIXME This should be asynchronous; reimplement!
        HttpPost method = null;
        try {
            String deleteURL = getDeleteURL(serverConfiguration);
            if (StringUtils.isNotBlank(deleteURL)) {
                this.logger.debug("Pushing deleted document to [{}]", deleteURL);
                method = new HttpPost(deleteURL);
                NameValuePair data = new BasicNameValuePair("document", doc);
                method.setEntity(new UrlEncodedFormEntity(Collections.singletonList(data), Consts.UTF_8));
                this.client.execute(method).close();
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to notify remote server of patient removal: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * Return the URL of the specified remote PhenoTips instance, where the updated document should be sent.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the configured URL, in the format {@code http://remote.host.name/bin/receive/data/}, or {@code null} if
     *         the configuration isn't valid
     */
    private String getSubmitURL(BaseObject serverConfiguration)
    {
        String result = getBaseURL(serverConfiguration);
        if (StringUtils.isNotBlank(result)) {
            return result + "?action=save&token=" + getToken(serverConfiguration);
        }
        return null;
    }

    /**
     * Return the URL of the specified remote PhenoTips instance, where the notifications about deleted documents should
     * be sent.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the configured URL, in the format {@code http://remote.host.name/bin/deleted/data/}, or {@code null} if
     *         the configuration isn't valid
     */
    private String getDeleteURL(BaseObject serverConfiguration)
    {
        String result = getBaseURL(serverConfiguration);
        if (StringUtils.isNotBlank(result)) {
            return result + "?action=delete&token=" + getToken(serverConfiguration);
        }
        return null;
    }

    /**
     * Return the base URL of the specified remote PhenoTips instance.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the configured URL, in the format {@code http://remote.host.name/bin/}, or {@code null} if the
     *         configuration isn't valid
     */
    private String getBaseURL(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            String result = serverConfiguration.getStringValue("url");
            if (StringUtils.isBlank(result)) {
                return null;
            }

            if (!result.startsWith("http")) {
                result = "http://" + result;
            }
            return StringUtils.stripEnd(result, "/") + "/bin/data/sync";
        }
        return null;
    }

    private String getToken(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            return serverConfiguration.getStringValue("token");
        }
        return null;
    }

    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     *
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
