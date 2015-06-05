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

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

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
        patientEdit = HomePage.gotoPage().clickNewPatientRecord();
    }


    @Test
    public void patientInformationTest()
    {
        /* PATIENT INFORMATION */
        patientEdit.setPatientName("Ceasar", "Salad");
        patientEdit.setPatientDateOfBirth("01", "04", "2013");
        patientEdit.setPatientDateOfDeath("02", "03", "2001");
        patientEdit.setPatientGender("male");
        // Need unique identifier each time so use current time
        long currentTime = System.currentTimeMillis();
        patientEdit.setPatientIdentifier(String.valueOf(currentTime));
        // check hint
        patientEdit.moreInfoSex();
        Assert.assertTrue(patientEdit.checkMoreInfoSex().contains(
            "Sex should be recorded as biological sex rather than gender."));
        patientEdit.setIndicationForReferral("indicate referral");
        // check hint
        patientEdit.moreInfoIndicationForReferral();
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoIndicationForReferral()
                .contains(
                    "This content can be a short summary or include content from the patient record. DO NOT RECORD any Protected Health Information."));

        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();
        Assert.assertEquals("Ceasar, Salad", patientView.getPatientInformationSummary("fieldPatientName"));
        Assert.assertEquals("2013-04-01", patientView.getPatientInformationSummary("fieldDateOfBirth"));
        Assert.assertEquals("2001-03-02", patientView.getPatientInformationSummary("fieldDateOfDeath"));
        Assert.assertEquals("Male", patientView.getPatientInformationSummary("fieldSex"));
        Assert.assertEquals("indicate referral", patientView.getPatientInformationSummary("fieldIndicationForReferral"));
    }
    
    @Test
    public void familyHistoryTest()
    {
        /* FAMILY HISTORY */
        patientEdit.expandFamilyHistory();
        patientEdit.newEntryFamilyStudy("Child", "2");
        patientEdit.moreInfoNewEntryFamilyStudy();
        // check hint
        Assert.assertTrue(patientEdit.checkMoreInfoFamilyStudy().contains(
            "Create links to other patients or family members in the system."));
        patientEdit.setEthnicites("Arab", "Japanese");
        patientEdit.setGlobalModeOfInheritance();
        patientEdit.familyHealthConditions("Autism, Dementia, Asthma");
        patientEdit.moreInfoFamilyHealthConditions();
        // check hint
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoFamilyHealthConditions()
                .contains(
                    "List health conditions found in family (describe the relationship with proband). DO NOT RECORD any Protected Health Information."));

        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldRelativeDescription").contains("Child"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldRelativeOfPatientWithIdentifier").contains("2"));
        Assert.assertEquals("Arab", patientView.getFamilyHistorySummary("fieldMaternalEthnicity"));
        Assert.assertEquals("Japanese", patientView.getFamilyHistorySummary("fieldPaternalEthnicity"));
        Assert.assertEquals("Autism, Dementia, Asthma", patientView.getFamilyHistorySummary("fieldHealthConditions"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Sporadic"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Autosomal dominant inheritance"));
        Assert.assertTrue(patientView.getFamilyHistorySummary("fieldGlobalInheritence").contains("Polygenic inheritance"));
    }

    @Test
    public void prenatalAndPerinatalHistoryTest()
    {
        /* PRENETAL AND PERINATAL HISTORY */
        patientEdit.expandPrenatalAndPerinatalHistory();
        patientEdit.clickTermBirth();
        patientEdit.setAssistedReproduction();
        patientEdit.setAPGARScores("2", "5");
        patientEdit.setPrenatalNotes("Thai food is delicious");
        // TODO: fix visibility issues
        //patientEdit.setPrenatalGrowthParameters("I had great thai food");
        //patientEdit.setPrenatalDevelopmentOrBirth("Pad thai is good");
        
        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();
        Assert.assertTrue(patientView.getPrenatalAndPerinatalHistorySummary("fieldGestationAtDelivery").contains("Term birth"));
        Assert.assertTrue(patientView.getPrenatalAndPerinatalHistorySummary("fieldConceptionAfterFertility").contains("Conception after fertility medication"));
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldIVF"), "In vitro fertilization");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreOneMinute"), "2");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreFiveMinutes"), "5");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldPrenatalNotes"), "Thai food is delicious");
        // This should be set automatically since term birth was specified
        Assert.assertTrue(patientView.getPrenatalAndPerinatalHistorySummary("fieldPrematureBirth").contains("NO Premature birth"));
    }

    @Test
    public void medicalHistoryTest()
    {
        /* MEDICAL HISTORY */
        patientEdit.expandMedicalHistory();
        patientEdit.setMedicalHistory("This is the medical history");
        patientEdit.setLateOnset();
        // check if upload image button exists
        Assert.assertTrue(patientEdit.findElementsUploadImage() > 0);
    }

    @Test
    public void measurementsTest()
    {
        /* MEASUREMENTS */
        patientEdit.expandMeasurements();
        patientEdit.createNewMeasurementsEntry();

        // First column
        patientEdit.setMeasurementWeight("1");
        patientEdit.setMeasurementArmSpan("2");
        patientEdit.setMeasurementHeadCircumference("3");
        patientEdit.setMeasurementOuterCanthalDistance("4");
        patientEdit.setMeasurementLeftHandLength("5");
        patientEdit.setMeasurementRightHandLength("18");

        // Second column
        patientEdit.setMeasurementHeight("6");
        patientEdit.setMeasurementSittingHeight("7");
        patientEdit.setMeasurementPhiltrumLength("8");
        patientEdit.setMeasurementInnerCanthalDistance("9");
        patientEdit.setMeasurementLeftPalmLength("10");
        patientEdit.setMeasurementRightPalmLength("11");

        // Third column
        patientEdit.setMeasurementLeftEarLength("12");
        patientEdit.setMeasuremtnePalpebralFissureLength("13");
        patientEdit.setMeasurementLeftFootLength("14");
        patientEdit.setMeasurementRightFootLength("15");
        
        // Fourth column
        patientEdit.setMeasurementRightEarLength("16");
        patientEdit.setMeasurementInterpupilaryDistance("17");
    }

    @Test
    public void genotypeInformationTest()
    {
        /* GENOTYPE INFORMATION */
        patientEdit.expandGenotypeInformation();

        // Check that suggestions bar appears 
        patientEdit.openNewEntryListOfCandidateGenes();
        Assert.assertTrue(patientEdit.checkGeneCandidateSearchHideSuggestions("a") > 0);
        patientEdit.setGeneCandidateComment("Genes");

        // Check that suggestions bar appears 
        patientEdit.openNewEntryPreviouslyTested();
        //Assert.assertTrue(patientEdit.checkGenePreviouslySearchHideSuggestions("a") > 1);
        //patientEdit.setPreviouslyTestedGenesComment("Genes");
    }

    @Test
    public void clinicalSymptomsTest()
    {
        /* CLINICAL SYMPTOMS AND PHYSICAL FINDINGS */
        patientEdit.expandClinicalSymptomsAndPhysicalFindings();
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
        patientEdit.expandCaseResolution();
        patientEdit.setCaseSolved();
        patientEdit.setIDsAndNotes("pubmed", "gene", "good");

        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();
        Assert.assertEquals("Case solved", patientView.getCaseResolutionSummary("caseResolution"));
        Assert.assertEquals("pubmed", patientView.getCaseResolutionSummary("fieldPubmed"));
        Assert.assertEquals("gene", patientView.getCaseResolutionSummary("fieldGeneID"));
        Assert.assertEquals("good", patientView.getCaseResolutionSummary("fieldResolutionNotes"));
    }
}
