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
    WebElement recordId;

    @FindBy(id = "prActionEdit")
    WebElement menuEditLink;

    @FindBy(xpath = "//span[text() = 'Patient name:']/following-sibling::span[@class = 'displayed-value']")
    WebElement patientName;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[@class = 'displayed-value'][text() = '3']")
    WebElement patientIdentifier;

    @FindBy(xpath = "//span[text() = 'Sex:']/following-sibling::span[@class = 'displayed-value']")
    WebElement sex;

    @FindBy(xpath = "//*[contains(@class, 'maternal_ethnicity')]//*[@class = 'xwiki-free-multiselect']")
    WebElement maternalEthnicity;

    @FindBy(xpath = "//*[contains(@class, 'global_mode_of_inheritance ')]//*[@class = 'displayed-value']")
    WebElement globalModeOfInheritance;

    @FindBy(css = ".fieldset.consanguinity .displayed-value .yes-no-picker-label")
    WebElement consanguinity;

    @FindBy(css = ".fieldset.family_history .displayed-value")
    WebElement familyHealthConditions;

    @FindBy(css = ".fieldset.assistedReproduction_fertilityMeds .displayed-value")
    WebElement conceptionAfterFertiliyMedication;

    @FindBy(css = ".fieldset.ivf .displayed-value")
    WebElement inVitroFertilization;

    @FindBy(css = ".fieldset.assistedReproduction_surrogacy .displayed-value")
    WebElement gestationalSurrogacy;

    @FindBy(css = ".fieldset.prenatal_development .displayed-value")
    WebElement prenatalAndPerinatalHistoryNotes;

    @FindBy(css = ".fieldset.medical_history .displayed-value")
    WebElement medicalAndDevelopmentalHistory;

    @FindBy(css = ".fieldset.global_age_of_onset .displayed-value")
    WebElement globalAgeOfOnset;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Weight')]/following-sibling::td[1]")
    WebElement measurementsWeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Height')]/following-sibling::td[1]")
    WebElement measurementsHeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//*[@class = 'bmi displayed-value']")
    WebElement BMI;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Arm span')]/following-sibling::td[1]")
    WebElement measurementsArmSpan;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Sitting height')]/following-sibling::td[1]")
    WebElement measurementsSittingHeight;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Head circumference')]/following-sibling::td[1]")
    WebElement measurementsHeadCircumference;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Philtrum length')]/following-sibling::td[1]")
    WebElement measurementsPhiltrumLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left ear length')]/following-sibling::td[1]")
    WebElement measurementsLeftEarLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right ear length')]/following-sibling::td[1]")
    WebElement measurementsRightEarLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Outer canthal distance')]/following-sibling::td[1]")
    WebElement measurementsOuterCanthalDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Inner canthal distance')]/following-sibling::td[1]")
    WebElement measurementsInnerCanthalDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Palpebral fissure length')]/following-sibling::td[1]")
    WebElement measurementsPalpebralFissureLenghth;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Interpupilary distance')]/following-sibling::td[1]")
    WebElement measurementsInterpupilaryDistance;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left hand length')]/following-sibling::td[1]")
    WebElement measurementsLeftHandLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left palm length')]/following-sibling::td[1]")
    WebElement measurementsLeftPalmLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Left foot length')]/following-sibling::td[1]")
    WebElement measurementsLeftFootLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right hand length')]/following-sibling::td[1]")
    WebElement measurementsRightHandLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right palm length')]/following-sibling::td[1]")
    WebElement measurementsRightPalmLength;

    @FindBy(xpath = "//div[contains(@class, 'measurement-info')]//th[contains(text(), 'Right foot length')]/following-sibling::td[1]")
    WebElement measurementsRightFootLength;

    @FindBy(xpath = "//div[contains(@class, 'phenotype-info')]//div[contains(@class,'controlled-group')]")
    WebElement patientIsClinicallyNormal;

    @FindBy(css = ".fieldset.diagnosis_notes .displayed-value")
    WebElement diagnosisAdditionalComments;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#270400 SMITH-LEMLI-OPITZ SYNDROME; SLOS ;;SLO SYNDROME;; RSH SYNDROME;; RUTLEDGE LETHAL MULTIPLE CONGENITAL ANOMALY SYNDROME;; POLYDACTYLY, SEX REVERSAL, RENAL HYPOPLASIA, AND UNILOBAR LUNG;; LETHAL ACRODYSGENITAL SYNDROME')]")
    WebElement smithLemliOptizSyndrome;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#193520 WATSON SYNDROME ;;PULMONIC STENOSIS WITH CAFE-AU-LAIT SPOTS;; CAFE-AU-LAIT SPOTS WITH PULMONIC STENOSIS')]")
    WebElement watsonSyndrome;

    @FindBy(xpath = "//div[contains(@class, 'diagnosis-info')]//p[contains(text(), '#194190 WOLF-HIRSCHHORN SYNDROME; WHS ;;CHROMOSOME 4p16.3 DELETION SYNDROME;; PITT-ROGERS-DANKS SYNDROME; PRDS;; PITT SYNDROME')]")
    WebElement wolfSyndrome;

    @FindBy(xpath = "//*[contains(@class, 'genotype chapter')]//*[@class = 'comments'][text() = ' test']")
    WebElement genotypeInformationComments;

    @FindBy(xpath = "//*[contains(@class, 'genotype chapter')]//*[@class = 'gene'][text() = ' test1']")
    WebElement genotypeInformationGene;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//div[contains(@class, 'value-checked')][text() = 'Hemihypertrophy']//*[@class = 'phenotype-details']//dd[text() = 'Neonatal onset']")
    WebElement neonatalOnsetHemihypertrophy;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//div[contains(@class, 'value-checked')][text() = 'Hemihypertrophy']//*[@class = 'phenotype-details']//dd[text() = 'Subacute']")
    WebElement subacuteTemporalPatternHemihypertrophy;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Abnormal facial shape']//*[@class = 'phenotype-details']//dd[text() = 'Slow progression']")
    WebElement slowPaceOfProgressionAbnormalFacialShape;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Hypotelorism']//*[@class = 'phenotype-details']//dd[text() = 'Moderate']")
    WebElement moderateSeverityHypotelorism;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Abnormality of the inner ear']//*[@class = 'phenotype-details']//dd[text() = 'Distal']")
    WebElement distalSpatialPatternAbnormalityOfTheInnerEar;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Arrhythmia']//*[@class = 'phenotype-details']//dd[text() = 'test']")
    WebElement commentsArrythmia;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Scoliosis']//*[@class = 'phenotype-details']//dd[text() = 'Right']")
    WebElement rightLateralityScoliosis;

    @FindBy(xpath = "//*[contains(@class, 'value-checked')][text() = 'Seizures']//*[@class = 'phenotype-details']//dd[text() = 'Mild']")
    WebElement mildSeveritySeizures;

    @FindBy(css = ".fieldset.indication_for_referral .displayed-value")
    WebElement indicationForReferral;

    @FindBy(xpath = "//*[contains(@class, 'yes-selected')][text() = 'Positive ferric chloride test']")
    WebElement positiveFerricChlorideTest;

    @FindBy(xpath = "//*[contains(@class, 'yes-selected')][text() = 'Dysplastic testes']")
    WebElement dysplasticTestes;

    @FindBy(id = "prActionEdit")
    WebElement goToEditPage;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPagePatientInfo;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPageFamilyHistory;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPagePrenatalHistory;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'fa-pencil')]")
    WebElement goToEditPageMeasurements;

    @FindBy(xpath = "//*[contains(@class, 'genotype')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPageGenotypeInfo;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPageClinicalSymptoms;

    @FindBy(xpath = "//*[contains(@class, 'diagnosis-info')]//*[contains(@class, 'action-edit')]")
    WebElement goToEditPageDiagnosis;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[@class = 'displayed-value'][text() = '6']")
    WebElement APGARScore1Minute;

    @FindBy(xpath = "//*[contains(@class, 'prenatal-info')]//*[@class = 'displayed-value'][text() = '8']")
    WebElement APGARScore5Minutes;

    @FindBy(xpath = "//*[contains(@class, 'relatives-info')]//*[@class = 'relative_type']")
    WebElement checkPatientRelative;

    @FindBy(xpath = "//*[contains(@class, 'relatives-info')]//*[@class = 'relative_of']")
    WebElement checkRelativeOfPatient;

    public static PatientRecordViewPage gotoPage(String patientId)
    {
        getUtil().gotoPage("data", patientId, "view");
        return new PatientRecordViewPage();
    }

    public String getPatientRecordId()
    {
        this.waitUntilElementIsVisible(By.cssSelector("#document-title h1"));
        return this.recordId.getText();
    }

    public PatientRecordEditPage clickEdit()
    {
        this.menuEditLink.click();
        return new PatientRecordEditPage();
    }

    public String getPatientName()
    {

        this.waitUntilElementIsVisible(By
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
        this.waitUntilElementIsVisible(By.id("PhenoTips.PatientClass_0_indication_for_referral"));
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

}
