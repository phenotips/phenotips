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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        Assert.assertEquals(patientView.getPatientInformationSummary("fieldPatientName"), "Ceasar, Salad");
        Assert.assertEquals(patientView.getPatientInformationSummary("fieldDateOfBirth"), "2013-04-01");
        Assert.assertEquals(patientView.getPatientInformationSummary("fieldDateOfDeath"), "2001-03-02");
        Assert.assertEquals(patientView.getPatientInformationSummary("fieldSex"), "Male");
        Assert.assertEquals(patientView.getPatientInformationSummary("fieldIndicationForReferral"), "indicate referral");
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
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldRelativeDescription"), "Child");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldRelativeOfPatientWithIdentifier"), "2");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldMaternalEthnicity"), "Arab");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldPaternalEthnicity"), "Japanese");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldHealthConditions"), "Autism, Dementia, Asthma");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldFirstGlobalInheritence"), "Sporadic");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldSecondGlobalInheritence"), "Autosomal dominant inheritance");
        Assert.assertEquals(patientView.getFamilyHistorySummary("fieldThirdGlobalInheritence"), "Polygenic inheritance");
    }

    @Test
    public void prenatalAndPerinatalHistoryTest()
    {
        /* PRENETAL AND PERINATAL HISTORY */
        patientEdit.expandPrenatalAndPerinatalHistory();
        patientEdit.setPrenatalGestationAtDelivery("4");
        patientEdit.setAssistedReproduction();
        patientEdit.setAPGARScores("2", "5");
        patientEdit.setPrenatalNotes("Thai food is delicious");
        // TODO: fix visibility issues
        //patientEdit.setPrenatalGrowthParameters("I had great thai food");
        //patientEdit.setPrenatalDevelopmentOrBirth("Pad thai is good");
        
        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldGestationAtDelivery"), "4");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldFirstAssistedReproduction"), "Conception after fertility medication");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldSecondAssistedReproduction"), "NO In vitro fertilization");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreOneMinute"), "2");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldAPGARScoreFiveMinutes"), "5");
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldPrenatalNotes"), "Thai food is delicious");
        // This should be set automatically since gestation at delivery was 4
        Assert.assertEquals(patientView.getPrenatalAndPerinatalHistorySummary("fieldPrenatalNotes"), "Premature birth");
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

    @Test
    public void oldStuff()
    {
        
        //// CHECK THIS ONE

        // this is the old code

        //// Expanding clinical symptoms tab and checking it expanded
        //Assert.assertTrue(patientEdit.checkIfClinicalSymptomsAndPhysicalFindingsExpanded(By
            //.id("PhenoTips.PatientClass_0_unaffected")));

        //// Filling in all the phenotypes of the patient
        //patientEdit.moreInfoSelectObservedPhenotypes();
        //Assert.assertTrue(patientEdit.checkMoreInfoSelectObservedPhenotypes().contains(
            //"For each phenotype, choose the most specific term possible, based on the definition"));
        //patientEdit.closeMoreInfoSelectObservedPhenotypes();
        //Assert.assertTrue(patientEdit.checkShortStatureLightningBoltAppearsOnRight());
        //Assert.assertTrue(patientEdit.checkHypotelorismLightningBoltAppearsOnRight());
        //patientEdit.setWeight3rdPercentile();
        //patientEdit.setHemihypertrophyYes();
        //patientEdit.setCraniosynostosisYes();
        //patientEdit.setCleftUpperLipNO();
        //patientEdit.setCleftPalateYes();
        //patientEdit.setAbnormalFacialShapeYes();
        //patientEdit.setVisualImpairmentYes();
        //patientEdit.setAbnormalityOfTheCorneaNO();
        //patientEdit.setColobomaNO();
        //patientEdit.setSensorineuralNO();
        //patientEdit.setAbnormalityOfTheInnerEarYes();
        //patientEdit.setHyperpigmentationOfTheSkinYes();
        //patientEdit.setCapillaryHemangiomasNO();
        //patientEdit.setVentricularSeptalDefectNO();
        //patientEdit.setArrhythmiaYes();
        //patientEdit.setCongenitalDiaphragmaticHerniaYes();
        //patientEdit.setAbnormalityOfTheLungNO();
        //patientEdit.setLowerLimbUndergrowthYes();
        //patientEdit.setScoliosisYes();
        //patientEdit.setAbnormalityOfTheVertebralColumnNO();
        //patientEdit.setCholestasisYes();
        //patientEdit.setDiabetesMellitusNO();
        //patientEdit.setHorseshoeKidneyNO();
        //patientEdit.setHypospadiasYes();
        //patientEdit.setDelayedFineMotorDevelopmentYes();
        //patientEdit.setAttentionDeficitHyperactivityDisorderNO();
        //patientEdit.setAustismYes();
        //patientEdit.setSeizuresYes();
        //patientEdit.setSpinalDysraphismNO();
        //patientEdit.setGastroschisisYes();
        //// patientEdit.setHypotelorismYes();

        //// Checking sidebars function correctly
        //patientEdit.setPreauricularPitYes();
        //Assert.assertTrue(patientEdit.checkPreauricularPitAppearsOnRight());
        //patientEdit.setNystagmusNO();
        //Assert.assertTrue(patientEdit.checkNystagmusAppearsOnRightNO());
        //patientEdit.deleteNystagmusFromRight();
        //Assert.assertTrue(patientEdit.checkNystagmusReturnsToNA());

        //// Checking that the drop down menus for the phenotypes work
        //patientEdit.coarctationOfAortaDropDown();
        //Assert.assertTrue(patientEdit.checkCoarctationOfAortaDropDown().contains("Coarctation of abdominal aorta"));

        //// Checking more info button functions correctly
        //patientEdit.moreInfoHypotelorism();
        //patientEdit.checkMoreInfoOpened();
        //patientEdit.closeMoreInfoHypotelorism();

        //// Adding details to different phenotypes in the current selection selection
        //patientEdit.hemihypertrophyAddDetails();
        //patientEdit.ageOfOnsetHemihypertrophy();
        //patientEdit.setNeonatalOnsetHemihypertrophy();
        //patientEdit.temporalPatternHemihypertrophy();
        //patientEdit.setSubacuteTemporalPatternHemihypertrophy();
        //patientEdit.deleteCleftPalate();
        //patientEdit.abnormalFacialShapeAddDetails();
        //patientEdit.paceOfProgressionAbnormalFacialShape();
        //patientEdit.slowPaceOfProgressionAbnormalFacialShape();
        //patientEdit.hypotelorismAddDetails();
        //patientEdit.severityHypotelorism();
        //patientEdit.moderateSeverityHypotelorism();
        //patientEdit.abnormalityOfTheInnerEarAddDetails();
        //patientEdit.spatialPatternAbnormalityOfTheInnerEar();
        //patientEdit.distalSpatialPatternAbnomalityOfTheInnerEar();
        //patientEdit.arrythmiaAddDetails();
        //patientEdit.arrythmiaComments("test");
        //patientEdit.scoliosisAddDetails();
        //patientEdit.lateralityScoliosis();
        //patientEdit.rightLateralityScoliosis();
        //patientEdit.seizuresAddDetails();
        //patientEdit.severitySeizures();
        //patientEdit.mildSeveritySeizures();

        //// FIXME I DON'T WORK
        //// Adding new phenotypes from the "You may want to investigate" box
        //// patientEdit.bifidTongueYes();
        //// Assert.assertTrue(patientEdit.checkBifidTongueDissapearsFromRightInvestigateBox());

        //// Adding a phenotype from the quick search bar
        //patientEdit.phenotypeQuickSearch("Cataract");
        //patientEdit.quickSearchCataractYes();

        //// Checking the "browse related terms" under quick search bar functions correctly
        //patientEdit.phenotypeQuickSearch("Cataract");
        //patientEdit.browseRelatedTermsCataract();
        //patientEdit.abnormalityOfTheLensGoUP();
        //patientEdit.phacodonesisYes();
        //patientEdit.abnormalityOfTheAnteriorSegmentOfTheEye();
        //patientEdit.closeBrowseRelatedTerms();
        //patientEdit.hideQuickSearchBarSuggestions();
        //Assert.assertTrue(patientEdit.checkPhacodonesisAppearsOnRight());

        //// Checking You many want to investigate tab hides
        //patientEdit.hideYouMayWantToInvestigate();
        //Assert.assertFalse(patientEdit.checkYouMayWantToInvestigateHid());
        //patientEdit.hideYouMayWantToInvestigate();

        //// Checking expand and collapse buttons function correctly

        //// Expanding diagnois tab and checking it expanded
        //patientEdit.expandDiagnosis();
        //Assert.assertEquals("Additional comments:", patientEdit.checkDiagnosisExpaned());

        //// setting diagnosis details about patient.
        //patientEdit.setDiagnosisAdditionalComments("test");
        //patientEdit.moreInfoOMIMDisorder();
        //Assert
            //.assertTrue(patientEdit
                //.checkMoreInfoOMIMDisorder()
                //.contains(
                    //"Generally, a disorder is used to annotate the patient profile because it was either: tested and confirmed to be either present or absent OR suspected based on the manifestations (phenotypes), but not yet tested."));
        //patientEdit.closeMoreInfoOMIMDisorder();
        //patientEdit.excludeLowerLimbUndergrowth();
        //Assert.assertEquals("Lower limb undergrowth", patientEdit.checkLowerLimbUndergrowthExcluded());
        //patientEdit.setSmithLemliOptizSyndrome();
        //patientEdit.setWatsonSyndrome();
        //patientEdit.setOMIMDisorder("wolf");
        //patientEdit.setOMIMDisorderWolfSyndrome();
        //patientEdit.setCriDuChatSyndromeFromBottom();
        //Assert.assertEquals("#270400 SMITH-LEMLI-OPITZ SYNDROME", patientEdit.checkSmithLemliInOMIM());
        //Assert.assertEquals("#123450 CRI-DU-CHAT SYNDROME", patientEdit.checkCriDuChatAppears());
        //patientEdit.setCriDuChatSyndromeFromBottom();
        //Assert
            //.assertFalse(patientEdit.checkCriDuChatDisappearsFromTop(By
                //.xpath("//*[@class = 'fieldset omim_id']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']")));

        //// FIXME I DON'T WORK

        //// patientEdit.setCriDuChatSyndromeFromBottom();
        //// patientEdit.setCriDuChatFromTop();
        //// Assert.assertFalse(patientEdit.checkCriDuChatDisappearsFromBottom());
        //// Assert
        //// .assertFalse(patientEdit.checkCriDuChatDisappearsFromTop(By
        //// .xpath("//*[@class = 'fieldset omim_id']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']")));

        //Assert.assertTrue(patientEdit.checkSmithLemliSyndrome());

        //// Saving the patient record and accesing the view patient page.
        //PatientRecordViewPage patientView = patientEdit.clickSaveAndView();

        //// Checking patient details - name + gender + parental ethnicities
        //Assert.assertEquals("Doe, John", patientView.getPatientName());
        //// Assert.assertEquals("3", patientView.getPatientIdentifier());
        //Assert.assertEquals("Male", patientView.getSex());
        //List<String> maternalEthnicities = patientView.getMaternalEthnicities();
        //Assert.assertEquals(1, maternalEthnicities.size());
        //Assert.assertEquals("Arab", maternalEthnicities.get(0));
        //Assert.assertEquals("test", patientView.getIndicationForReferral());

        //// Checking Family history of patient
        //Assert.assertEquals("This patient is the Child", patientView.checkPatientRelative());
        //Assert.assertEquals("Of patient with identifier 2", patientView.checkRelativeOfPatient());
        //Assert.assertEquals("Polygenic inheritance", patientView.getGlobalModeOfInheritance());
        //Assert.assertEquals("Consanguinity", patientView.getConsanguinity());
        //Assert.assertEquals("Autism, Dementia, Asthma", patientView.getFamilyConditions());

        //// Checking prenatal and perinatal details of patient
        //Assert.assertEquals("Conception after fertility medication",
            //patientView.getConceptionAfterFertilityMedication());
        //Assert.assertEquals("In vitro fertilization", patientView.getInVitroFertilization());
        //Assert.assertEquals("Gestational surrogacy", patientView.getGestationalSurrogacy());
        //Assert.assertEquals("6", patientView.getAPGARScore1Minute());
        //Assert.assertEquals("8", patientView.getAPGARScore5Minutes());
        //Assert.assertEquals("Test", patientView.getPrenatalAndPerinatalNotes());
        //Assert.assertEquals("Test", patientView.getMedicalAndDevelopmentalHistory());
        //Assert.assertEquals("Infantile onset", patientView.getGlobalAgeOfOnset());
        //Assert.assertEquals("Positive ferric chloride test", patientView.getPositiveFerricChlorideTest());
        //Assert.assertEquals("Dysplastic testes", patientView.getDysplasticTestes());

        //// Checking measurements of patient
        //Assert.assertTrue(patientView.getMeasurementsWeight().contains("3.0 kg"));
        //Assert.assertTrue(patientView.getMeasurementsHeight().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getBMI().contains("3333.33"));
        //Assert.assertTrue(patientView.getMeasurementsArmSpan().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsSittingHeight().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsHeadCircumference().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsPhiltrumLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsLeftEarLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsRightEarLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsOuterCanthalDistance().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsInnerCanthalDistance().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsPalpebralFissureLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementInterpupilaryDistance().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsLeftHandLength().contains("3.0 cm"));
        //// Assert.assertTrue(patientView.getMeasurementsLeftPalmLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsLeftFootLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsRightHandLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsRightPalmLength().contains("3.0 cm"));
        //Assert.assertTrue(patientView.getMeasurementsRightFootLength().contains("3.0 cm"));

        //// Check genotype information - comments + gene
        //Assert.assertEquals("test", patientView.getGenotypeInformationComments());
        //Assert.assertEquals("test1", patientView.getGenotypeInformationGene());

        //// Checking phenotypes of patient.
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Stature for age", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Weight for age", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Head circumference for age", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Hemihypertrophy", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Craniosynostosis", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Cleft upper lip", false));
        //Assert.assertFalse(patientView.hasPhenotypeSelected("Cleft palate", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormal facial shape", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Visual impairment", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the cornea", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Coloboma", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Sensorineural", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the inner ear", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Hyperpigmentation of the skin", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Capillary hemangiomas", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Ventricular septal defect", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Arrhythmia", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Congenital diaphragmatic hernia", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the lung", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Lower limb undergrowth", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Scoliosis", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the vertebral column", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Cholestasis", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Diabetes mellitus", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Horseshoe kidney", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Hypospadias", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Delayed fine motor development", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Attention deficit hyperactivity disorder", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Autism", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Seizures", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Spinal dysraphism", false));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Phacodonesis", true));
        //Assert.assertTrue(patientView.hasPhenotypeSelected("Cataract", true));
        //// FIXME I DON'T WORK (Refer to line 274)
        //// Assert.assertTrue(patientView.hasPhenotypeSelected("Bifid tongue", true));

        //// Checking the details of the phenotypes
        //Assert.assertEquals("test", patientView.getDiagnosisAdditionalComments());
        //Assert.assertEquals("Neonatal onset", patientView.getNeonatalOnsetHemihypertrophy());
        //Assert.assertEquals("Subacute", patientView.getSubacuteTemporalPatternHemihypertrophy());
        //Assert.assertEquals("Slow progression", patientView.getSlowPaceOfProgressionAbnormalFacialShape());
        //Assert.assertEquals("Moderate", patientView.getModerateSeverityHypotelorism());
        //Assert.assertEquals("Distal", patientView.getDistalSpatialPatternAbnormalityOfTheInnerEar());
        //Assert.assertEquals("test", patientView.getCommentsArrhythmia());
        //Assert.assertEquals("Right", patientView.getRightLateralityScoliosis());
        //Assert.assertEquals("Mild", patientView.getMildSeveritySeizures());

        //// Checking the diagnosis is displaying correctly
        //Assert
            //.assertEquals(
                //"#270400 SMITH-LEMLI-OPITZ SYNDROME; SLOS ;;SLO SYNDROME;; RSH SYNDROME;; RUTLEDGE LETHAL MULTIPLE CONGENITAL ANOMALY SYNDROME;; POLYDACTYLY, SEX REVERSAL, RENAL HYPOPLASIA, AND UNILOBAR LUNG;; LETHAL ACRODYSGENITAL SYNDROME",
                //patientView.getSmithLemliOptizSyndrome());
        //Assert
            //.assertEquals(
                //"#193520 WATSON SYNDROME ;;PULMONIC STENOSIS WITH CAFE-AU-LAIT SPOTS;; CAFE-AU-LAIT SPOTS WITH PULMONIC STENOSIS",
                //patientView.getWatsonSyndrome());

        //Assert
            //.assertEquals(
                //"#194190 WOLF-HIRSCHHORN SYNDROME; WHS ;;CHROMOSOME 4p16.3 DELETION SYNDROME;; PITT-ROGERS-DANKS SYNDROME; PRDS;; PITT SYNDROME",
                //patientView.getWolfSyndrome());

        //// Cheking the Edit button is working correctly
        //patientView.goToEditPage();
        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //[> Checking that each tab's edit button is working correctly <]

        //// Checking patient information edit button
        //patientView.hoverOverPatientInformation();
        //patientView.goToEditPagePatientInfo();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// Checking family history edit button
        //patientView.hoverOverFamilyHistory();
        //patientView.goToEditPageFamilyHistory();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// Checking prenatal and perinatal edit button
        //patientView.hoverOverPrenatalHistory();
        //patientView.goToEditPagePrenatalHistory();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// FIXME I DON'T WORK

        //// Checking measurements edit button
        //// patientView.hoverOverMeasurements();
        //// patientView.goToEditPageMeasurements();

        //// Assert
        //// .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //// patientEdit.clickSaveAndView();
        //// Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// Checking genotype information edit button
        //patientView.hoverOverGenotypeInfo();
        //patientView.goToEditPageGenotypeInfo();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// Checking clinical symptoms edit button
        //patientView.hoverOverClinicalSymptoms();
        //patientView.goToEditPageClinicalSymptoms();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

        //// Checking diagnosis edit button
        //patientView.hoverOverDiagnosis();
        //patientView.goToEditPageDiagnosis();

        //Assert
            //.assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        //patientEdit.clickSaveAndView();
        //Assert.assertEquals("Doe, John", patientView.getPatientName());

    //}

    //@Test
    //public void patientIsClinicallyNormal()
    //{
        //// Creating a new patient and setting basic details: name + gender
        //PatientRecordEditPage patientEdit = HomePage.gotoPage().clickNewPatientRecord();
        //patientEdit.setPatientLastName("Doe");
        //patientEdit.setPatientFirstName("Jane");
        //patientEdit.setFemaleGender();

        //patientEdit.expandClinicalSymptomsAndPhysicalFindings();
        //// Setting patient to be clinically normal
        //patientEdit.setPatientClinicallyNormal();

        //// Saving the patient record and accesing the view patient page.
        //PatientRecordViewPage patientView = patientEdit.clickSaveAndView();

        //// Checking patient name, gender and clinically normal.
        //Assert.assertEquals("Doe, Jane", patientView.getPatientName());
        //Assert.assertEquals("Female", patientView.getSex());
        //Assert.assertEquals("This patient is clinically normal", patientView.getPatientIsClinicallyNormal());

    }

    //@Test
    //public void testingMeasurements()
    //{
        //PatientRecordEditPage patientEdit = HomePage.gotoPage().clickNewPatientRecord();
        //patientEdit.expandMeasurements();
        //patientEdit.createNewMeasurementsEntry();
        //patientEdit.setMeasurementWeight("1");
        //patientEdit.expandClinicalSymptomsAndPhysicalFindings();
        ////Assert.assertTrue(patientEdit.checkDecreasedBodyWeight());
        //patientEdit.setMeasurementWeight("4");
        ////Assert.assertFalse(patientEdit.checkDecreasedBodyWeightDisappears());
        //patientEdit.setMeasurementHeight("3");
        //Assert.assertTrue(patientEdit.checkShortStature());
        //patientEdit.setMeasurementHeight("53");
        //Assert.assertFalse(patientEdit.checkShortStatureDisappears());
        //Assert
            //.assertTrue(patientEdit.checkChartTitleChangesMale().contains("Weight for age, birth to 36 months, boys"));
        //Assert.assertTrue(patientEdit.checkChartTitleChangesFemale().contains(
            //"Weight for age, birth to 36 months, girls"));
        //patientEdit.setMaleGender();
        //patientEdit.openDateOfBirth();
        //patientEdit.setYearDateOfBirth("1998");
        //patientEdit.setMonthDateOfBirth("August");
        //patientEdit.setDate12();
        //patientEdit.openDateOfMeasurements();
        //patientEdit.setYearMeasurement("2014");
        //patientEdit.setMonthMeasurement("August");
        //patientEdit.setDate12();
        //Assert.assertTrue(patientEdit.checkDecreasedBodyWeight());
        //Assert.assertTrue(patientEdit.checkShortStature());
        //Assert.assertEquals("16y", patientEdit.getAgeMeasurements());
        //Assert
            //.assertTrue(patientEdit.checkChartTitleChangesOlderMale().contains("Weight for age, 2 to 20 years, boys"));
        //Assert.assertTrue(patientEdit.getWeightPctl().contains("6"));
        //Assert.assertTrue(patientEdit.getHeightPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getBMIPctl().contains("100"));
        //Assert.assertTrue(patientEdit.getSittingPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getHeadCircumferencePctl().contains("0"));
        //Assert.assertTrue(patientEdit.getPhiltrumPctl().contains("100"));
        //Assert.assertTrue(patientEdit.getLeftEarPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getRightEarPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getOuterCanthalPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getInnerCanthalPctl().contains("100"));
        //Assert.assertTrue(patientEdit.getPalpebralFissurePctl().contains("100"));
        //Assert.assertTrue(patientEdit.getInterpupilaryPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getLeftPalmPctl().contains("3"));
        //Assert.assertTrue(patientEdit.getLeftFootPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getRightPalmPctl().contains("3"));
        //Assert.assertTrue(patientEdit.getRightFootPctl().contains("0"));
        //Assert.assertTrue(patientEdit.getBMINumber().contains("3333.33"));
}
