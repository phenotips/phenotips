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
package org.phenotips.data.export.script;

import org.phenotips.Constants;
import org.phenotips.data.export.internal.SpreadsheetExporter;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import groovy.lang.Singleton;

/**
 * FIXME
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Unstable
@Component
@Named("spreadsheetexport")
@Singleton
public class SpreadsheetExportService implements ScriptService
{
    private final static String patientClassName = "PatientClass";

    private final static EntityReference patientClass =
        new EntityReference(patientClassName, EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    private Execution execution;

    @Inject
    DocumentReferenceResolver<EntityReference> referenceResolver;

    @Inject
    DocumentReferenceResolver<String> stringReferenceResolver;

    @Inject
    Logger logger;

    XWikiContext context;

    XWiki wiki;

    /**
     * This export assumes that the fields passed in are not 'pretty', and converts them to be human readable.
     *
     * @param enabledFields string array of 'non-pretty' names
     */
    public void export(String[] enabledFields, List<String> patients) throws XWikiException
    {
        context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        wiki = context.getWiki();

        SpreadsheetExporter exporter = new SpreadsheetExporter();
        try {
            exporter.export(enabledFields, patientListToXWikiDocument(patients));
        } catch (Exception ex) {
            logger.error("Error caught while generating a export in spreadsheet format", ex);
        }
    }

    protected List<XWikiDocument> patientListToXWikiDocument(List<String> patients) throws XWikiException
    {
        List<XWikiDocument> docList = new LinkedList<XWikiDocument>();
        for (String id : patients) {
            docList.add(wiki.getDocument(stringReferenceResolver.resolve(id), context));
        }
        return docList;
    }

    protected String[] substitutePretty(String[] fields) throws XWikiException
    {
        DocumentReference classDoc = referenceResolver.resolve(patientClass);
        BaseClass patientClassObj = wiki.getXClass(classDoc, context);
        String[] prettyFields = new String[fields.length];
        Integer counter = 0;
        for (String field : fields) {
            if (StringUtils.isBlank(field)) {
                continue;
            }
            try {
                PropertyClass baseField = (PropertyClass) patientClassObj.get(field);
                prettyFields[counter] = baseField.getPrettyName();
            } catch (Exception ex) {
                prettyFields[counter] = field;
            }
            counter += 1;
        }
        return prettyFields;
    }
}

