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

import org.phenotips.vocabulary.AbstractCSVAnnotationsExtension;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyInputTerm;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.csv.CSVFormat;

/**
 * Extends {@link AbstractCSVAnnotationExtension} to annotate an HPO {@link VocabularyInputTerm} with its associated
 * genes.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("hpo-annotation-associated-genes")
@Singleton
public class GeneForPhenotypesAnnotationsExtension extends AbstractCSVAnnotationsExtension
{
    /** The source URL for phenotype annotations. */
    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations.monthly/"
        + "lastStableBuild/artifact/annotation/ALL_SOURCES_ALL_FREQUENCIES_phenotype_to_genes.txt";

    private static final Collection<String> TARGET_VOCABULARIES = Collections.singletonList("hpo");

    @Override
    protected Collection<String> getTargetVocabularyIds()
    {
        return TARGET_VOCABULARIES;
    }

    @Override
    protected String getAnnotationSource()
    {
        return ANNOTATION_SOURCE;
    }

    @Override
    protected CSVFormat setupCSVParser(Vocabulary vocabulary)
    {
        // Bug in commons-csv: although duplicate null headers are allowed in CSVParser, CSVFormat#validate doesn't
        // allow more than one null header
        return CSVFormat.TDF.withHeader("id", null, "", "associated_genes").withAllowMissingColumnNames()
            .withCommentMarker('#');
    }
}
