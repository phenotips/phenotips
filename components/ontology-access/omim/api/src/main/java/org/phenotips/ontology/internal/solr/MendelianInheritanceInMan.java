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
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides access to the Online Mendelian Inheritance in Man (OMIM) ontology. The ontology prefix is {@code MIM}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("omim")
@Singleton
public class MendelianInheritanceInMan extends AbstractSolrOntologyService
{
    /** The standard name of this ontology, used as a term prefix. */
    public static final String STANDARD_NAME = "MIM";

    @Override
    protected String getName()
    {
        return "omim";
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = super.getTerm(id);
        if (result == null) {
            String optionalPrefix = STANDARD_NAME + ":";
            if (StringUtils.startsWith(id, optionalPrefix)) {
                result = getTerm(StringUtils.substringAfter(id, optionalPrefix));
            }
        }
        return result;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add(STANDARD_NAME);
        result.add("OMIM");
        return result;
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        // FIX ME. For now returns just an empty string.
        return "";
    }
}
