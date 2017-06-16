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
package org.phenotips.export.internal;

import org.phenotips.data.Patient;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Assembles the various DataSections.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class SheetAssembler
{
    /** The global section meant to eventually contain all {@link org.phenotips.export.internal.DataCell}s. */
    private DataSection oneSection = new DataSection();

    /** The number of rows the header occupies. */
    private Integer headerHeight = 0;

    /**
     * Generates {@link org.phenotips.export.internal.DataCell} containing the data to export, combines them together
     * into one big matrix ({@link #oneSection}), and styles them.
     *
     * @param enabledFields set of fields for which data should be exported
     * @param patients list of patients from whom data should exported
     * @throws java.lang.Exception half of the functions used throw exceptions
     */
    public SheetAssembler(Set<String> enabledFields, List<Patient> patients) throws Exception
    {
        DataToCellConverter converter = new DataToCellConverter();

        /* Some sections require setup, which need to be run here. */
        converter.phenotypeSetup(enabledFields);
        converter.prenatalPhenotypeSetup(enabledFields);
        converter.genesSetup(enabledFields);
        converter.variantsSetup(enabledFields);

        /* Headers MUST be generated first. Some of them contain setup code for the body */
        List<DataSection> headers = generateHeader(converter, enabledFields);
        List<List<DataSection>> bodySections = generateBody(converter, patients);

        List<DataSection> patientsCombined = new LinkedList<>();
        for (List<DataSection> patientSections : bodySections) {
            for (DataSection section : patientSections) {
                section.finalizeToMatrix();
                Styler.disallowBodyStyles(section);
                Styler.extendStyleHorizontally(section, StyleOption.FEATURE_SEPARATOR, StyleOption.YES_NO_SEPARATOR);
                Styler.styleSectionBorder(section, StyleOption.SECTION_BORDER_LEFT, StyleOption.SECTION_BORDER_RIGHT);
            }

            DataSection assembled = assembleSectionsX(patientSections, true);
            Styler.styleSectionBottom(assembled, StyleOption.PATIENT_BORDER);
            patientsCombined.add(assembled);
        }

        /* Inserting styling calls here is fairly unavoidable. Also don't forget to merge BEFORE styling. */
        for (DataSection header : headers) {
            header.finalizeToMatrix();
            header.mergeX();
            Styler.styleSectionBorder(header, StyleOption.SECTION_BORDER_LEFT, StyleOption.SECTION_BORDER_RIGHT);
        }

        DataSection bodyCombined = assembleSectionsY(patientsCombined, false);
        DataSection headerCombined = assembleSectionsX(headers, true);

        /* Add style through functions. Use only with finalized sections. */
        Styler.styleSectionBottom(headerCombined, StyleOption.HEADER_BOTTOM);

        this.headerHeight = headerCombined.getMaxY() + 1;
        this.oneSection = assembleSectionsY(Arrays.asList(headerCombined, bodyCombined), true);

        /* Extend the section borders all the way to the bottom of the sheet */
        Styler
            .extendStyleVertically(this.oneSection, StyleOption.SECTION_BORDER_LEFT, StyleOption.SECTION_BORDER_RIGHT);
    }

    /**
     * Instruction list of which {@link org.phenotips.export.internal.DataToCellConverter}'s functions to call with a
     * null {@link org.phenotips.export.internal.DataSection} filter.
     *
     * @return list of generated, not null {@link org.phenotips.export.internal.DataSection}s
     */
    private List<List<DataSection>> generateBody(DataToCellConverter converter, List<Patient> patients)
        throws Exception
    {
        List<List<DataSection>> allSections = new LinkedList<>();
        for (Patient patient : patients) {
            if (patient == null) {
                continue;
            }
            List<DataSection> patientSections = new LinkedList<>();
            patientSections.add(converter.idBody(patient));
            patientSections.add(converter.documentInfoBody(patient));
            patientSections.add(converter.patientInfoBody(patient));
            patientSections.add(converter.familyHistoryBody(patient));
            patientSections.add(converter.prenatalPerinatalHistoryBody(patient));
            patientSections.add(converter.prenatalPhenotypeBody(patient));
            patientSections.add(converter.medicalHistoryBody(patient));
            patientSections.add(converter.isNormalBody(patient));
            patientSections.add(converter.phenotypeBody(patient));
            patientSections.add(converter.genesBody(patient));
            patientSections.add(converter.geneticNotesBody(patient));
            patientSections.add(converter.variantsBody(patient));
            patientSections.add(converter.clinicalDiagnosisBody(patient));
            patientSections.add(converter.disordersBody(patient));
            patientSections.add(converter.diagnosisNotesBody(patient));
            patientSections.add(converter.isSolvedBody(patient));

            /* Null section filter */
            Iterator<DataSection> it = patientSections.iterator();
            while (it.hasNext()) {
                DataSection i = it.next();
                if (i == null) {
                    it.remove();
                }
            }
            allSections.add(patientSections);
        }
        return allSections;
    }

    /**
     * Same as {@link #generateBody(DataToCellConverter, java.util.List)} but for header sections. Most of header
     * functions from {@link org.phenotips.export.internal.DataToCellConverter} contain some set up code.
     */
    private List<DataSection> generateHeader(DataToCellConverter converter, Set<String> enabledFields) throws Exception
    {
        List<DataSection> headerSections = new LinkedList<>();
        headerSections.add(converter.idHeader(enabledFields));
        headerSections.add(converter.documentInfoHeader(enabledFields));
        headerSections.add(converter.patientInfoHeader(enabledFields));
        headerSections.add(converter.familyHistoryHeader(enabledFields));
        headerSections.add(converter.prenatalPerinatalHistoryHeader(enabledFields));
        headerSections.add(converter.prenatalPhenotypeHeader());
        headerSections.add(converter.medicalHistoryHeader(enabledFields));
        headerSections.add(converter.isNormalHeader(enabledFields));
        headerSections.add(converter.phenotypeHeader());
        headerSections.add(converter.genesHeader());
        headerSections.add(converter.geneticNotesHeader(enabledFields));
        headerSections.add(converter.variantsHeader());
        headerSections.add(converter.clinicalDiagnosisHeaders(enabledFields));
        headerSections.add(converter.disordersHeaders(enabledFields));
        headerSections.add(converter.diagnosisNotesHeader(enabledFields));
        headerSections.add(converter.isSolvedHeader(enabledFields));

        Iterator<DataSection> it = headerSections.iterator();
        while (it.hasNext()) {
            DataSection i = it.next();
            if (i == null) {
                it.remove();
            }
        }
        return headerSections;
    }

    /** Combines the passed in sections into one large section, keeping track of positioning along the x axis. */
    private DataSection assembleSectionsX(List<DataSection> sections, Boolean finalize) throws Exception
    {
        DataSection combinedSection = new DataSection();

        Integer offset = 0;
        for (DataSection section : sections) {
            Set<DataCell> cells = section.getCellList();

            for (DataCell cell : cells) {
                cell.setX(cell.getX() + offset);
                combinedSection.addCell(cell);
            }
            // Don't forget that offset needs to be added to the previous value
            offset = offset + section.getMaxX() + 1;
        }
        if (finalize) {
            combinedSection.finalizeToMatrix();
        }

        return combinedSection;
    }

    /** Combines the passed in sections into one large section, keeping track of positioning along the y axis. */
    private DataSection assembleSectionsY(List<DataSection> sections, Boolean finalize) throws Exception
    {
        DataSection combinedSection = new DataSection();

        Integer offset = 0;
        for (DataSection section : sections) {
            Set<DataCell> cells = section.getCellList();

            for (DataCell cell : cells) {
                cell.setY(cell.getY() + offset);
                combinedSection.addCell(cell);
            }
            offset += section.getMaxY() + 1;
        }
        if (finalize) {
            combinedSection.finalizeToMatrix();
        }

        return combinedSection;
    }

    /**
     * @return a {@link org.phenotips.export.internal.DataSection} that contains all
     *         {@link org.phenotips.export.internal.DataCell}s
     */
    public DataSection getAssembled()
    {
        return this.oneSection;
    }

    /**
     * @return {@link #headerHeight}
     */
    public Integer getHeaderHeight()
    {
        return this.headerHeight;
    }
}
