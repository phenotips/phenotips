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
package org.phenotips.hpoa.ontology;

import org.phenotips.hpoa.utils.graph.IDAGNode;
import org.phenotips.solr.AbstractSolrScriptService;

import org.xwiki.component.annotation.Role;

import java.io.File;
import java.util.Set;

@Role
public interface Ontology
{
    public abstract int load(AbstractSolrScriptService source);

    public abstract int load(File source);

    public abstract String getRealId(String id);

    public abstract OntologyTerm getTerm(String id);

    public abstract String getName(String id);

    public abstract String getRootId();

    public abstract IDAGNode getRoot();

    public abstract Set<String> getAncestors(String termId);
}
