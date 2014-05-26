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
package org.phenotips.data.export.OLD_INTERNAL;

import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.Patient;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DataToCellConverter
{
    private DataToCellFormatter formatter;

    public DataToCellConverter(DataToCellFormatter formatter)
    {
        this.formatter = formatter;
    }

    protected void convertFeatures(Set<String> enabledFields, List<Patient> patients, Integer dataRow)
    {
        String sectionName = "phenotype";
        String[] fieldIds =
            {"phenotype", "phenotype_code", "phenotype_combined", "phenotype_code_meta", "phenotype_meta", "negative_phenotype"};
        String[][] headerIds = {{"phenotype"}, {"code"}, {"phenotype", "code"}, {"meta_code"}, {"meta"}, {"negative"}};
        Set<String> present = new HashSet<String>();
        List<Set<DataCell>> allCells = new LinkedList<Set<DataCell>>();
        Boolean bothTypes = false;

        int counter = 0;
        for (String fieldId : fieldIds) {
            if (enabledFields.remove(fieldId)) {
                for (String headerId : headerIds[counter]) {
                    present.add(headerId);
                }
            }
            counter++;
        }
        if (present.isEmpty()) {
            return;
        }

        /** Adding headers */
        Set<DataCell> headers = new HashSet<DataCell>();
        List<String> orderedHeaderIds = new LinkedList<String>();
        orderedHeaderIds.add("phenotype");
        orderedHeaderIds.add("code");
        orderedHeaderIds.add("meta");
        orderedHeaderIds.add("meta_code");
        List<String> orderedHeaderNames = new LinkedList<String>();
        orderedHeaderNames.add("Name");
        orderedHeaderNames.add("HPO Code");
        orderedHeaderNames.add("Details");
        orderedHeaderNames.add("HPO Code");

        counter = 0;
        Short hX = 0;
        if (present.contains("phenotype") && present.contains("negative")) {
            DataCell cell = new DataCell("Present", hX, (short) 1, dataRow);
            headers.add(cell);
            hX = (short) (hX + 1);
            bothTypes = true;
        }
        for (String headerId : orderedHeaderIds) {
            if (!present.contains(headerId)) {
                continue;
            }
            DataCell cell = new DataCell(orderedHeaderNames.get(counter), hX, (short) 1, dataRow);
            headers.add(cell);
            hX = (short) (hX + 1);
            counter++;
        }
        DataCell sectionHeader = new DataCell("Phenotype", (short) 0, (short) 0, dataRow);
        headers.add(sectionHeader);
        formatter.addHeaderSection(headers, dataRow);

        Integer patientIndex = 0;
        for (Patient patient : patients) {
            short y = 0;
            short x;
            Set<DataCell> patientCells = new HashSet<DataCell>();
            Set<? extends Feature> features = patient.getFeatures();
            List<Feature> sortedFeatures = new LinkedList<Feature>();
            for (Feature feature : features) {
                boolean positive = present.contains("phenotype");
                boolean negative = present.contains("negative");
                if (feature.isPresent() && positive) {
                    sortedFeatures.add(0, feature);
                }else if (!feature.isPresent() && negative) {
                    sortedFeatures.add(feature);
                }
            }
            Boolean lastStatus = false;
            for (Feature feature : sortedFeatures) {
                x = 0;
                boolean negative = !feature.isPresent();
                if (bothTypes && lastStatus != feature.isPresent()) {
                    lastStatus = feature.isPresent();
                    DataCell cell = new DataCell(lastStatus ? "Yes" : "No", x, y, patientIndex, dataRow);
                    if (negative) {
                        cell.styleRequests.add("shade");
                    }
                    patientCells.add(cell);
                }
                if (bothTypes) {
                    x = (short) (x + 1);
                }
                if (present.contains("phenotype")) {
                    DataCell cell = new DataCell(feature.getName(), x, y, patientIndex, dataRow);
                    if (negative) {
                        cell.styleRequests.add("shade");
                    }
                    patientCells.add(cell);
                    x = (short) (x + 1);
                }
                if (present.contains("code")) {
                    DataCell cell = new DataCell(feature.getId(), x, y, patientIndex, dataRow);
                    if (negative) {
                        cell.styleRequests.add("shade");
                    }
                    patientCells.add(cell);
                    x = (short) (x + 1);
                }
                if (present.contains("meta") || present.contains("meta_code")) {
                    Short mX = x;
                    Short mCX = (short) (x + 1);
                    Collection<? extends FeatureMetadatum> featureMetadatum = feature.getMetadata().values();
                    Boolean metaPresent = featureMetadatum.size() > 0;
                    for (FeatureMetadatum meta : featureMetadatum) {
                        if (present.contains("meta")) {
                            DataCell cell = new DataCell(meta.getName(), mX, y, patientIndex, dataRow);
                            if (negative) {
                                cell.styleRequests.add("shade");
                            }
                            cell.setYMerge(0);
                            patientCells.add(cell);
                        }
                        if (present.contains("meta_code")) {
                            DataCell cell = new DataCell(meta.getId(), mCX, y, patientIndex, dataRow);
                            if (negative) {
                                cell.styleRequests.add("shade");
                            }
                            cell.setYMerge(0);
                            patientCells.add(cell);
                        }
                        y = (short) (y + 1);
                    }
                    if (metaPresent) {
                        y = (short) (y - 1);
                    } else {
                        DataCell fCellN = new DataCell("", mX, y, patientIndex, dataRow);
                        DataCell fCellC = new DataCell("", mCX, y, patientIndex, dataRow);
                        fCellN.setYMerge(0);
                        fCellC.setYMerge(0);
                        if (negative) {
                            fCellN.styleRequests.add("shade");
                            fCellC.styleRequests.add("shade");
                        }
                        patientCells.add(fCellN);
                        patientCells.add(fCellC);
                    }
                }
                y = (short) (y + 1);
            }
            allCells.add(patientCells);
            patientIndex++;
        }
        formatter.addSection(allCells, dataRow);
    }

    protected void convertIDs(Set<String> enabledFields, List<Patient> patients, Integer dataRow)
    {
        String sectionName = "id";
        Integer patientIndex = 0;
        List<Set<DataCell>> allCells = new LinkedList<Set<DataCell>>();
        if (!enabledFields.remove("doc.name")) {
            return;
        }
        Set<DataCell> headerSet = new HashSet<DataCell>();
        DataCell header = new DataCell("Identifier", (short) 0, (short) 0, dataRow);
        headerSet.add(header);
        formatter.addHeaderSection(headerSet, dataRow);

        for (Patient patient : patients) {
            Set<DataCell> patientCells = new HashSet<DataCell>();
            DataCell cell = new DataCell(patient.getExternalId(), (short) 0, (short) 0, patientIndex, dataRow);
            patientCells.add(cell);
            allCells.add(patientCells);

            patientIndex++;
        }
        formatter.addSection(allCells, dataRow);
    }
}
