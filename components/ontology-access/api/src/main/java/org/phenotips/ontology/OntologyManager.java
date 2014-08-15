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
package org.phenotips.ontology;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provides access to the available ontologies and their terms.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Role
public interface OntologyManager
{
    /**
     * Retrieve a term from its owner ontology. For this to work properly, the term identifier must contain a known
     * ontology prefix.
     *
     * @param termId the term identifier, in the format {@code <ontology prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in the ontology, or no matching ontology is
     *         available
     */
    OntologyTerm resolveTerm(String termId);

    /**
     * Retrieve an ontology given its identifier.
     *
     * @param ontologyId the ontology identifier, which is also used as a prefix in every term identifier from that
     *            ontology, for example {@code HP} or {@code MIM}
     * @return the requested ontology, or {@code null} if it doesn't exist or isn't available in the platform
     */
    OntologyService getOntology(String ontologyId);
}
