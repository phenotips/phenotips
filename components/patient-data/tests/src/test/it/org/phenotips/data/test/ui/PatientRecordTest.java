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
package org.phenotips.data.test.ui;

import org.phenotips.data.test.po.PatientRecordEditPage;
import org.phenotips.data.test.po.PatientRecordViewPage;
import org.phenotips.navigation.test.po.HomePage;

import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AuthenticationRule;
import org.xwiki.test.ui.JDoeAuthenticationRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verify the overall Administration application features.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class PatientRecordTest extends AbstractTest
{
    PatientRecordEditPage patientEdit;

    @Rule
    public AuthenticationRule authenticationRule = new JDoeAuthenticationRule(getUtil(), getDriver(), true);

    @Before
    public void setUp()
    {
        // Going to a new patient page
        this.patientEdit = HomePage.gotoPage().clickNewPatientRecord();
    }

    @Test
    public void patientInformationTest()
    {
        /* PATIENT INFORMATION */
        this.patientEdit.setPatientName("Caesar", "Salad");
        this.patientEdit.setPatientDateOfBirth("01", "04", "2013");
        this.patientEdit.setPatientDateOfDeath("02", "03", "2014");
        this.patientEdit.setPatientGender("male");
        // Need unique identifier each time so use current time
        long currentTime = System.currentTimeMillis();
        this.patientEdit.setPatientIdentifier(String.valueOf(currentTime));
        // check hint
        this.patientEdit.moreInfoSex();
        Assert.assertTrue(this.patientEdit.checkMoreInfoSex().contains(
            "Sex should be recorded as biological sex rather than gender."));
        this.patientEdit.clickBody();
        this.patientEdit.setIndicationForReferral("indicate referral");
        // check hint
        this.patientEdit.moreInfoIndicationForReferral();
        Assert
            .assertTrue(this.patientEdit
                .checkMoreInfoIndicationForReferral()
                .contains(
                    "This content can be a short summary or include content from the patient record. DO NOT RECORD any Protected Health Information."));
        this.patientEdit.clickBody();

        PatientRecordViewPage patientView = this.patientEdit.clickSaveAndView();
        Assert.assertEquals("Ceasar, Salad", patientView.getPatientInformationSummary("fieldPatientName"));
        Assert.assertEquals("2013-04-01", patientView.getPatientInformationSummary("fieldDateOfBirth"));
        Assert.assertEquals("2014-03-02", patientView.getPatientInformationSummary("fieldDateOfDeath"));
        Assert.assertEquals("Male", patientView.getPatientInformationSummary("fieldSex"));
        Assert.assertEquals("indicate referral",
            patientView.getPatientInformationSummary("fieldIndicationForReferral"));
    }

    @Test
    public void familyHistoryTest()
    {
        /* FAMILY HISTORY */
        this.patientEdit.expandFamilyHistory();
        this.patientEdit.newEntryFamilyStudy("Child", "2");
        this.patientEdit.moreInfoNewEntryFamilyStudy();
        // check hint
        Assert.assertTrue(this.patientEdit.checkMoreInfoFamilyStudy().contains(
            "Create links to other patients or family members in the system."));
        this.patientEdit.setEthnicites("Arab", "Japanese");
        this.patientEdit.setGlobalModeOfInheritance();
        this.patientEdit.familyHealthConditions("Autism, Dementia, Asthma");
        this.patientEdit.moreInfoFamilyHealthConditions();
        // check hint
        Assert
            .assertTrue(this.patientEdit
                .checkMoreInfoFamilyHealthConditions()
                .contains(
                    "List health conditions found in family (describe the relationship with proband). DO NOT RECORD any Protected Health Information."));

        PatientRecordViewPage patientView = this.patientEdit.clickSaveAndView();
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldRelativeDescription").contains("Child"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldRelativeOfPatientWithIdentifier").contains("2"));
        Assert.assertEquals("Arab", patientView.getFamilyHistorySummary("fieldMaternalEthnicity"));
        Assert.assertEquals("Japanese", patientView.getFamilyHistorySummary("fieldPaternalEthnicity"));
        Assert.assertEquals("Autism, Dementia, Asthma", patientView.getFamilyHistorySummary("fieldHealthConditions"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Sporadic"));
        Assert.assertTrue(
            patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Autosomal dominant inheritance"));
        Assert.assertTrue(
            patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Polygenic inheritance"));
    }

    @Test
    public void prenatalAndPerinatalHistoryTest()
    {
        /* PRENATAL AND PERINATAL HISTORY */
        this.patientEdit.expandPrenatalAndPerinatalHistory();
        this.patientEdit.clickTermBirth();
        this.patientEdit.setAssistedReproduction();
        this.patientEdit.setAPGARScores("2", "5");
        this.patientEdit.setPrenatalNotes("Thai food is delicious");
        // TODO: fix visibility issues

        PatientRecordViewPage patientView = this.patientEdit.clickSaveAndView();
        Assert.assertTrue(
            patientView.getPrenatalAndPerinatalHistorySummary("fieldGestationAtDelivery").contains("Term birth"));
        Assert.assertTrue(patientView.getPrenatalAndPerinatalHistorySummary("fieldConceptionAfterFertility")
            .contains("Conception after fertility medication"));
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldIVF"), "In vitro fertilization");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreOneMinute"), "2");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreFiveMinutes"), "5");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldPrenatalNotes"),
            "Thai food is delicious");
        // This should be set automatically since term birth was specified
        Assert.assertTrue(
            patientView.getPrenatalAndPerinatalHistorySummary("fieldPrematureBirth").contains("NO Premature birth"));
    }

    @Test
    public void ObstetricHistoryTest(){
        this.patientEdit.expandPrenatalAndPerinatalHistory();

    }

    @Test
    public void parentalAgeTest(){
        this.patientEdit.expandPrenatalAndPerinatalHistory();
        this.patientEdit.setMaternalAgeAtEDD("25");
        this.patientEdit.setPaternalAgeAtEDD("27");

        PatientRecordViewPage patientView = this.patientEdit.clickSaveAndView();

        Assert.assertEquals("25", patientView.getPrenatalAndPerinatalHistorySummary("fieldMaternalAgeAtEDD"));
        Assert.assertEquals("27", patientView.getPrenatalAndPerinatalHistorySummary("fieldPaternalAgeAtEDD"));

        this.patientEdit.setPaternalAgeAtEDD("");
        patientView = this.patientEdit.clickSaveAndView();
        Assert.assertEquals("25", patientView.getPrenatalAndPerinatalHistorySummary("fieldMaternalAgeAtEDD"));
        Assert.assertNull(patientView.getPrenatalAndPerinatalHistorySummary("fieldPaternalAgeAtEDD"));
    }

    @Test
    public void medicalHistoryTest()
    {
        /* MEDICAL HISTORY */
        this.patientEdit.expandMedicalHistory();
        this.patientEdit.setMedicalHistory("This is the medical history");
        this.patientEdit.setLateOnset();
        // check if upload image button exists
        Assert.assertTrue(this.patientEdit.findElementsUploadImage() > 0);
    }

    @Test
    public void measurementsTest()
    {
        /* MEASUREMENTS */
        this.patientEdit.expandMeasurements();
        this.patientEdit.createNewMeasurementsEntry();

        // First column
        this.patientEdit.setMeasurementWeight("1");
        this.patientEdit.setMeasurementArmSpan("2");
        this.patientEdit.setMeasurementHeadCircumference("3");
        this.patientEdit.setMeasurementOuterCanthalDistance("4");
        this.patientEdit.setMeasurementLeftHandLength("5");
        this.patientEdit.setMeasurementRightHandLength("18");

        // Second column
        this.patientEdit.setMeasurementHeight("6");
        this.patientEdit.setMeasurementSittingHeight("7");
        this.patientEdit.setMeasurementPhiltrumLength("8");
        this.patientEdit.setMeasurementInnerCanthalDistance("9");
        this.patientEdit.setMeasurementLeftPalmLength("10");
        this.patientEdit.setMeasurementRightPalmLength("11");

        // Third column
        this.patientEdit.setMeasurementLeftEarLength("12");
        this.patientEdit.setMeasuremtnePalpebralFissureLength("13");
        this.patientEdit.setMeasurementLeftFootLength("14");
        this.patientEdit.setMeasurementRightFootLength("15");

        // Fourth column
        this.patientEdit.setMeasurementRightEarLength("16");
        this.patientEdit.setMeasurementInterpupilaryDistance("17");
    }

    @Test
    public void genotypeInformationTest()
    {
        /* GENOTYPE INFORMATION */
        this.patientEdit.expandGenotypeInformation();

        // Check that suggestions bar appears
        this.patientEdit.openNewEntryListOfCandidateGenes();
        Assert.assertTrue(this.patientEdit.checkGeneCandidateSearchHideSuggestions("a") > 0);
        this.patientEdit.setGeneCandidateComment("Genes");

        // Check that suggestions bar appears
        this.patientEdit.openNewEntryPreviouslyTested();
        // Assert.assertTrue(patientEdit.checkGenePreviouslySearchHideSuggestions("a") > 1);
        // patientEdit.setPreviouslyTestedGenesComment("Genes");
    }

    @Test
    public void clinicalSymptomsTest()
    {
        /* CLINICAL SYMPTOMS AND PHYSICAL FINDINGS */
        this.patientEdit.expandClinicalSymptomsAndPhysicalFindings();
    }

    @Test
    public void diagnosisTest()
    {
        /* DIAGNOSIS */
    }

    @Test
    public void caseResolutionTest()
    {
        /* CASE RESOLUTION */
        this.patientEdit.expandCaseResolution();
        this.patientEdit.setCaseSolved();
        this.patientEdit.setIDsAndNotes("pubmed", "gene", "good");

        PatientRecordViewPage patientView = this.patientEdit.clickSaveAndView();
        Assert.assertEquals("Case solved", patientView.getCaseResolutionSummary("caseResolution"));
        Assert.assertEquals("pubmed", patientView.getCaseResolutionSummary("fieldPubmed"));
        Assert.assertEquals("gene", patientView.getCaseResolutionSummary("fieldGeneID"));
        Assert.assertEquals("good", patientView.getCaseResolutionSummary("fieldResolutionNotes"));
    }
}
