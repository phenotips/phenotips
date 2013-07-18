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
package org.phenotips.data.similarity.internal.mocks;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import java.util.Set;

/**
 * Simple mock for an ontology term, responding with pre-specified values.
 * 
 * @version $Id$
 */
public class MockOntologyTerm implements OntologyTerm
{
    private final String id;

    private final Set<OntologyTerm> parents;

    private final Set<OntologyTerm> ancestors;

    public MockOntologyTerm(String id, Set<OntologyTerm> parents, Set<OntologyTerm> ancestors)
    {
        this.id = id;
        this.parents = parents;
        this.ancestors = ancestors;
    }

    @Override
    public String getId()
    {

        return this.id;
    }

    @Override
    public String getName()
    {
        // Not used
        return null;
    }

    @Override
    public String getDescription()
    {
        // Not used
        return null;
    }

    @Override
    public Set<OntologyTerm> getParents()
    {
        return this.parents;
    }

    @Override
    public Set<OntologyTerm> getAncestors()
    {
        return this.ancestors;
    }

    @Override
    public Object get(String name)
    {
        // Not used
        return null;
    }

    @Override
    public OntologyService getOntology()
    {
        // Not used
        return null;
    }
}
