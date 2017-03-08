/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.vocabulary.annotation.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyInputTerm;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;

/**
 * Extends {@link AbstractPhenotypesFromCSVAnnotationExtension} to annotate an Orphanet {@link VocabularyInputTerm} with
 * its associated phenotypes. Two annotations are added: one contains actual symptoms directly from the annotation
 * source (labeled {@link #getDirectPhenotypesLabel()}), the other one contains symptoms from the annotation source, as
 * well as the ancestor phenotypes (labeled {@link #getAllAncestorPhenotypesLabel()}).
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("orphanet-annotation-symptom")
@Singleton
public class OrphanetSymptomAnnotationExtension extends AbstractPhenotypesFromCSVAnnotationExtension
{
    /** The source URL for phenotype annotations. */
    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations/"
        + "lastStableBuild/artifact/misc/phenotype_annotation.tab";

    private static final String ORPHANET_LABEL = "orphanet";

    private static final String ACTUAL_SYMPTOM = "actual_symptom";

    private static final String SYMPTOM = "symptom";

    @Override
    protected String getTargetVocabularyId()
    {
        return ORPHANET_LABEL;
    }

    @Override
    protected String getAnnotationSource()
    {
        return ANNOTATION_SOURCE;
    }

    @Override
    public void extendQuery(final SolrQuery query, final Vocabulary vocabulary)
    {
        // Nothing to do.
    }

    @Override
    protected String getDirectPhenotypesLabel()
    {
        return ACTUAL_SYMPTOM;
    }

    @Override
    protected String getAllAncestorPhenotypesLabel()
    {
        return SYMPTOM;
    }

    @Override
    protected int getDBNameColNumber()
    {
        return 0;
    }

    @Override
    protected int getDiseaseColNumber()
    {
        return 1;
    }

    @Override
    protected int getPhenotypeColNumber()
    {
        return 4;
    }
}
