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
package org.phenotips.ontology.script;

import org.phenotips.ontology.OntologyManager;
import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides access to the available ontologies and their terms to public scripts.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Component
@Named("ontologies")
@Singleton
public class OntologyScriptService implements ScriptService
{
    /** The ontology manager that actually does all the work. */
    @Inject
    private OntologyManager manager;

    /**
     * Retrieve a term from its owner ontology. For this to work properly, the term identifier must contain a known
     * ontology prefix.
     *
     * @param termId the term identifier, in the format {@code <ontology prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in the ontology, or no matching ontology is
     *         available
     */
    public OntologyTerm resolveTerm(String termId)
    {
        return this.manager.resolveTerm(termId);
    }

    /**
     * Retrieve an ontology given its identifier.
     *
     * @param ontologyId the ontology identifier, which is also used as a prefix in every term identifier from that
     *            ontology, for example {@code HP} or {@code MIM}
     * @return the requested ontology, or {@code null} if it doesn't exist or isn't available in the platform
     */
    public OntologyService getOntology(String ontologyId)
    {
        return this.manager.getOntology(ontologyId);
    }

    /**
     * Retrieve an ontology given its identifier. This is a shortcut for {@link #getOntology(String)} which allows
     * scripts to use the shorter {@code $services.ontologies.hpo} notation for accessing an ontology.
     *
     * @param ontologyId the ontology identifier, which is also used as a prefix in every term identifier from that
     *            ontology, for example {@code HP} or {@code MIM}
     * @return the requested ontology, or {@code null} if it doesn't exist or isn't available in the platform
     */
    public OntologyService get(String ontologyId)
    {
        return this.manager.getOntology(ontologyId);
    }
}
