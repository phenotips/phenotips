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
package org.phenotips.studies.family.groupManagers;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.entities.spi.AbstractSecureOutgoingPrimaryEntityConnectionsManager;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Pedigree;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component(roles = PrimaryEntityConnectionsManager.class)
@Named(DefaultPatientsInFamilyManager.NAME)
@Singleton
public class DefaultPatientsInFamilyManager
    extends AbstractSecureOutgoingPrimaryEntityConnectionsManager<Family, Patient>
    implements PrimaryEntityConnectionsManager<Family, Patient>
{
    /** The name of this component implementation. */
    public static final String NAME = "family-contains-patient";

    /** Type instance for lookup. */
    public static final ParameterizedType TYPE =
        new DefaultParameterizedType(null, PrimaryEntityConnectionsManager.class,
            Family.class, Patient.class);

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Override
    public void initialize()
    {
        super.subjectsManager = this.familyRepository;
        super.objectsManager = this.patientRepository;
    }

    @Override
    public boolean connect(Family family, Patient patient)
    {
        // check for logical problems: patient in another family
        Collection<Family> families = getAllReverseConnections(patient);
        if (families.size() >= 1) {
            Family familyForLinkedPatient = families.iterator().next();
            if (familyForLinkedPatient != null && !familyForLinkedPatient.getId().equals(family.getId())) {
                return false;
            }
        }

        return super.connect(family, patient);
    }

    @Override
    public boolean disconnect(Family family, Patient patient)
    {
        if (!super.disconnect(family, patient)) {
            return false;
        }

        Pedigree pedigree = family.getPedigree();
        if (pedigree != null) {
            pedigree.removeLink(patient.getId());
            this.setPedigreeObject(family, pedigree);
        }

        return true;
    }

    private void setPedigreeObject(@Nonnull Family family, @Nonnull Pedigree pedigree)
    {
        XWikiContext context = this.xcontextProvider.get();
        BaseObject pedigreeObject = family.getXDocument().getXObject(Pedigree.CLASS_REFERENCE);
        pedigreeObject.set(Pedigree.IMAGE, pedigree.getImage(null), context);
        pedigreeObject.set(Pedigree.DATA, pedigree.getData().toString(), context);

        // Update proband ID every time pedigree is changed
        BaseObject familyClassObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        String probandId = pedigree.getProbandId();
        String probandFullReference = "";
        if (!StringUtils.isEmpty(probandId)) {
            Patient patient = this.patientRepository.get(probandId);
            probandFullReference = (patient == null) ? "" : patient.getDocumentReference().toString();
        }
        familyClassObject.setStringValue("proband_id", probandFullReference);
        try {
            this.xcontextProvider.get().getWiki().saveDocument(family.getXDocument(), "Updated pedigree", true,
                this.xcontextProvider.get());
        } catch (Exception ex) {
            this.logger.warn("Failed to update pedigree for [{}] {}", family.getDocumentReference(), ex.getMessage());
        }
    }
}
