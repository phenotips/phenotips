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
package org.phenotips.data.export.internal;

import org.phenotips.data.Patient;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Assembles the various DataSections.
 */
public class SheetAssembler
{
    DataSection oneSection = new DataSection();

    Integer headerHeight = 0;

    public SheetAssembler(Set<String> enabledFields, List<Patient> patients,
        Map<Patient, XWikiDocument> patientToDocMap) throws Exception
    {
        DataToCellConverter converter = new DataToCellConverter();

        /* Some sections require setup, which need to be run here. */
        converter.phenotypeSetup(enabledFields);
        converter.prenatalPhenotypeSetup(enabledFields);

        /* Important. Headers MUST be generated first. Some of them contain setup code for the body */
        List<DataSection> headers = generateHeader(converter, enabledFields);
        List<List<DataSection>> bodySections = generateBody(converter, patients, patientToDocMap);

        List<DataSection> patientsCombined = new LinkedList<DataSection>();
        for (List<DataSection> patientSections : bodySections) {
            for (DataSection section : patientSections) {
                section.finalizeToMatrix();
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

        headerHeight = headerCombined.getMaxX();
        oneSection = assembleSectionsY(Arrays.asList(headerCombined, bodyCombined), true);

        /* Extend the section borders all the way to the bottom of the sheet */
        Styler.extendStyleVertically(oneSection, StyleOption.SECTION_BORDER_LEFT, StyleOption.SECTION_BORDER_RIGHT);
    }

    private List<List<DataSection>> generateBody(DataToCellConverter converter, List<Patient> patients,
        Map<Patient, XWikiDocument> patientToDocMap) throws Exception
    {
        List<List<DataSection>> allSections = new LinkedList<List<DataSection>>();
        for (Patient patient : patients) {
            /* To weed out null sections */
            List<DataSection> _patientSections = new LinkedList<DataSection>();
            List<DataSection> patientSections = new LinkedList<DataSection>();
            _patientSections.add(converter.idBody(patient));
            /* An unfortunate need for the XWiki patient doc. This should be fixed in PhenoTipsPatient */
            _patientSections.add(converter.documentInfoBody(patientToDocMap.get(patient)));
            _patientSections.add(converter.patientInfoBody(patient));
            _patientSections.add(converter.familyHistoryBody(patient));
            _patientSections.add(converter.prenatalPerinatalHistoryBody(patient));
            _patientSections.add(converter.prenatalPhenotypeBody(patient));
            _patientSections.add(converter.medicalHistoryBody(patient));
            _patientSections.add(converter.isNormalBody(patient));
            _patientSections.add(converter.phenotypeBody(patient));
            _patientSections.add(converter.omimBody(patient));

            /* Null section filter */
            for (DataSection section : _patientSections) {
                if (section != null) {
                    patientSections.add(section);
                }
            }
            allSections.add(patientSections);
        }
        return allSections;
    }

    private List<DataSection> generateHeader(DataToCellConverter converter, Set<String> enabledFields) throws Exception
    {
        List<DataSection> headerSections = new LinkedList<DataSection>();
        List<DataSection> _headerSections = new LinkedList<DataSection>();
        _headerSections.add(converter.idHeader(enabledFields));
        _headerSections.add(converter.documentInfoHeader(enabledFields));
        _headerSections.add(converter.patientInfoHeader(enabledFields));
        _headerSections.add(converter.familyHistoryHeader(enabledFields));
        _headerSections.add(converter.prenatalPerinatalHistoryHeader(enabledFields));
        _headerSections.add(converter.prenatalPhenotypeHeader());
        _headerSections.add(converter.medicalHistoryHeader(enabledFields));
        _headerSections.add(converter.isNormalHeader(enabledFields));
        _headerSections.add(converter.phenotypeHeader());
        _headerSections.add(converter.omimHeader(enabledFields));

        for (DataSection section : _headerSections) {
            if (section != null) {
                headerSections.add(section);
            }
        }
        return headerSections;
    }

    private DataSection assembleSectionsX(Collection<DataSection> sections, Boolean finalize)  throws Exception {
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

    private DataSection assembleSectionsY(Collection<DataSection> sections, Boolean finalize) throws Exception {
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

    public DataSection getAssembled()
    {
        return oneSection;
    }
}
