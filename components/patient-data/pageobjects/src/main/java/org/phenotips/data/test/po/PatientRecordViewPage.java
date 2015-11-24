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
package org.phenotips.data.test.po;

import org.xwiki.test.ui.po.ViewPage;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on a patient record in view mode.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class PatientRecordViewPage extends ViewPage
{
    @FindBy(css = "#document-title h1")
    private WebElement recordId;

    @FindBy(id = "prActionEdit")
    private WebElement menuEditLink;

    @FindBy(xpath = "//span[text() = 'Patient name:']/following-sibling::span[@class = 'displayed-value']")
    private WebElement patientName;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[@class = 'displayed-value'][text() = '3']")
    private WebElement patientIdentifier;

    @FindBy(xpath = "//span[text() = 'Sex:']/following-sibling::span[@class = 'displayed-value']")
    private WebElement sex;

    @FindBy(xpath = "//*[contains(@class, 'maternal_ethnicity')]//*[@class = 'xwiki-free-multiselect']")
    private WebElement maternalEthnicity;

    @FindBy(xpath = "//*[contains(@class, 'global_mode_of_inheritance ')]//*[@class = 'displayed-value']")
    private WebElement globalModeOfInheritance;

    @FindBy(css = ".fieldset.consanguinity .displayed-value .yes-no-picker-label")
    private WebElement consanguinity;

    @FindBy(css = ".fieldset.family_history .displayed-value")
    private WebElement familyHealthConditions;

    @FindBy(css = ".fieldset.assistedReproduction_fertilityMeds .displayed-value")
    private WebElement conceptionAfterFertiliyMedication;

    @FindBy(css = ".fieldset.ivf .displayed-value")
    private WebElement inVitroFertilization;

    @FindBy(css = ".fieldset.assistedReproduction_surrogacy .displayed-value")
    private WebElement gestationalSurrogacy;

    @FindBy(css = ".fieldset.prenatal_development .displayed-value")
    private WebElement prenatalAndPerinatalHistoryNotes;

    @FindBy(css = ".fieldset.medical_history .displayed-value")
    private WebElement medicalAndDevelopmentalHistory;

    @FindBy(css = ".fieldset.global_age_of_onset .displayed-value")
    private WebElement globalAgeOfOnset;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Weight')]/following-sibling::td[1]")
    private WebElement measurementsWeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Height')]/following-sibling::td[1]")
    private WebElement measurementsHeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//*[@class = 'bmi displayed-value']")
    private WebElement BMI;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Arm span')]/following-sibling::td[1]")
    private WebElement measurementsArmSpan;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Sitting height')]/following-sibling::td[1]")
    private WebElement measurementsSittingHeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Head circumference')]/following-sibling::td[1]")
    private WebElement measurementsHeadCircumference;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Philtrum length')]/following-sibling::td[1]")
    private WebElement measurementsPhiltrumLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left ear length')]/following-sibling::td[1]")
    private WebElement measurementsLeftEarLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right ear length')]/following-sibling::td[1]")
    private WebElement measurementsRightEarLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Outer canthal distance')]/following-sibling::td[1]")
    private WebElement measurementsOuterCanthalDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Inner canthal distance')]/following-sibling::td[1]")
    private WebElement measurementsInnerCanthalDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Palpebral fissure length')]/following-sibling::td[1]")
    private WebElement measurementsPalpebralFissureLenghth;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Interpupilary distance')]/following-sibling::td[1]")
    private WebElement measurementsInterpupilaryDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left hand length')]/following-sibling::td[1]")
    private WebElement measurementsLeftHandLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left palm length')]/following-sibling::td[1]")
    private WebElement measurementsLeftPalmLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left foot length')]/following-sibling::td[1]")
    private WebElement measurementsLeftFootLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right hand length')]/following-sibling::td[1]")
    private WebElement measurementsRightHandLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right palm length')]/following-sibling::td[1]")
    private WebElement measurementsRightPalmLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right foot length')]/following-sibling::td[1]")
    private WebElement measurementsRightFootLength;

    @FindBy(xpath = "//div[contains(@class, 'phenotype-info')]//div[contains(@class,'controlled-group')]")
    private WebElement patientIsClinicallyNormal;

    @FindBy(css = ".fieldset.diagnosis_notes .displayed-value")
    private WebElement diagnosisAdditionalComments;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#270400 SMITH-LEMLI-OPITZ SYNDROME; SLOS ;;SLO SYNDROME;; RSH SYNDROME;; RUTLEDGE LETHAL MULTIPLE CONGENITAL ANOMALY SYNDROME;; POLYDACTYLY, SEX REVERSAL, RENAL HYPOPLASIA, AND UNILOBAR LUNG;; LETHAL ACRODYSGENITAL SYNDROME')]")
    private WebElement smithLemliOptizSyndrome;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#193520 WATSON SYNDROME ;;PULMONIC STENOSIS WITH CAFE-AU-LAIT SPOTS;; CAFE-AU-LAIT SPOTS WITH PULMONIC STENOSIS')]")
    private WebElement watsonSyndrome;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#194190 WOLF-HIRSCHHORN SYNDROME; WHS ;;CHROMOSOME 4p16.3 DELETION SYNDROME;; PITT-ROGERS-DANKS SYNDROME; PRDS;; PITT SYNDROME')]")
    private WebElement wolfSyndrome;

    @FindBy(xpath = "//*[contains(@class, 'genotype chapter')]//*[@class = 'comments'][text() = ' test']")
    private WebElement genotypeInformationComments;

    @FindBy(xpath = "//*[contains(@class, 'genotype chapter')]//*[@class = 'gene'][text() = ' test1']")
    private WebElement genotypeInformationGene;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//div[contains(@class, 'value-checked')][text() = 'Hemihypertrophy']//*[@class = 'phenotype-details']//dd[text() = 'Neonatal onset']")
    private WebElement neonatalOnsetHemihypertrophy;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//div[contains(@class, 'value-checked')][text() = 'Hemihypertrophy']//*[@class = 'phenotype-details']//dd[text() = 'Subacute']")
    private WebElement subacuteTemporalPatternHemihypertrophy;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Abnormal facial shape']//*[@class = 'phenotype-details']//dd[text() = 'Slow progression']")
    private WebElement slowPaceOfProgressionAbnormalFacialShape;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Hypotelorism']//*[@class = 'phenotype-details']//dd[text() = 'Moderate']")
    private WebElement moderateSeverityHypotelorism;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Abnormality of the inner ear']//*[@class = 'phenotype-details']//dd[text() = 'Distal']")
    private WebElement distalSpatialPatternAbnormalityOfTheInnerEar;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Arrhythmia']//*[@class = 'phenotype-details']//dd[text() = 'test']")
    private WebElement commentsArrythmia;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Scoliosis']//*[@class = 'phenotype-details']//dd[text() = 'Right']")
    private WebElement rightLateralityScoliosis;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Seizures']//*[@class = 'phenotype-details']//dd[text() = 'Mild']")
    private WebElement mildSeveritySeizures;

    @FindBy(css = ".fieldset.indication_for_referral .displayed-value")
    private WebElement indicationForReferral;

    @FindBy(xpath = "//*[contains(@class, 'yes-selected')][text() = 'Positive ferric chloride test']")
    private WebElement positiveFerricChlorideTest;

    @FindBy(xpath = "//*[contains(@class, 'yes-selected')][text() = 'Dysplastic testes']")
    private WebElement dysplasticTestes;

    @FindBy(id = "prActionEdit")
    private WebElement goToEditPage;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPagePatientInfo;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPageFamilyHistory;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPagePrenatalHistory;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'fa-pencil')]")
    private WebElement goToEditPageMeasurements;

    @FindBy(xpath = "//*[contains(@class, 'genotype')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPageGenotypeInfo;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPageClinicalSymptoms;

    @FindBy(xpath = "//*[contains(@class, 'diagnosis-info')]//*[contains(@class, 'action-edit')]")
    private WebElement goToEditPageDiagnosis;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[@class = 'displayed-value'][text() = '6']")
    private WebElement APGARScore1Minute;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[@class = 'displayed-value'][text() = '8']")
    private WebElement APGARScore5Minutes;

    @FindBy(xpath = "//*[contains(@class, 'relatives-info')]//*[@class = 'relative_type']")
    private WebElement checkPatientRelative;

    @FindBy(xpath = "//*[contains(@class, 'relatives-info')]//*[@class = 'relative_of']")
    private WebElement checkRelativeOfPatient;

    /* SUMMARY */

    /* CASE RESOLUTION */

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__pubmed_id > div:nth-child(1) > p > span")
    private WebElement summaryPubmedIDTitle;

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__gene_id > div:nth-child(1) > p > span")
    private WebElement summaryGeneIDTitle;

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__notes > div:nth-child(1) > p > span")
    private WebElement summaryResolutionNotesTitle;

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__pubmed_id > div:nth-child(2) > div")
    private WebElement summaryPubmedIDField;

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__gene_id > div:nth-child(2) > div")
    private WebElement summaryGeneIDField;

    @FindBy(css = ".case-resolution.chapter > div > div.controlled > div.fieldset.solved__notes > div:nth-child(2) > div")
    private WebElement summaryResolutionNotesField;

    @FindBy(css = ".case-resolution.chapter > div > div.fieldset.unaffected.controller > p")
    private WebElement summaryCaseResolution;

    /* PATIENT INFORMATION */

    // TODO: REFER TO ELEMENTS AS 1st, 2nd, 3rd.. and so on that don't have class names
    // identifier is not tested since it must be unique every time. The work around is to delete it every time

    @FindBy(css = ".patient-info.chapter > div:nth-child(3) > p > span.displayed-value")
    private WebElement summaryPatientNameField;

    @FindBy(xpath = "//div[contains(@class, 'patient-info')]//p[./span[text() = 'Date of birth:']]/span[contains(@class, 'displayed-value')]")
    private WebElement summaryDateOfBirthField;

    @FindBy(xpath = "//div[contains(@class, 'patient-info')]//p[./span[text() = 'Date of death:']]/span[contains(@class, 'displayed-value')]")
    private WebElement summaryDateOfDeathField;

    @FindBy(css = ".patient-info.chapter > p:nth-child(6) > span.displayed-value")
    private WebElement summarySexField;

    @FindBy(css = ".patient-info.chapter > div.fieldset.indication_for_referral > div")
    private WebElement summaryIndicationForReferralField;

    /* FAMILY HISTORY */

    @FindBy(css = "#extradata-list-PhenoTips\\2e RelativeClass > tbody > tr:nth-child(2) > td.relative_type")
    private WebElement fieldRelativeDescription;

    @FindBy(css = "#extradata-list-PhenoTips\\2e RelativeClass > tbody > tr:nth-child(2) > td.relative_of")
    private WebElement fieldRelativeOfPatientWithIdentifier;

    @FindBy(css = ".family-info.chapter > div.fieldset.ethnicity > div.half-width.maternal_ethnicity > div > ol > li")
    private WebElement fieldMaternalEthnicity;

    @FindBy(css = ".family-info.chapter > div.fieldset.ethnicity > div.half-width.paternal_ethnicity > div > ol > li")
    private WebElement fieldPaternalEthnicity;

    @FindBy(css = ".family-info.chapter > div.fieldset.family_history > div")
    private WebElement fieldHealthConditions;

    @FindBy(css = ".global_mode_of_inheritance > div")
    private WebElement fieldGlobalInheritence;

    @FindBy(css = "//*[@id=\"xwikicontent\"]/div[3]/div[4]/div/text()[1]")
    private WebElement fieldFirstGlobalInheritence;

    @FindBy(css = "//*[@id=\"xwikicontent\"]/div[3]/div[4]/div/text()[2]")
    private WebElement fieldSecondGlobalInheritence;

    @FindBy(css = "//*[@id=\"xwikicontent\"]/div[3]/div[4]/div/text()[3]")
    private WebElement fieldThirdGlobalInheritence;

    /* PRENATAL AND PERINATAL HISTORY */

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(2) > span.displayed-value")
    private WebElement fieldGestationAtDelivery;

    @FindBy(css = ".prenatal-info.chapter .maternal_age .displayed-value")
    private WebElement fieldMaternalAgeAtEDD;

    @FindBy(css = ".prenatal-info.chapter .paternal_age .displayed-value")
    private WebElement fieldPaternalAgeAtEDD;

    @FindBy(css = ".pregnancy_history > p:nth-of-type(2) > ")
    private WebElement fieldPregnancyHistoryGravida;

    @FindBy(css = ".assistedReproduction_fertilityMeds > div > label")
    private WebElement fieldConceptionAfterFertility;

    @FindBy(css = ".ivf > div > label")
    private WebElement fieldIVF;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(1) > span.displayed-value")
    private WebElement fieldPrenatalFirstField;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(2) > span.displayed-value")
    private WebElement fieldPrenatalSecondField;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(3) > span.displayed-value")
    private WebElement fieldPrenatalThirdField;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(1)")
    private WebElement fieldFirstAssistedReproduction;

    @FindBy(css = ".prenatal-info.chapter > div.fieldset.ivf > div > label")
    private WebElement fieldSecondAssistedReproduction;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(6) > span:nth-child(2)")
    private WebElement fieldAPGARScoreOneMinute;

    @FindBy(css = ".prenatal-info.chapter > p:nth-child(6) > span:nth-child(5)")
    private WebElement fieldAPGARScoreFiveMinutes;

    @FindBy(css = ".prenatal-info.chapter > div.fieldset.prenatal_development > div")
    private WebElement fieldPrenatalNotes;

    @FindBy(css = ".prenatal_phenotype-main.predefined-entries > div")
    private WebElement fieldPrematureBirth;

    public String getPrenatalAndPerinatalHistorySummary(String option)
    {
        switch (option) {
            case "fieldConceptionAfterFertility":
                return this.fieldConceptionAfterFertility.getText();
            case "fieldIVF":
                return this.fieldIVF.getText();
            case "fieldGestationAtDelivery":
                return this.fieldGestationAtDelivery.getText();
            case "fieldMaternalAgeAtEDD":
                return this.fieldMaternalAgeAtEDD.getText();
            case "fieldPaternalAgeAtEDD":
                return this.fieldPaternalAgeAtEDD.getText();
            case "fieldFirstAssistedReproduction":
                return this.fieldFirstGlobalInheritence.getText();
            case "fieldSecondAssistedReproduction":
                return this.fieldSecondAssistedReproduction.getText();
            case "fieldAPGARScoreOneMinute":
                return this.fieldAPGARScoreOneMinute.getText();
            case "fieldAPGARScoreFiveMinutes":
                return this.fieldAPGARScoreFiveMinutes.getText();
            case "fieldPrenatalNotes":
                return this.fieldPrenatalNotes.getText();
            case "fieldPrematureBirth":
                return this.fieldPrematureBirth.getText();
        }
        return "";
    }

    public String getFamilyHistorySummary(String option)
    {
        switch (option) {
            case "fieldRelativeDescription":
                return this.fieldRelativeDescription.getText();
            case "fieldRelativeOfPatientWithIdentifier":
                return this.fieldRelativeOfPatientWithIdentifier.getText();
            case "fieldMaternalEthnicity":
                return this.fieldMaternalEthnicity.getText();
            case "fieldPaternalEthnicity":
                return this.fieldPaternalEthnicity.getText();
            case "fieldHealthConditions":
                return this.fieldHealthConditions.getText();
            case "fieldGlobalInheritence":
                return this.fieldGlobalInheritence.getText();
            case "fieldFirstGlobalInheritence":
                return this.fieldFirstGlobalInheritence.getText();
            case "fieldSecondGlobalInheritence":
                return this.fieldSecondGlobalInheritence.getText();
            case "fieldThirdGlobalInheritence":
                return this.fieldThirdGlobalInheritence.getText();
        }
        return "";
    }

    public String getPatientInformationSummary(String option)
    {
        switch (option) {
            case "fieldPatientName":
                return this.summaryPatientNameField.getText();
            case "fieldDateOfBirth":
                return this.summaryDateOfBirthField.getText();
            case "fieldDateOfDeath":
                return this.summaryDateOfDeathField.getText();
            case "fieldSex":
                return this.summarySexField.getText();
            case "fieldIndicationForReferral":
                return this.summaryIndicationForReferralField.getText();
        }
        return "";
    }

    public String getCaseResolutionSummary(String option)
    {
        switch (option) {
            case "titlePubmed":
                return this.summaryPubmedIDTitle.getText();
            case "fieldPubmed":
                return this.summaryPubmedIDField.getText();
            case "titleGeneID":
                return this.summaryGeneIDTitle.getText();
            case "fieldGeneID":
                return this.summaryGeneIDField.getText();
            case "titleResolutionNotes":
                return this.summaryResolutionNotesTitle.getText();
            case "fieldResolutionNotes":
                return this.summaryResolutionNotesField.getText();
            case "caseResolution":
                return this.summaryCaseResolution.getText();
        }
        return "";
    }

    ///////////////////
    public static PatientRecordViewPage gotoPage(String patientId)
    {
        getUtil().gotoPage("data", patientId, "view");
        return new PatientRecordViewPage();
    }

    public String getPatientRecordId()
    {
        this.getDriver().waitUntilElementIsVisible(By.cssSelector("#document-title h1"));
        return this.recordId.getText();
    }

    public PatientRecordEditPage clickEdit()
    {
        this.menuEditLink.click();
        return new PatientRecordEditPage();
    }

    public String getPatientName()
    {

        this.getDriver().waitUntilElementIsVisible(By
            .xpath("//span[text() = 'Patient name:']/following-sibling::span[@class = 'displayed-value']"));
        return this.patientName.getText();

    }

    public String getSex()
    {
        return this.sex.getText();
    }

    public String getPatientIdentifier()
    {
        return this.patientIdentifier.getText();
    }

    public List<String> getMaternalEthnicities()
    {
        List<String> result = new ArrayList<>();
        for (WebElement item : this.maternalEthnicity.findElements(By.tagName("li"))) {
            result.add(item.getText());
        }
        return result;

    }

    public String getGlobalModeOfInheritance()
    {
        return this.globalModeOfInheritance.getText();
    }

    public String getConsanguinity()
    {
        return this.consanguinity.getText();
    }

    public String getFamilyConditions()
    {
        return this.familyHealthConditions.getText();
    }

    public String getConceptionAfterFertilityMedication()
    {
        return this.conceptionAfterFertiliyMedication.getText();
    }

    public String getInVitroFertilization()
    {
        return this.inVitroFertilization.getText();
    }

    public String getGestationalSurrogacy()
    {
        return this.gestationalSurrogacy.getText();
    }

    public String getPrenatalAndPerinatalNotes()
    {
        return this.prenatalAndPerinatalHistoryNotes.getText();
    }

    public String getMedicalAndDevelopmentalHistory()
    {
        return this.medicalAndDevelopmentalHistory.getText();
    }

    public String getGlobalAgeOfOnset()
    {
        return this.globalAgeOfOnset.getText();
    }

    public String getMeasurementsWeight()
    {
        return this.measurementsWeight.getText();
    }

    public String getMeasurementsHeight()
    {
        return this.measurementsHeight.getText();
    }

    public String getBMI()
    {
        return this.BMI.getText();
    }

    public String getMeasurementsArmSpan()
    {
        return this.measurementsArmSpan.getText();
    }

    public String getMeasurementsSittingHeight()
    {
        return this.measurementsSittingHeight.getText();
    }

    public String getMeasurementsHeadCircumference()
    {
        return this.measurementsHeadCircumference.getText();
    }

    public String getMeasurementsPhiltrumLength()
    {
        return this.measurementsPhiltrumLength.getText();
    }

    public String getMeasurementsLeftEarLength()
    {
        return this.measurementsLeftEarLength.getText();
    }

    public String getMeasurementsRightEarLength()
    {
        return this.measurementsRightEarLength.getText();
    }

    public String getMeasurementsOuterCanthalDistance()
    {
        return this.measurementsOuterCanthalDistance.getText();
    }

    public String getMeasurementsInnerCanthalDistance()
    {
        return this.measurementsInnerCanthalDistance.getText();
    }

    public String getMeasurementsPalpebralFissureLength()
    {
        return this.measurementsPalpebralFissureLenghth.getText();
    }

    public String getMeasurementInterpupilaryDistance()
    {
        return this.measurementsInterpupilaryDistance.getText();
    }

    public String getMeasurementsLeftHandLength()
    {
        return this.measurementsLeftHandLength.getText();
    }

    public String getMeasurementsLeftPalmLength()
    {
        return this.measurementsLeftPalmLength.getText();
    }

    public String getMeasurementsLeftFootLength()
    {
        return this.measurementsLeftFootLength.getText();
    }

    public String getMeasurementsRightHandLength()
    {
        return this.measurementsRightHandLength.getText();
    }

    public String getMeasurementsRightPalmLength()
    {
        return this.measurementsRightPalmLength.getText();
    }

    public String getMeasurementsRightFootLength()
    {
        return this.measurementsRightFootLength.getText();
    }

    public String getGenotypeInformationComments()
    {
        return this.genotypeInformationComments.getText();
    }

    public String getGenotypeInformationGene()
    {
        return this.genotypeInformationGene.getText();
    }

    public String getPatientIsClinicallyNormal()
    {
        return this.patientIsClinicallyNormal.getText();
    }

    public boolean hasPhenotypeSelected(String label, boolean positive)
    {
        return getUtil().hasElement(
            By.xpath("//*[contains(@class, 'phenotype-info')]//*[contains(text(),'" + (positive ? "" : "NO ") + label
                + "')]"));

    }

    public String getDiagnosisAdditionalComments()
    {
        return this.diagnosisAdditionalComments.getText();
    }

    public String getNeonatalOnsetHemihypertrophy()
    {
        return this.neonatalOnsetHemihypertrophy.getText();
    }

    public String getSubacuteTemporalPatternHemihypertrophy()
    {
        return this.subacuteTemporalPatternHemihypertrophy.getText();
    }

    public String getSlowPaceOfProgressionAbnormalFacialShape()
    {
        return this.slowPaceOfProgressionAbnormalFacialShape.getText();
    }

    public String getModerateSeverityHypotelorism()
    {
        return this.moderateSeverityHypotelorism.getText();
    }

    public String getDistalSpatialPatternAbnormalityOfTheInnerEar()
    {
        return this.distalSpatialPatternAbnormalityOfTheInnerEar.getText();
    }

    public String getCommentsArrhythmia()
    {
        return this.commentsArrythmia.getText();
    }

    public String getRightLateralityScoliosis()
    {
        return this.rightLateralityScoliosis.getText();
    }

    public String getMildSeveritySeizures()
    {
        return this.mildSeveritySeizures.getText();
    }

    public String getSmithLemliOptizSyndrome()
    {
        return this.smithLemliOptizSyndrome.getText();
    }

    public String getWatsonSyndrome()
    {
        return this.watsonSyndrome.getText();
    }

    public String getWolfSyndrome()
    {
        return this.wolfSyndrome.getText();
    }

    public String getIndicationForReferral()
    {
        return this.indicationForReferral.getText();
    }

    public String getPositiveFerricChlorideTest()
    {
        return this.positiveFerricChlorideTest.getText();
    }

    public String getDysplasticTestes()
    {
        return this.dysplasticTestes.getText();
    }

    public void goToEditPage()
    {
        this.goToEditPage.click();
    }

    public boolean checkIfWentToEditPage(By by)
    {
        this.getDriver().waitUntilElementIsVisible(By.id("PhenoTips.PatientClass_0_indication_for_referral"));
        try {
            getDriver().findElement(By.id("PhenoTips.PatientClass_0_indication_for_referral"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void hoverOverPatientInformation()
    {
        new Actions(getDriver()).moveToElement(
            getDriver().findElement(By.xpath("//*[contains(@class, 'patient-info')]"))).perform();
    }

    public void goToEditPagePatientInfo()
    {

        this.goToEditPagePatientInfo.click();
    }

    public void hoverOverFamilyHistory()
    {
        new Actions(getDriver()).moveToElement(getDriver().findElement(By.id("HFamilyhistory"))).perform();
    }

    public void goToEditPageFamilyHistory()
    {

        this.goToEditPageFamilyHistory.click();
    }

    public void hoverOverPrenatalHistory()
    {
        new Actions(getDriver()).moveToElement(getDriver().findElement(By.id("HPrenatalandperinatalhistory")))
            .perform();
    }

    public void goToEditPagePrenatalHistory()
    {

        this.goToEditPagePrenatalHistory.click();
    }

    public void hoverOverMeasurements()
    {
        new Actions(getDriver()).moveToElement(
            getDriver().findElement(By.xpath("//*[contains(@class, 'measurement-info')]"))).perform();
    }

    public void goToEditPageMeasurements()
    {

        this.goToEditPageMeasurements.click();
    }

    public void hoverOverGenotypeInfo()
    {
        new Actions(getDriver()).moveToElement(getDriver().findElement(By.id("HGenotypeinformation"))).perform();
    }

    public void goToEditPageGenotypeInfo()
    {

        this.goToEditPageGenotypeInfo.click();
    }

    public void hoverOverClinicalSymptoms()
    {
        new Actions(getDriver()).moveToElement(getDriver().findElement(By.id("HClinicalsymptomsandphysicalfindings")))
            .perform();
    }

    public void goToEditPageClinicalSymptoms()
    {

        this.goToEditPageClinicalSymptoms.click();
    }

    public void hoverOverDiagnosis()
    {
        new Actions(getDriver()).moveToElement(getDriver().findElement(By.id("HDiagnosis"))).perform();
    }

    public void goToEditPageDiagnosis()
    {

        this.goToEditPageDiagnosis.click();
    }

    public String getAPGARScore1Minute()
    {
        return this.APGARScore1Minute.getText();
    }

    public String getAPGARScore5Minutes()
    {
        return this.APGARScore5Minutes.getText();
    }

    public String checkPatientRelative()
    {
        return this.checkPatientRelative.getText();
    }

    public String checkRelativeOfPatient()
    {
        return this.checkRelativeOfPatient.getText();
    }

    @Override
    public void waitUntilPageJSIsLoaded()
    {
        getDriver().waitUntilJavascriptCondition("return window.Prototype != null && window.Prototype.Version != null");
    }
}
