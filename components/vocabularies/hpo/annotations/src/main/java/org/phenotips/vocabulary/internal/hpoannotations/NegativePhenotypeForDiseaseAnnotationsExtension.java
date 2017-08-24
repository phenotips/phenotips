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
package org.phenotips.vocabulary.internal.hpoannotations;

import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularySourceRelocationService;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Extends {@link AbstractPhenotypeForDiseaseAnnotationsExtension} to annotate a disease {@link VocabularyInputTerm}
 * with its associated negative phenotypes. Two annotations are added: one contains actual negative symptoms directly
 * from the annotation source (labeled {@link #getDirectPhenotypesLabel()}), the other one contains negative symptoms
 * from the annotation source, as well as the ancestor phenotypes (labeled {@link #getAllAncestorPhenotypesLabel()}).
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("disease-hpo-negative-annotations")
@Singleton
public class NegativePhenotypeForDiseaseAnnotationsExtension extends AbstractPhenotypeForDiseaseAnnotationsExtension
{
    /** The source URL for phenotype annotations. */
    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations/"
        + "lastStableBuild/artifact/misc/negative_phenotype_annotation.tab";

    private static final String ACTUAL_NOT_SYMPTOM = "actual_not_symptom";

    private static final String NOT_SYMPTOM = "not_symptom";

    @Inject
    protected VocabularySourceRelocationService relocationService;

    @Override
    protected String getAnnotationSource()
    {
        return this.relocationService.getRelocation(ANNOTATION_SOURCE);
    }

    @Override
    protected String getDirectPhenotypesLabel()
    {
        return ACTUAL_NOT_SYMPTOM;
    }

    @Override
    protected String getAllAncestorPhenotypesLabel()
    {
        return NOT_SYMPTOM;
    }
}
