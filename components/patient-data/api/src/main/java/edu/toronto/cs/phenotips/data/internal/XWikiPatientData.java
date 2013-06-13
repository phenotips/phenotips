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
package edu.toronto.cs.phenotips.data.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.doc.XWikiDocument;

import edu.toronto.cs.phenotips.data.Patient;
import edu.toronto.cs.phenotips.data.PatientData;

/**
 * @version $Id$
 */
@Component
@Singleton
public class XWikiPatientData implements PatientData
{
    /** Runs queries for finding a patient given its external identifier. */
    @Inject
    private QueryManager qm;

    /** Provides access to the XWiki data. */
    @Inject
    private DocumentAccessBridge bridge;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> resolver;

    private EntityReference patientClass = new EntityReference("PatientClass", EntityType.DOCUMENT,
        new EntityReference("PhenoTips", EntityType.SPACE));

    @Override
    public Patient getPatientById(String id)
    {
        DocumentReference reference = this.resolver.resolve(id, new EntityReference("data", EntityType.SPACE));
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument(reference);
            if (doc != null && doc.getXObject(this.patientClass) != null) {
                return new XWikiPatient((XWikiDocument) this.bridge.getDocument(reference));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Patient getPatientByExternalId(String externalId)
    {
        try {
            Query q = this.qm.createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);
            q.bindValue("eid", externalId);
            List<String> results = q.<String> execute();
            if (results.size() == 1) {
                DocumentReference reference =
                    this.resolver.resolve(results.get(0), new EntityReference("data", EntityType.SPACE));
                return new XWikiPatient((XWikiDocument) this.bridge.getDocument(reference));
            }
        } catch (QueryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Patient createNewPatient()
    {
        // FIXME implementation missing
        throw new UnsupportedOperationException();
    }
}
