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
package org.phenotips.integration.lims247.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Script service that accepts document modification requests from a trusted remote service.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Named("remoteData")
@Singleton
public class RemoteDataScriptService implements ScriptService
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * Check that the token received in the request is valid. The expected token is configured in the
     * {@code xwiki.properties} configuration file, under the {@code phenotips.remoteAuthentication.trustedToken} key.
     *
     * @return {@code true} if a token parameter is present in the request, and it's value matches the configuration
     */
    public boolean isTrusted()
    {
        String token = getXContext().getRequest().getParameter("token");
        String expected = this.configuration.getProperty("phenotips.remoteAuthentication.trustedToken");
        return StringUtils.equals(expected, token);
    }

    /**
     * Receives a serialized document from a trusted remote PhenoTips instance, and saves it in the local database.
     */
    public void save()
    {
        try {
            XWikiContext context = getXContext();
            if (isTrusted()) {
                try {
                    @SuppressWarnings("deprecation")
                    XWikiDocument doc = new XWikiDocument();
                    doc.fromXML(context.getRequest().getInputStream());
                    context.getWiki().saveDocument(doc, doc.getComment(), doc.isMinorEdit(), context);
                    this.logger.debug("Imported [{}] from remote", doc.getDocumentReference());
                } catch (IOException ex) {
                    // Shouldn't happen
                    this.logger.info("Error reading request: {}", ex.getMessage());
                }
            } else {
                this.logger.warn("Unauthorized data submission from [{}]!", context.getRequest().getRemoteAddr());
            }
        } catch (XWikiException ex) {
            this.logger.warn("Failed to save remotely submitted document: {}", ex.getMessage());
        }
    }

    /**
     * Receives the name of a document that has been deleted on a trusted remote PhenoTips instance, and deletes it from
     * the local database.
     */
    public void delete()
    {
        try {
            XWikiContext context = getXContext();
            if (isTrusted()) {
                @SuppressWarnings("deprecation")
                XWikiDocument doc =
                    context.getWiki().getDocument(context.getRequest().getParameter("document"), context);
                if (!doc.isNew()) {
                    // Skip the trash, to prevent a data leak; it can be restored only on the original instance
                    context.getWiki().deleteDocument(doc, false, context);
                    this.logger.debug("Deleted [{}] by remote", doc.getDocumentReference());
                }
            } else {
                this.logger.warn("Unauthorized delete request from [{}]!", context.getRequest().getRemoteAddr());
            }
        } catch (XWikiException ex) {
            this.logger.warn("Failed to delete remotely deleted document: {}", ex.getMessage());
        }
    }

    /**
     * Helper method for obtaining a valid XContext from the execution context.
     *
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
