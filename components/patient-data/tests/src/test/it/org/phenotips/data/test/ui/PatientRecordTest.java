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
package org.phenotips.data.test.ui;

import org.phenotips.data.test.po.PatientRecordEditPage;
import org.phenotips.data.test.po.PatientRecordViewPage;
import org.phenotips.navigation.test.po.HomePage;

import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
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
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil(), getDriver());

    @Test
    public void verifyPatientRecordEdit()
    {
        // Going to a new patient page
        PatientRecordEditPage patientEdit = HomePage.gotoPage().clickNewPatientRecord();
        // Setting details about patient - name + gender + date of birth
        patientEdit.setPatientLastName("Doe");
        patientEdit.setPatientFirstName("John");
        patientEdit.openDateOfBirth();
        patientEdit.setDate1();
        patientEdit.setMaleGender();
        patientEdit.moreInfoSex();
        Assert.assertTrue(patientEdit.checkMoreInfoSex().contains(
            "Sex should be recorded as biological sex rather than gender."));
        patientEdit.closeMoreInfoSex();
        patientEdit.setIndicationForReferral("test");

        // Checking more information button for indication for referral
        patientEdit.moreInfoIndicationForReferral();
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoIndicationForReferral()
                .contains(
                    "This content can be a short summary or include content from the patient record. DO NOT RECORD any Protected Health Information."));
        patientEdit.closeMoreInfoIndicationForReferral();

        // Filling family history of patient
        patientEdit.expandFamilyHistory();
        // Checking family history tab expanded.
        patientEdit.newEntryFamilyStudy();
        patientEdit.setPatientIsTheRelativeOf("Child");
        patientEdit.relativeOfCurrentPatient("2");
        // Checking more information button for new entry for family study
        patientEdit.moreInfoNewEntryFamilyStudy();
        Assert.assertTrue(patientEdit.checkMoreInfoFamilyStudy().contains(
            "Create links to other patients or family members in the system."));
        patientEdit.closeMoreInfoNewEntryFamilyStudy();
        Assert.assertTrue(patientEdit.checkFamilyHistoryExpanded());
        patientEdit.setMaternalEthnicity("Arab");
        patientEdit.setPaternalEthnicity("Japanese");
        patientEdit.polygenicInheritance();
        patientEdit.setConsanguinityYes();
        patientEdit.familyHealthConditions("Autism, Dementia, Asthma");
        // Checking more information buttion for family health conditions
        patientEdit.moreInfoFamilyHealthConditions();
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoFamilyHealthConditions()
                .contains(
                    "List health conditions found in family (describe the relationship with proband). DO NOT RECORD any Protected Health Information."));
        patientEdit.closeMoreInfoFamilyHealthConditions();

        // Setting details about the prenatal and parinatal history of the patient
        patientEdit.expandPrenatalAndPerinatalHistory();
        // Checking prenatal and perinatal history tab expanded
        Assert.assertTrue(patientEdit.checkPrenatalAndPerinatalHistoryExpanded());
        patientEdit.setGestationAtDelivery("3");
        // Checking more information button for gestation at delivery
        patientEdit.moreInfoGestation();
        Assert.assertTrue(patientEdit.checkMoreInfoGestation().contains("Numeric value in weeks"));
        patientEdit.closeMoreInfoGestation();
        patientEdit.setConceptionAfterFertilityMedication();
        patientEdit.setInVitroFertilization();
        patientEdit.setGestationalSurrogacy();
        patientEdit.setApgar1Score("6");
        patientEdit.setApgar5Score("8");
        patientEdit.setPrenatalAndPerinatalNotes("Test");
        patientEdit.setWeightIn3rdPercentile();
        patientEdit.setPrenatalGrowthPatternsOther("test");
        patientEdit.setPositvieFerricChlorideTestYes();
        patientEdit.setPrenatalDevelopmentOrBirthOther("test");
        patientEdit.setDysplasticTestesYes();

        // Setting details about the medical history of the patient
        patientEdit.expandMedicalHistory();
        // Checking medical history tab expanded
        Assert.assertEquals("Medical and developmental history:", patientEdit.checkMedicalHistoryExpanded());
        patientEdit.setMedicalAndDevelopmentalHistory("Test");
        patientEdit.setGlobalAgeOfOnset();
        // Checking more information button for global age of onset
        patientEdit.moreInfoGlobalAgeOfOnset();
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoGlobalAgeOfOnset()
                .contains(
                    "The onset for the majority of the symptoms. Individual phenotypes may be designated with different onsets than recorded here in the Clinical Symptoms section."));
        patientEdit.closeMoreInfoGlobalAgeOfOnset();

        // Filling in all the measurements of the patient.
        patientEdit.expandMeasurements();
        // Checking if measurements tab expanded.
        Assert.assertEquals("GROWTH CHARTS", patientEdit.checkIfMeasurementsExpanded());
        patientEdit.createNewMeasurementsEntry();
        patientEdit.setMeasurementWeight("3");
        patientEdit.setMeasurementHeight("3");
        patientEdit.setMeasurementArmSpan("3");
        patientEdit.setMeasurementSittingHeight("3");
        patientEdit.setMeasurementHeadCircumference("3");
        patientEdit.setMeasurementPhiltrumLength("3");
        patientEdit.setMeasurementLeftEarLength("3");
        patientEdit.setMeasurementRightEarLength("3");
        patientEdit.setMeasurementOuterCanthalDistance("3");
        patientEdit.setMeasurementInnerCanthalDistance("3");
        patientEdit.setMeasuremtnePalpebralFissureLength("3");
        patientEdit.setMeasurementInterpupilaryDistance("3");
        patientEdit.setMeasurementLeftHandLength("3");
        patientEdit.setMeasurementLeftFootLength("3");
        patientEdit.setMeasurementRightHandLength("3");
        patientEdit.setMeasurementRightPalmLength("3");
        patientEdit.setMeasurementRightFootLength("3");
        patientEdit.setMeasurementLeftPalmLength("3");
        Assert.assertTrue(patientEdit.getWeightPctl().contains("6"));
        Assert.assertTrue(patientEdit.getHeightPctl().contains("0"));
        Assert.assertTrue(patientEdit.getBMIPctl().contains("100"));
        Assert.assertTrue(patientEdit.getSittingPctl().contains("0"));
        Assert.assertTrue(patientEdit.getHeadCircumferencePctl().contains("0"));
        Assert.assertTrue(patientEdit.getPhiltrumPctl().contains("100"));
        Assert.assertTrue(patientEdit.getLeftEarPctl().contains("0"));
        Assert.assertTrue(patientEdit.getRightEarPctl().contains("0"));
        Assert.assertTrue(patientEdit.getOuterCanthalPctl().contains("0"));
        Assert.assertTrue(patientEdit.getInnerCanthalPctl().contains("100"));
        Assert.assertTrue(patientEdit.getPalpebralFissurePctl().contains("100"));
        Assert.assertTrue(patientEdit.getInterpupilaryPctl().contains("0"));
        Assert.assertTrue(patientEdit.getLeftPalmPctl().contains("3"));
        Assert.assertTrue(patientEdit.getLeftFootPctl().contains("0"));
        Assert.assertTrue(patientEdit.getRightPalmPctl().contains("3"));
        Assert.assertTrue(patientEdit.getRightFootPctl().contains("0"));
        Assert.assertTrue(patientEdit.getBMINumber().contains("3333.33"));

        patientEdit.moreInfoNewEntryMeasurements();
        Assert.assertTrue(patientEdit.checkMoreInfoNewEntryMeasurements().contains(
            "Create a list of patient measurements. These are dated, and a new set of measurements from a different date may be recorded"));
        patientEdit.closeMoreInfoNewEntryMeasurements();

        // Expanding growth charts tab and checking they appear
        // patientEdit.expandGrowthCharts();
        Assert.assertTrue(patientEdit.checkIfGrowthChartsAreShowing());
        Assert.assertTrue(patientEdit.checkIfGrowthChartsAreShowingByText().contains(
            "Weight for age, birth to 36 months, boys"));

        // Set genotype information for patient
        patientEdit.expandGenotypeInformation();
        Assert.assertEquals("Genotype information", patientEdit.checkIfGenotypeInformationExpanded());
        patientEdit.newEntryGenotypeInformation();
        patientEdit.setGenotypeInformationComments("test");
        patientEdit.setGenotypeInformationGene("test1");

        // Expanding clinical symptoms tab and checking it expanded
        patientEdit.expandClinicalSymptomsAndPhysicalFindings();
        Assert.assertTrue(patientEdit.checkIfClinicalSymptomsAndPhysicalFindingsExpanded(By
            .id("PhenoTips.PatientClass_0_unaffected")));

        // Filling in all the phenotypes of the patient
        patientEdit.moreInfoSelectObservedPhenotypes();
        Assert.assertTrue(patientEdit.checkMoreInfoSelectObservedPhenotypes().contains(
            "For each phenotype, choose the most specific term possible, based on the definition"));
        patientEdit.closeMoreInfoSelectObservedPhenotypes();
        Assert.assertTrue(patientEdit.checkShortStatureLightningBoltAppearsOnRight());
        Assert.assertTrue(patientEdit.checkHypotelorismLightningBoltAppearsOnRight());
        patientEdit.setWeight3rdPercentile();
        patientEdit.setHemihypertrophyYes();
        patientEdit.setCraniosynostosisYes();
        patientEdit.setCleftUpperLipNO();
        patientEdit.setCleftPalateYes();
        patientEdit.setAbnormalFacialShapeYes();
        patientEdit.setVisualImpairmentYes();
        patientEdit.setAbnormalityOfTheCorneaNO();
        patientEdit.setColobomaNO();
        patientEdit.setSensorineuralNO();
        patientEdit.setAbnormalityOfTheInnerEarYes();
        patientEdit.setHyperpigmentationOfTheSkinYes();
        patientEdit.setCapillaryHemangiomasNO();
        patientEdit.setVentricularSeptalDefectNO();
        patientEdit.setArrhythmiaYes();
        patientEdit.setCongenitalDiaphragmaticHerniaYes();
        patientEdit.setAbnormalityOfTheLungNO();
        patientEdit.setLowerLimbUndergrowthYes();
        patientEdit.setScoliosisYes();
        patientEdit.setAbnormalityOfTheVertebralColumnNO();
        patientEdit.setCholestasisYes();
        patientEdit.setDiabetesMellitusNO();
        patientEdit.setHorseshoeKidneyNO();
        patientEdit.setHypospadiasYes();
        patientEdit.setDelayedFineMotorDevelopmentYes();
        patientEdit.setAttentionDeficitHyperactivityDisorderNO();
        patientEdit.setAustismYes();
        patientEdit.setSeizuresYes();
        patientEdit.setSpinalDysraphismNO();
        patientEdit.setGastroschisisYes();
        // patientEdit.setHypotelorismYes();

        // Checking sidebars function correctly
        patientEdit.setPreauricularPitYes();
        Assert.assertTrue(patientEdit.checkPreauricularPitAppearsOnRight());
        patientEdit.setNystagmusNO();
        Assert.assertTrue(patientEdit.checkNystagmusAppearsOnRightNO());
        patientEdit.deleteNystagmusFromRight();
        Assert.assertTrue(patientEdit.checkNystagmusReturnsToNA());

        // Checking that the drop down menus for the phenotypes work
        patientEdit.coarctationOfAortaDropDown();
        Assert.assertTrue(patientEdit.checkCoarctationOfAortaDropDown().contains("Coarctation of abdominal aorta"));

        // Checking more info button functions correctly
        patientEdit.moreInfoHypotelorism();
        patientEdit.checkMoreInfoOpened();
        patientEdit.closeMoreInfoHypotelorism();

        // Adding details to different phenotypes in the current selection selection
        patientEdit.hemihypertrophyAddDetails();
        patientEdit.ageOfOnsetHemihypertrophy();
        patientEdit.setNeonatalOnsetHemihypertrophy();
        patientEdit.temporalPatternHemihypertrophy();
        patientEdit.setSubacuteTemporalPatternHemihypertrophy();
        patientEdit.deleteCleftPalate();
        patientEdit.abnormalFacialShapeAddDetails();
        patientEdit.paceOfProgressionAbnormalFacialShape();
        patientEdit.slowPaceOfProgressionAbnormalFacialShape();
        patientEdit.hypotelorismAddDetails();
        patientEdit.severityHypotelorism();
        patientEdit.moderateSeverityHypotelorism();
        patientEdit.abnormalityOfTheInnerEarAddDetails();
        patientEdit.spatialPatternAbnormalityOfTheInnerEar();
        patientEdit.distalSpatialPatternAbnomalityOfTheInnerEar();
        patientEdit.arrythmiaAddDetails();
        patientEdit.arrythmiaComments("test");
        patientEdit.scoliosisAddDetails();
        patientEdit.lateralityScoliosis();
        patientEdit.rightLateralityScoliosis();
        patientEdit.seizuresAddDetails();
        patientEdit.severitySeizures();
        patientEdit.mildSeveritySeizures();

        // FIXME I DON'T WORK
        // Adding new phenotypes from the "You may want to investigate" box
        // patientEdit.bifidTongueYes();
        // Assert.assertTrue(patientEdit.checkBifidTongueDissapearsFromRightInvestigateBox());

        // Adding a phenotype from the quick search bar
        patientEdit.phenotypeQuickSearch("Cataract");
        patientEdit.quickSearchCataractYes();

        // Checking the "browse related terms" under quick search bar functions correctly
        patientEdit.phenotypeQuickSearch("Cataract");
        patientEdit.browseRelatedTermsCataract();
        patientEdit.abnormalityOfTheLensGoUP();
        patientEdit.phacodonesisYes();
        patientEdit.abnormalityOfTheAnteriorSegmentOfTheEye();
        patientEdit.closeBrowseRelatedTerms();
        patientEdit.hideQuickSearchBarSuggestions();
        Assert.assertTrue(patientEdit.checkPhacodonesisAppearsOnRight());

        // Checking You many want to investigate tab hides
        patientEdit.hideYouMayWantToInvestigate();
        Assert.assertFalse(patientEdit.checkYouMayWantToInvestigateHid());
        patientEdit.hideYouMayWantToInvestigate();

        // Checking expand and collapse buttons function correctly

        // Expanding diagnois tab and checking it expanded
        patientEdit.expandDiagnosis();
        Assert.assertEquals("Additional comments:", patientEdit.checkDiagnosisExpaned());

        // setting diagnosis details about patient.
        patientEdit.setDiagnosisAdditionalComments("test");
        patientEdit.moreInfoOMIMDisorder();
        Assert
            .assertTrue(patientEdit
                .checkMoreInfoOMIMDisorder()
                .contains(
                    "Generally, a disorder is used to annotate the patient profile because it was either: tested and confirmed to be either present or absent OR suspected based on the manifestations (phenotypes), but not yet tested."));
        patientEdit.closeMoreInfoOMIMDisorder();
        patientEdit.excludeLowerLimbUndergrowth();
        Assert.assertEquals("Lower limb undergrowth", patientEdit.checkLowerLimbUndergrowthExcluded());
        patientEdit.setSmithLemliOptizSyndrome();
        patientEdit.setWatsonSyndrome();
        patientEdit.setOMIMDisorder("wolf");
        patientEdit.setOMIMDisorderWolfSyndrome();
        patientEdit.setCriDuChatSyndromeFromBottom();
        Assert.assertEquals("#270400 SMITH-LEMLI-OPITZ SYNDROME", patientEdit.checkSmithLemliInOMIM());
        Assert.assertEquals("#123450 CRI-DU-CHAT SYNDROME", patientEdit.checkCriDuChatAppears());
        patientEdit.setCriDuChatSyndromeFromBottom();
        Assert
            .assertFalse(patientEdit.checkCriDuChatDisappearsFromTop(By
                .xpath("//*[@class = 'fieldset omim_id']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']")));

        // FIXME I DON'T WORK

        // patientEdit.setCriDuChatSyndromeFromBottom();
        // patientEdit.setCriDuChatFromTop();
        // Assert.assertFalse(patientEdit.checkCriDuChatDisappearsFromBottom());
        // Assert
        // .assertFalse(patientEdit.checkCriDuChatDisappearsFromTop(By
        // .xpath("//*[@class = 'fieldset omim_id']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']")));

        Assert.assertTrue(patientEdit.checkSmithLemliSyndrome());

        // Saving the patient record and accesing the view patient page.
        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();

        // Checking patient details - name + gender + parental ethnicities
        Assert.assertEquals("Doe, John", patientView.getPatientName());
        // Assert.assertEquals("3", patientView.getPatientIdentifier());
        Assert.assertEquals("Male", patientView.getSex());
        List<String> maternalEthnicities = patientView.getMaternalEthnicities();
        Assert.assertEquals(1, maternalEthnicities.size());
        Assert.assertEquals("Arab", maternalEthnicities.get(0));
        Assert.assertEquals("test", patientView.getIndicationForReferral());

        // Checking Family history of patient
        Assert.assertEquals("This patient is the Child", patientView.checkPatientRelative());
        Assert.assertEquals("Of patient with identifier 2", patientView.checkRelativeOfPatient());
        Assert.assertEquals("Polygenic inheritance", patientView.getGlobalModeOfInheritance());
        Assert.assertEquals("Consanguinity", patientView.getConsanguinity());
        Assert.assertEquals("Autism, Dementia, Asthma", patientView.getFamilyConditions());

        // Checking prenatal and perinatal details of patient
        Assert.assertEquals("Conception after fertility medication",
            patientView.getConceptionAfterFertilityMedication());
        Assert.assertEquals("In vitro fertilization", patientView.getInVitroFertilization());
        Assert.assertEquals("Gestational surrogacy", patientView.getGestationalSurrogacy());
        Assert.assertEquals("6", patientView.getAPGARScore1Minute());
        Assert.assertEquals("8", patientView.getAPGARScore5Minutes());
        Assert.assertEquals("Test", patientView.getPrenatalAndPerinatalNotes());
        Assert.assertEquals("Test", patientView.getMedicalAndDevelopmentalHistory());
        Assert.assertEquals("Infantile onset", patientView.getGlobalAgeOfOnset());
        Assert.assertEquals("Positive ferric chloride test", patientView.getPositiveFerricChlorideTest());
        Assert.assertEquals("Dysplastic testes", patientView.getDysplasticTestes());

        // Checking measurements of patient
        Assert.assertTrue(patientView.getMeasurementsWeight().contains("3.0 kg"));
        Assert.assertTrue(patientView.getMeasurementsHeight().contains("3.0 cm"));
        Assert.assertTrue(patientView.getBMI().contains("3333.33"));
        Assert.assertTrue(patientView.getMeasurementsArmSpan().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsSittingHeight().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsHeadCircumference().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsPhiltrumLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsLeftEarLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsRightEarLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsOuterCanthalDistance().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsInnerCanthalDistance().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsPalpebralFissureLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementInterpupilaryDistance().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsLeftHandLength().contains("3.0 cm"));
        // Assert.assertTrue(patientView.getMeasurementsLeftPalmLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsLeftFootLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsRightHandLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsRightPalmLength().contains("3.0 cm"));
        Assert.assertTrue(patientView.getMeasurementsRightFootLength().contains("3.0 cm"));

        // Check genotype information - comments + gene
        Assert.assertEquals("test", patientView.getGenotypeInformationComments());
        Assert.assertEquals("test1", patientView.getGenotypeInformationGene());

        // Checking phenotypes of patient.
        Assert.assertTrue(patientView.hasPhenotypeSelected("Stature for age", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Weight for age", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Head circumference for age", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Hemihypertrophy", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Craniosynostosis", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Cleft upper lip", false));
        Assert.assertFalse(patientView.hasPhenotypeSelected("Cleft palate", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormal facial shape", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Visual impairment", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the cornea", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Coloboma", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Sensorineural", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the inner ear", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Hyperpigmentation of the skin", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Capillary hemangiomas", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Ventricular septal defect", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Arrhythmia", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Congenital diaphragmatic hernia", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the lung", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Lower limb undergrowth", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Scoliosis", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Abnormality of the vertebral column", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Cholestasis", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Diabetes mellitus", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Horseshoe kidney", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Hypospadias", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Delayed fine motor development", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Attention deficit hyperactivity disorder", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Autism", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Seizures", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Spinal dysraphism", false));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Phacodonesis", true));
        Assert.assertTrue(patientView.hasPhenotypeSelected("Cataract", true));
        // FIXME I DON'T WORK (Refer to line 274)
        // Assert.assertTrue(patientView.hasPhenotypeSelected("Bifid tongue", true));

        // Checking the details of the phenotypes
        Assert.assertEquals("test", patientView.getDiagnosisAdditionalComments());
        Assert.assertEquals("Neonatal onset", patientView.getNeonatalOnsetHemihypertrophy());
        Assert.assertEquals("Subacute", patientView.getSubacuteTemporalPatternHemihypertrophy());
        Assert.assertEquals("Slow progression", patientView.getSlowPaceOfProgressionAbnormalFacialShape());
        Assert.assertEquals("Moderate", patientView.getModerateSeverityHypotelorism());
        Assert.assertEquals("Distal", patientView.getDistalSpatialPatternAbnormalityOfTheInnerEar());
        Assert.assertEquals("test", patientView.getCommentsArrhythmia());
        Assert.assertEquals("Right", patientView.getRightLateralityScoliosis());
        Assert.assertEquals("Mild", patientView.getMildSeveritySeizures());

        // Checking the diagnosis is displaying correctly
        Assert
            .assertEquals(
                "#270400 SMITH-LEMLI-OPITZ SYNDROME; SLOS ;;SLO SYNDROME;; RSH SYNDROME;; RUTLEDGE LETHAL MULTIPLE CONGENITAL ANOMALY SYNDROME;; POLYDACTYLY, SEX REVERSAL, RENAL HYPOPLASIA, AND UNILOBAR LUNG;; LETHAL ACRODYSGENITAL SYNDROME",
                patientView.getSmithLemliOptizSyndrome());
        Assert
            .assertEquals(
                "#193520 WATSON SYNDROME ;;PULMONIC STENOSIS WITH CAFE-AU-LAIT SPOTS;; CAFE-AU-LAIT SPOTS WITH PULMONIC STENOSIS",
                patientView.getWatsonSyndrome());

        Assert
            .assertEquals(
                "#194190 WOLF-HIRSCHHORN SYNDROME; WHS ;;CHROMOSOME 4p16.3 DELETION SYNDROME;; PITT-ROGERS-DANKS SYNDROME; PRDS;; PITT SYNDROME",
                patientView.getWolfSyndrome());

        // Cheking the Edit button is working correctly
        patientView.goToEditPage();
        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        /* Checking that each tab's edit button is working correctly */

        // Checking patient information edit button
        patientView.hoverOverPatientInformation();
        patientView.goToEditPagePatientInfo();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        // Checking family history edit button
        patientView.hoverOverFamilyHistory();
        patientView.goToEditPageFamilyHistory();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        // Checking prenatal and perinatal edit button
        patientView.hoverOverPrenatalHistory();
        patientView.goToEditPagePrenatalHistory();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        // FIXME I DON'T WORK

        // Checking measurements edit button
        // patientView.hoverOverMeasurements();
        // patientView.goToEditPageMeasurements();

        // Assert
        // .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        // patientEdit.clickSaveAndView();
        // Assert.assertEquals("Doe, John", patientView.getPatientName());

        // Checking genotype information edit button
        patientView.hoverOverGenotypeInfo();
        patientView.goToEditPageGenotypeInfo();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        // Checking clinical symptoms edit button
        patientView.hoverOverClinicalSymptoms();
        patientView.goToEditPageClinicalSymptoms();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

        // Checking diagnosis edit button
        patientView.hoverOverDiagnosis();
        patientView.goToEditPageDiagnosis();

        Assert
            .assertTrue(patientView.checkIfWentToEditPage((By.id("PhenoTips.PatientClass_0_indication_for_referral"))));
        patientEdit.clickSaveAndView();
        Assert.assertEquals("Doe, John", patientView.getPatientName());

    }

    @Test
    public void patientIsClinicallyNormal()
    {
        // Creating a new patient and setting basic details: name + gender
        PatientRecordEditPage patientEdit = HomePage.gotoPage().clickNewPatientRecord();
        patientEdit.setPatientLastName("Doe");
        patientEdit.setPatientFirstName("Jane");
        patientEdit.setFemaleGender();

        patientEdit.expandClinicalSymptomsAndPhysicalFindings();
        // Setting patient to be clinically normal
        patientEdit.setPatientClinicallyNormal();

        // Saving the patient record and accesing the view patient page.
        PatientRecordViewPage patientView = patientEdit.clickSaveAndView();

        // Checking patient name, gender and clinically normal.
        Assert.assertEquals("Doe, Jane", patientView.getPatientName());
        Assert.assertEquals("Female", patientView.getSex());
        Assert.assertEquals("This patient is clinically normal", patientView.getPatientIsClinicallyNormal());

    }

    @Test
    public void testingMeasurements()
    {
        PatientRecordEditPage patientEdit = HomePage.gotoPage().clickNewPatientRecord();
        patientEdit.setPatientLastName("Smith");
        patientEdit.setPatientFirstName("Aiden");
        patientEdit.setMaleGender();
        patientEdit.expandMeasurements();
        patientEdit.createNewMeasurementsEntry();
        patientEdit.setMeasurementWeight("1");
        patientEdit.expandClinicalSymptomsAndPhysicalFindings();
        Assert.assertTrue(patientEdit.checkDecreasedBodyWeight());
        patientEdit.setMeasurementWeight("4");
        Assert.assertFalse(patientEdit.checkDecreasedBodyWeightDisappears());
        patientEdit.setMeasurementHeight("3");
        Assert.assertTrue(patientEdit.checkShortStature());
        patientEdit.setMeasurementHeight("53");
        Assert.assertFalse(patientEdit.checkShortStatureDisappears());
        Assert
            .assertTrue(patientEdit.checkChartTitleChangesMale().contains("Weight for age, birth to 36 months, boys"));
        patientEdit.setFemaleGender();
        Assert.assertTrue(patientEdit.checkChartTitleChangesFemale().contains(
            "Weight for age, birth to 36 months, girls"));
        patientEdit.setMaleGender();
        patientEdit.openDateOfBirth();
        patientEdit.setYearDateOfBirth("1998");
        patientEdit.setMonthDateOfBirth("August");
        patientEdit.setDate12();
        patientEdit.openDateOfMeasurements();
        patientEdit.setYearMeasurement("2014");
        patientEdit.setMonthMeasurement("August");
        patientEdit.setDate12();
        Assert.assertTrue(patientEdit.checkDecreasedBodyWeight());
        Assert.assertTrue(patientEdit.checkShortStature());
        Assert.assertEquals("16y", patientEdit.getAgeMeasurements());
        Assert
            .assertTrue(patientEdit.checkChartTitleChangesOlderMale().contains("Weight for age, 2 to 20 years, boys"));

    }
}
