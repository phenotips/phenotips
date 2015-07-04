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

import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.InlinePage;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents the actions possible on a patient record in view mode.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public class PatientRecordEditPage extends InlinePage
{
    @FindBy(id = "HFamilyhistoryandpedigree")
    WebElement expandFamilyHistory;

    @FindBy(id = "PhenoTips.PatientClass_0_maternal_ethnicity_2")
    WebElement maternalEthnicity;

    @FindBy(id = "PhenoTips.PatientClass_0_paternal_ethnicity_2")
    WebElement paternalEthnicity;

    @FindBy(id = "HPrenatalandperinatalhistory")
    WebElement expandPrenatalAndPerinatalHistory;

    @FindBy(id = "PhenoTips.PatientClass_0_family_history")
    WebElement familyHealthConditions;

    @FindBy(css = ".fieldset.gestation > div:nth-child(2) > div > div > div.controlled > input[type=\"text\"]")
    WebElement gestationAtDelivery;

    @FindBy(id = "HMedicalhistory")
    WebElement expandMedicalHistory;

    @FindBy(id = "HMeasurements")
    WebElement expandMeasurements;

    @FindBy(css = ".measurement-info.chapter > div.list-actions > p > span.buttonwrapper > a")
    WebElement newEntryMeasurements;

    /* MEASUREMENT ELEMENTS */

    @FindBy(id = "PhenoTips.MeasurementsClass_0_weight")
    WebElement measurementWeight;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_height")
    WebElement measurementHeight;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_armspan")
    WebElement measurementArmSpan;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_sitting")
    WebElement measurementSittingHeight;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_hc")
    WebElement measurementHeadCircumference;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_philtrum")
    WebElement measurementPhiltrumLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_ear")
    WebElement measurementLeftEarLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_ear_right")
    WebElement measurementRightEarLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_ocd")
    WebElement measurementOuterCanthalDistance;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_icd")
    WebElement measurementInnerCanthalDistance;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_pfl")
    WebElement measurementPalpebralFissureLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_ipd")
    WebElement measurementInterpupilaryDistance;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_hand")
    WebElement measurementLeftHandLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_palm")
    WebElement measurementLeftPalmLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_foot")
    WebElement measurementLeftFootLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_hand_right")
    WebElement measurementRightHandLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_palm_right")
    WebElement measurementRightPalmLength;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_foot_right")
    WebElement measurementRightFootLength;

    @FindBy(id = "HGenotypeinformation")
    WebElement expandGenotypeInformation;

    @FindBy(id = "PhenoTips.PatientClass_0_unaffected")
    WebElement patientIsClinicallyNormal;

    @FindBy(id = "HDiagnosis")
    WebElement expandDiagnosis;
    //

    @FindBy(id = "PhenoTips.PatientClass_0_diagnosis_notes")
    WebElement diagnosisAdditionalComments;

    @FindBy(id = "result__270400")
    WebElement smithLemliOptizSyndrome;

    @FindBy(id = "result__193520")
    WebElement watsonSyndrome;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001528']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement hemihypertrophyAddDetails;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001528']]//*[contains(@class, 'age_of_onset')]//*[contains(@class, 'collapse-button')][text() = '►']")
    WebElement ageOfOnsetHemihypertrophy;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001528']]//dd[@class = 'age_of_onset']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0003623']]//*[@value = 'HP:0003623']")
    WebElement neonatalOnsetHemihypertrophy;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001528']]//*[contains(@class, 'temporal_pattern')]//*[contains(@class, 'collapse-button')][text() = '►']")
    WebElement temporalPatternHemihypertrophy;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001528']]//dd[@class = 'temporal_pattern']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0011011']]//*[@value = 'HP:0011011']")
    WebElement subacuteTemporalPatternHemihypertrophy;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0000175']]//*[@class = 'tool'][text() = 'Delete']")
    WebElement deleteCleftPalate;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001999']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement abnormalFacialShapeAddDetails;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0001999']]//*[contains(@class, 'pace_of_progression')]//*[contains(@class, 'collapse-button')][text() = '►']")
    WebElement paceOfProgressionAbnormalFacialShape;

    @FindBy(id = "PhenoTips.PhenotypeMetaClass_1_pace_of_progression_HP:0003677")
    WebElement slowPaceOfProgressionAbnormalFacialShape;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0000601']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement hypotelorismAddDetails;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0000601']]//*[contains(@class, 'severity')]//*[@class = 'collapse-button'][text() = '►']")
    WebElement severityHypotelorism;

    @FindBy(xpath = "//*[@class = 'summary-item'][.//input[@value = 'HP:0000601']]//dd[@class = 'severity']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0012826']]//*[@value = 'HP:0012826']")
    WebElement moderateSeverityHypotelorism;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0000359']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement abnormalityOfTheInnerEarAddDetails;

    @FindBy(xpath = "//*[@class = 'group-contents'][.//input[@value = 'HP:0000359']]//*[contains(@class, 'spatial_pattern')]//*[@class = 'collapse-button'][text() = '►']")
    WebElement spatialPatternAbnormalityOfTheInnerEar;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0000359']]//dd[@class = 'spatial_pattern']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0012839']]//*[@value = 'HP:0012839']")
    WebElement distalSpatialPatternAbnormalityOfTheInnerEar;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0011675']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement arrhythmiaAddDetails;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0011675']]//dd[@class = 'comments']//textarea[contains(@name, 'PhenoTips.PhenotypeMetaClass')]")
    WebElement arrhythmiaComments;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0002650']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement scoliosisAddDetails;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0002650']]//*[contains(@class, 'laterality')]//*[@class = 'collapse-button'][text() = '►']")
    WebElement lateralityScoliosis;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0002650']]//dd[@class = 'laterality']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0012834']]//*[@value = 'HP:0012834']")
    WebElement rightLateralityScoliosis;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0001250']]//*[@class = 'tool'][text() = 'Add details']")
    WebElement seizuresAddDetails;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0001250']]//*[contains(@class, 'severity')]//*[@class = 'collapse-button'][text() = '►']")
    WebElement severitySeizures;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0001250']]//dd[@class = 'severity']//li[contains(@class, 'term-entry')][.//input[@value = 'HP:0012825']]//*[@value = 'HP:0012825']")
    WebElement mildSeveritySeizures;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0010722']]")
    WebElement asymmetryOfTheEarsYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0002721']]")
    WebElement immunodeficiencyYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0005344']]")
    WebElement abnormalityOfTheCartoidArteriesYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//label[@class = 'yes'][.//input[@value = 'HP:0010297']]")
    WebElement bifidTongueYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//*[contains(@class, 'yes')][.//input[@value = 'HP:0002591']]")
    WebElement polyphagiaYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//*[contains(@class, 'yes')][.//input[@value = 'HP:0000892']]")
    WebElement bifidRibsYes;

    @FindBy(xpath = "//*[contains(@class, 'background-search')]//*[contains(@class, 'no')][.//input[@value = 'HP:0002521']]")
    WebElement hypsarrhythmiaNO;

    @FindBy(xpath = "//*[contains(@class, 'term-entry')][.//label[text() = 'Coarctation of aorta']]/span[@class = 'expand-tool']")
    WebElement coarctationOfAortaDropDown;

    @FindBy(xpath = "//*[contains(@class, 'entry')]//*[contains(@class, 'info')][.//*[text() = 'Coarctation of abdominal aorta']]//*[@class = 'value']")
    WebElement checkCoarctationOfAortaDropDown;

    @FindBy(id = "quick-phenotype-search")
    WebElement phenotypeQuickSearch;

    @FindBy(xpath = "//*[@class = 'resultContainer']//*[@class = 'yes'][//input[@value = 'HP:0000518']]")
    WebElement quickSearchCataractYes;

    @FindBy(id = "PhenoTips.PatientClass_0_indication_for_referral")
    WebElement indicationForReferral;

    @FindBy(xpath = "//*[@class = 'prenatal_phenotype-other custom-entries'][//*[contains(@class, 'suggested')]]//*[contains(@class, 'suggested')]")
    WebElement prenatalGrowthPatternsOther;

    @FindBy(xpath = "//*[@class = 'resultContainer']//*[@class = 'yes'][//input[@value = 'HP:0003612']]")
    WebElement positiveFerricChlorideTestYes;

    @FindBy(xpath = "//*[@class = 'prenatal_phenotype-group'][.//*[@id = 'HPrenatal-development-or-birth']]//*[contains(@class, 'prenatal_phenotype-other')][//*[contains(@class, 'suggested')]]//*[contains(@class, 'suggested')]")
    WebElement prenatalDevelopmentOrBirthOther;

    @FindBy(xpath = "//*[@class = 'resultContainer']//*[@class = 'yes'][.//input[@value = 'HP:0008733']]")
    WebElement dysplasticTestesYes;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'weight_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement weightPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'height_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement heightPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'bmi_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement bmiPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'sitting_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement sittingPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'hc_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement headCircumferencePctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'philtrum_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement philtrumPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'ear_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement leftEarPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'ear_right_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement rightEarPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'ocd_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement outerCanthalPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'icd_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement innerCanthalPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'pfl_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement palpebralFissurePctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'ipd_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement interpupilaryPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'palm_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement leftPalmPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'foot_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement leftFootPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'palm_right_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement rightPalmPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'foot_right_evaluation']//*[contains(@class, 'displayed-value')]")
    WebElement rightFootPctl;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'bmi']//*[contains(@class, 'displayed-value')]")
    WebElement bmiNumber;

    @FindBy(id = "PhenoTips.PatientClass_0_omim_id")
    WebElement OMIMDisorderBar;

    @FindBy(xpath = "//*[@class = 'resultContainer']//*[@class = 'suggestValue'][text() = '#194190 WOLF-HIRSCHHORN SYNDROME']")
    WebElement OMIMDisorderWolfSyndrome;

    @FindBy(xpath = "//*[@class = 'ncbi-search-box']//*[@id = 'defaultSearchTerms']//*[@class = 'search-term symptom'][text() = 'Lower limb undergrowth']")
    WebElement OMIMBoxLowerLimbUnderGrowth;

    @FindBy(xpath = "//*[@class = 'ncbi-search-box']//*[@id = 'defaultSearchTerms']//*[@class = 'search-term symptom disabled'][text() = 'Lower limb undergrowth']")
    WebElement OMIMBoxLowerLimbUnderGrowthExcluded;

    @FindBy(xpath = "//*[@class = 'fieldset omim_id ']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][.//input[@value = '270400']]//*[@class = 'value'][text()='#270400 SMITH-LEMLI-OPITZ SYNDROME']")
    WebElement checkSmithLemliInOMIM;

    @FindBy(id = "result__123450")
    WebElement criDuChatSyndrome;

    @FindBy(xpath = "//*[@class = 'fieldset omim_id ']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][.//input[@value = '123450']]//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']")
    WebElement checkCriDuChatAppears;

    @FindBy(id = "PhenoTips.PatientClass_0_omim_id_123450")
    WebElement criDuChatOMIMTop;

    @FindBy(xpath = "//*[contains(@class, 'summary-item')][.//input[@value = 'HP:0000639']]//*[@class = 'tool'][text() = 'Delete']")
    WebElement deleteNystagmusRight;

    @FindBy(xpath = "//*[@class = 'resultContainer']//li[contains(@class, 'xitem')]//*[contains(@class, 'xTooltip')]//*[contains(text(), 'Browse related terms')]")
    WebElement browseRelatedTermsCataract;

    @FindBy(xpath = "//*[@class = 'entry-data'][.//*[contains(@class, 'yes-no-picker')]][.//*[@value = 'HP:0000517']]//*[@class = 'value'][text() = 'Abnormality of the lens']")
    WebElement abnormalityOfTheLensGoUp;

    @FindBy(xpath = "//*[@class = 'entry-data'][.//*[contains(@class, 'yes-no-picker')]][.//*[@value = 'HP:0004328']]//*[@class = 'value'][text() = 'Abnormality of the anterior segment of the eye']")
    WebElement abnormalityOfTheAnteriorSegmentOfTheEyeCheck;

    @FindBy(xpath = "//*[contains(@class, 'msdialog-modal-container')]//*[@class = 'msdialog-box']//*[@class = 'msdialog-close']")
    WebElement closeBrowseRelatedTerms;

    @FindBy(xpath = "//*[contains(@class, 'entry descendent')][.//span[contains(@class, 'yes-no-picker')]][.//label[@class = 'yes']][.//input[@value = 'HP:0012629']]//*[contains(@class, 'yes-no-picker')]//*[@class = 'yes']")
    WebElement phacodonesisYes;

    @FindBy(xpath = "//*[contains(@class, 'suggestItems')]//*[@class = 'hide-button-wrapper']//*[@class = 'hide-button']")
    WebElement hideQuickSearchBarSuggestions;

    @FindBy(xpath = "//*[@class = 'calendar_date_select']//*[@class = 'month']")
    WebElement setMonthDate;

    @FindBy(xpath = "//*[@class = 'calendar_date_select']//*[contains(@class, 'cds_body')]//td[.//div[text() = '1']][1]")
    WebElement date1;

    @FindBy(xpath = "//*[@class = 'calendar_date_select']//*[contains(@class, 'cds_body')]//td[.//div[text() = '12']][1]")
    WebElement date12;

    @FindBy(xpath = "//*[@class = 'term-entry'][.//*[@value = 'HP:0000601']]//*[contains(@class, 'phenotype-info')]")
    WebElement moreInfoHypotelorism;

    @FindBy(xpath = "//*[@class = 'term-entry'][.//*[@value = 'HP:0000601']]//*[@class = 'xTooltip']//*[@class = 'value']")
    WebElement checkMoreInfoHypotelorismOpened;

    @FindBy(xpath = "//*[@class = 'term-entry'][.//*[@value = 'HP:0000601']]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoHypotelorism;

    @FindBy(xpath = "//*[@class = 'fieldset gender gender']//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoSex;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[contains(@class, 'gender')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoSex;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[contains(@class, 'gender')]//*[@class = 'xTooltip']//span[@class = 'hide-tool']")
    WebElement closeMoreInfoSex;

    @FindBy(css = "#inline > div:nth-child(3) > div.clear.clinical-info.patient-info.chapter > div.fieldset.indication_for_referral > p > span")
    WebElement moreInfoIndicationForReferral;

    @FindBy(css = "#inline > div:nth-child(3) > div.clear.clinical-info.patient-info.chapter > div.fieldset.indication_for_referral > p > div > div")
    WebElement checkMoreInfoIndicationForReferral;

    @FindBy(xpath = "//*[contains(@class, 'patient-info')]//*[contains(@class, 'indication_for_referral ')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoIndicationForReferral;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[contains(@class, 'relatives-info')]//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoNewEntryFamilyStudy;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[contains(@class, 'relatives-info')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoNewEntryFamilyStudy;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[contains(@class, 'relatives-info')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoNewEntryFamilyStudy;

    @FindBy(xpath = "//*[contains(@class, 'family-info')]//*[@class = 'list-actions']//*[contains(@class, 'add-data-button')]")
    WebElement newEntryFamilyStudy;

    @FindBy(id = "PhenoTips.RelativeClass_0_relative_type")
    WebElement thisPatientIsThe;

    @FindBy(xpath = "//*[contains(@class, 'relatives-info')]//*[@class = 'relative_of']//*[@class = 'suggested']")
    WebElement ofPatientWithIdentifier;

    @FindBy(xpath = "//*[contains(@class, 'family_history')]//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoFamilyHealthConditions;

    @FindBy(xpath = "//*[contains(@class, 'family_history')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoFamilyHealthConditions;

    @FindBy(xpath = "//*[contains(@class, 'family_history')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoFamilyHealthConditions;

    @FindBy(xpath = "//*[@class = 'fieldset gestation ']//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoGestation;

    @FindBy(xpath = "//*[@class = 'fieldset gestation ']//*[@class = 'xTooltip']")
    WebElement checkMoreInfoGestation;

    @FindBy(xpath = "//*[@class = 'fieldset gestation ']//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoGestation;

    @FindBy(xpath = "//*[@class = 'fieldset global_age_of_onset ']//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoGlobalAgeOfOnset;

    @FindBy(xpath = "//*[@class = 'fieldset global_age_of_onset ']//*[@class = 'xTooltip']")
    WebElement checkMoreInfoGlobalAgeOfOnset;

    @FindBy(xpath = "//*[@class = 'fieldset global_age_of_onset ']//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoGlobalAgeOfOnset;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoNewEntryMeasurements;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoNewEntryMeasurements;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoNewEntryMeasurements;

    @FindBy(xpath = "//*[contains(@class, 'diagnosis-info')]//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoOMIMDisorder;

    @FindBy(xpath = "//*[contains(@class, 'diagnosis-info')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoOMIMDisorder;

    @FindBy(xpath = "//*[contains(@class, 'diagnosis-info')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoOMIMDisorder;

    @FindBy(id = "HYoumaywanttoinvestigate...")
    WebElement youMayWantToInvestigate;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//*[contains(@class, 'fa-question-circle')]")
    WebElement moreInfoSelectObservedPhenotypes;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//*[contains(@class, 'unaffected controller')]//*[@class = 'xTooltip']")
    WebElement checkMoreInfoSelectObservedPhenotypes;

    @FindBy(xpath = "//*[contains(@class, 'phenotype-info')]//*[contains(@class, 'unaffected controller')]//*[@class = 'xTooltip']//*[@class = 'hide-tool']")
    WebElement closeMoreInfoSelectObservedPhenotypes;

    @FindBy(xpath = "//*[@class = 'browse-phenotype-categories']//*[@class = 'expand-tools'][@class = 'collapse-all']//*[@class = 'collapse-all']")
    WebElement collapseAllPhenotypes;

    @FindBy(xpath = "//*[@class = 'browse-phenotype-categories']//*[@class = 'expand-tools'][@class = 'collapse-all']//*[@class = 'expand-all']")
    WebElement expandAllPhenotypes;

    @FindBy(xpath = "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, birth to 36 months, boys']")
    WebElement chartTitleBoys;

    @FindBy(xpath = "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, birth to 36 months, girls']")
    WebElement chartTitleGirls;

    @FindBy(xpath = "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, 2 to 20 years, boys']")
    WebElement chartTitleOlderBoys;

    @FindBy(id = "PhenoTips.MeasurementsClass_0_date")
    WebElement dateOfMeasurments;

    @FindBy(xpath = "//*[contains(@class, 'measurement-info')]//*[contains(@class, 'extradata-list')]//*[@class = 'age']//*[@class = 'age displayed-value']")
    WebElement getAgeMeasurements;

    @FindBy(xpath = "//*[@class = 'bottombuttons']//*[@class = 'button'][@name = 'action_save']")
    WebElement saveAndViewButton;

    @FindBy(xpath = "//*[@class = 'bottombuttons']//*[@class = 'button'][@name = 'action_saveandcontinue']")
    WebElement saveAndContinueButton;

    @FindBy(xpath = "//*[contains(@class, 'maternal_ethnicity')]//*[@class = 'hint']")
    WebElement checkFamilyHistoryExpanded;

    @FindBy(xpath = "//*[contains(@class, 'assistedReproduction_fertilityMeds ')]//*[@class = 'yes']")
    WebElement assistedReproductionFertilityMedsYes;

    @FindBy(xpath = "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, birth to 36 months, boys']")
    WebElement checkIfGrowthChartsAreShowingByText;

    // MINE

    @FindBy(id = "PhenoTips.PatientClass_0_external_id")
    WebElement patientIdentifier;

    @FindBy(id = "body")
    WebElement body;

    /* Date of Birth */

    /* Date of Death */

    @FindBy(xpath = "//*[@class = 'calendar_date_select']//*[@class = 'year']")
    WebElement setYearDateOfBirth;

    @FindBy(css = ".fieldset.date_of_birth.date > div:nth-child(2) > div > div")
    WebElement birthDateSelector;

    @FindBy(css = ".fieldset.date_of_death.date > div:nth-child(2) > div > div")
    WebElement deathDateSelector;

    @FindBy(id = "PhenoTips.PatientClass_0_last_name")
    WebElement patientLastName;

    @FindBy(id = "PhenoTips.PatientClass_0_first_name")
    WebElement patientFirstName;

    @FindBy(id = "xwiki-form-gender-0-0")
    WebElement patientGenderMale;

    @FindBy(id = "xwiki-form-gender-0-1")
    WebElement patientGenderFemale;

    @FindBy(id = "xwiki-form-gender-0-2")
    WebElement genderOther;

    @FindBy(id = "PhenoTips.PatientClass_0_global_mode_of_inheritance_HP:0003745")
    WebElement globalInheritanceSporadic;

    @FindBy(id = "PhenoTips.PatientClass_0_global_mode_of_inheritance_HP:0000006")
    WebElement globalInheritanceAutosomal;

    @FindBy(id = "PhenoTips.PatientClass_0_global_mode_of_inheritance_HP:0010982")
    WebElement globalInheritancePolygenic;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_gestation_term")
    WebElement checkTermBirth;

    // Assisted Reproduction

    @FindBy(css = ".assistedReproduction_fertilityMeds > div > span > label.yes")
    WebElement assistedReproductionFertilityYes;

    @FindBy(css = ".prenatal-info.chapter > div.fieldset.ivf > div > span > label.no")
    WebElement assistedReproductionInVitroNo;

    // APGAR Scores

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_apgar1")
    WebElement APGAROneMinute;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_apgar5")
    WebElement APGARFiveMinutes;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_prenatal_development")
    WebElement prenatalNotes;

    @FindBy(xpath = "//*[@id=\"PhenoTips.PatientClass_0_prenatal_phenotype_HP:0001518\"]")
    WebElement prenatalGrowthSmallGestationalYes;

    @FindBy(css = ".prenatal_phenotype-main.predefined-entries > div:nth-child(4) > div:nth-child(2) > span.yes-no-picker.initialized > label.yes")
    WebElement prenatalGrowthLargeBirthYes;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_prenatal_phenotype_0\\2e 5173270363501185")
    WebElement prenatalGrowthOther;

    @FindBy(css = ".prenatal_phenotype-main.predefined-entries > div:nth-child(2) > span.yes-no-picker.initialized > label.no")
    WebElement prenatalDevelopmentPolyhydramniosNo;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_prenatal_phenotype_0\\2e 8363013346655174")
    WebElement prenatalDevelopmentOther;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_global_age_of_onset_HP\\3a 0003584")
    WebElement lateOnset;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_medical_history")
    WebElement medicalHistory;

    @FindBy(css = "#extradata-list-PhenoTips\\2e InvestigationClass-molecular > tbody > tr.new > td.gene > input")
    WebElement geneCandidateSearch;

    @FindBy(css = "#PhenoTips\\2e InvestigationClass_0_comments")
    WebElement geneCandidateComment;

    @FindBy(css = ".genotype.chapter > div:nth-child(5) > p > span.buttonwrapper > a")
    WebElement newEntryListOfCandidateGenes;

    @FindBy(css = "#extradata-list-PhenoTips\\2e RejectedGenesClass > tbody > tr.new > td.gene > input")
    WebElement genePreviously;

    @FindBy(css = "#PhenoTips\\2e RejectedGenesClass_1_comments")
    WebElement genePreviouslyTestedComment;

    @FindBy(css = ".genotype.chapter > div:nth-child(8) > p > span.buttonwrapper > a")
    WebElement newEntryPreviouslyTested;

    @FindBy(id = "HCaseresolution")
    WebElement caseResolution;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_solved")
    WebElement caseSolved;

    @FindBy(css = ".phenotype-info.chapter.collapsed > span > span.buttonwrapper.show > button")
    WebElement clinicalSymptomsAndPhysicalFindings;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_solved__pubmed_id")
    WebElement pubmedID;

    @FindBy(css = ".fieldset.solved__gene_id > div:nth-child(2) > div > input")
    WebElement geneID;

    @FindBy(css = "#PhenoTips\\2e PatientClass_0_solved__notes")
    WebElement resolutionNotes;

    @FindBy(css = "#inline > div:nth-child(3) > div.bottombuttons > div.buttons > span:nth-child(3) > input")
    WebElement saveAndViewSummary;

    public static PatientRecordEditPage gotoPage(String patientId)
    {
        getUtil().gotoPage("data", patientId, "edit");
        return new PatientRecordEditPage();
    }

    public void clickBody()
    {
        this.body.click();
    }

    /* PATIENT INFORMATION */

    /**
     * Sets the first and last name of the patient
     *
     * @param first patient first name
     * @param last patient last name
     */
    public void setPatientName(String first, String last)
    {
        // first name
        this.patientLastName.clear();
        this.patientLastName.sendKeys(first);

        // last name
        this.patientFirstName.clear();
        this.patientFirstName.sendKeys(last);
    }

    /**
     * Sets the full birthdate of the patient
     *
     * @param day the birthdate of the patient
     * @param month the birthmonth of the patient
     * @param year the birthyear of the patient
     */
    public void setPatientDateOfBirth(String day, String month, String year)
    {
        // date
        new Select(this.birthDateSelector.findElement(By.cssSelector("span:nth-child(3) > select")))
            .selectByVisibleText(day);

        // month
        new Select(this.birthDateSelector.findElement(By.cssSelector("span:nth-child(2) > select")))
            .selectByVisibleText(month);

        // year
        new Select(this.birthDateSelector.findElement(By.cssSelector("span:nth-child(1) > select")))
            .selectByVisibleText(year);
    }

    /**
     * Sets the date of passing of the patient
     *
     * @param day the day of death of the patient
     * @param month the month death of the patient
     * @param year the year death of the patient
     */
    public void setPatientDateOfDeath(String day, String month, String year)
    {
        // date
        new Select(this.deathDateSelector.findElement(By.cssSelector("span:nth-child(3) > select")))
            .selectByVisibleText(day);

        // month
        new Select(this.deathDateSelector.findElement(By.cssSelector("span:nth-child(2) > select")))
            .selectByVisibleText(month);

        // year
        new Select(this.deathDateSelector.findElement(By.cssSelector("span:nth-child(1) > select")))
            .selectByVisibleText(year);
    }

    /**
     * Sets the gender of the patient
     *
     * @param gender this either the string "male" or the string "female"
     */
    public void setPatientGender(String gender)
    {
        if (gender == "male") {
            this.patientGenderMale.click();
        } else if (gender == "female") {
            this.patientGenderFemale.click();
        }
        this.body.click();
    }

    public void setOtherGender()
    {
        this.genderOther.click();
        this.body.click();
    }

    public void expandFamilyHistory()
    {
        this.expandFamilyHistory.click();
    }

    /**
     * Creates a new entry for family studies
     *
     * @param relative type of relative; one of "Child", "Parent", "Sibling"... etc.
     * @param relative_id the reference if of the relative in the system
     */
    public void newEntryFamilyStudy(String relative, String relative_id)
    {
        // click new family study button
        this.newEntryFamilyStudy.click();

        // select the type of relative
        // this.waitUntilElementIsVisible(By.id("PhenoTips.RelativeClass_0_relative_type"));
        new Select(this.thisPatientIsThe).selectByVisibleText(relative);

        // input the id of the relative
        this.ofPatientWithIdentifier.clear();
        this.ofPatientWithIdentifier.sendKeys(relative_id);
    }

    /**
     * Sets ethnicities in Family History tab
     *
     * @param maternal the maternal ethnicity of the patient
     * @param paternal the paternal ethnicity of the patient
     */
    public void setEthnicites(String maternal, String paternal)
    {
        this.maternalEthnicity.clear();
        this.maternalEthnicity.sendKeys(maternal);

        this.paternalEthnicity.clear();
        this.paternalEthnicity.sendKeys(paternal);
    }

    /**
     * Checkboxes global mode of inheritance for autosomal, polygenic and sporadic
     */
    public void setGlobalModeOfInheritance()
    {
        this.globalInheritanceAutosomal.click();
        this.globalInheritancePolygenic.click();
        this.globalInheritanceSporadic.click();
    }

    /* PRENETAL AND PERINATAL HISTORY */

    public void expandPrenatalAndPerinatalHistory()
    {
        this.expandPrenatalAndPerinatalHistory.click();
    }

    /**
     * Sets prenatal gestration at birth text box and checks the term birth box if wanted
     *
     * @param weeks the number of weeks to input in the text box
     */
    public void setPrenatalGestationAtDelivery(String weeks)
    {
        this.gestationAtDelivery.clear();
        this.gestationAtDelivery.sendKeys(weeks);
    }

    /**
     * Sets the yes and no values for assisted reproduction boxes
     */
    public void setAssistedReproduction()
    {
        this.assistedReproductionFertilityYes.click();
        this.assistedReproductionInVitroNo.click();
    }

    /**
     * Sets APGAR scores from the one and five minute options
     *
     * @param oneMinute a string that this one of the numbers "1" to "10" or "Unknown" for the one minute APGAR
     * @param fiveMinute a string that this one of the numbers "1" to "10" or "Unknown" for the five minute APGAR
     */
    public void setAPGARScores(String oneMinute, String fiveMinutes)
    {
        // this.waitUntilElementIsVisible(By.id("PhenoTips.PatientClass_0_apgar1"));
        new Select(this.APGAROneMinute).selectByVisibleText(oneMinute);

        // this.waitUntilElementDisappears(By.id("PhenoTips.PatientClass_0_apgar5"));
        new Select(this.APGARFiveMinutes).selectByVisibleText(fiveMinutes);
    }

    public void setPrenatalNotes(String notes)
    {
        this.prenatalNotes.clear();
        this.prenatalNotes.sendKeys(notes);
    }

    /**
     * Sets the prenatal growth parameters
     *
     * @param other the text to goes in the other text box
     */
    public void setPrenatalGrowthParameters(String other)
    {
        // doesn't work, element isn't visible
        this.prenatalGrowthSmallGestationalYes.click();
        this.prenatalGrowthLargeBirthYes.click();
        this.prenatalGrowthOther.clear();
        this.prenatalGrowthOther.sendKeys();
    }

    /**
     * Sets the prenatal developement or birth information
     *
     * @param other the text that goes in the other text box
     */
    public void setPrenatalDevelopmentOrBirth(String other)
    {
        // doesn't work, element isn't visible
        this.prenatalDevelopmentPolyhydramniosNo.click();
        this.prenatalDevelopmentOther.clear();
        this.prenatalDevelopmentOther.sendKeys(other);
    }

    /**
     * Enters text in the "medical and developmental history text box
     *
     * @param history the text to be entered
     */
    public void setMedicalHistory(String history)
    {
        this.medicalHistory.clear();
        this.medicalHistory.sendKeys(history);

    }

    /**
     * Clicks the radio button "Late onset" in the radio group of "Global age at onset" buttons
     */
    public void setLateOnset()
    {
        this.lateOnset.click();
    }

    /**
     * Returns the number of elements that match the css for the "upload image" button
     *
     * @return the number of elements matching this css
     */
    public int findElementsUploadImage()
    {
        return getDriver()
            .findElements(
                By.cssSelector("#PhenoTips\\2e PatientClass_0_reports_history_container > div.actions > span > a"))
            .size();
    }

    public void clickTermBirth()
    {
        this.checkTermBirth.click();
    }

    public void openNewEntryListOfCandidateGenes()
    {
        this.newEntryListOfCandidateGenes.click();
    }

    public int checkGeneCandidateSearchHideSuggestions(String search)
    {
        this.geneCandidateSearch.clear();
        this.geneCandidateSearch.sendKeys(search);
        return getDriver()
            .findElements(By.cssSelector("#body > div.suggestItems.ajaxsuggest > div:nth-child(1) > span")).size();
    }

    public void setGeneCandidateComment(String comment)
    {
        this.geneCandidateComment.clear();
        this.geneCandidateComment.sendKeys(comment);
    }

    public void openNewEntryPreviouslyTested()
    {
        this.newEntryPreviouslyTested.click();
    }

    public int checkGenePreviouslySearchHideSuggestions(String search)
    {
        this.genePreviously.clear();
        this.genePreviously.sendKeys(search);
        return getDriver()
            .findElements(By.cssSelector("#body > div.suggestItems.ajaxsuggest > div:nth-child(1) > span")).size();
    }

    public void setPreviouslyTestedGenesComment(String comment)
    {
        this.genePreviouslyTestedComment.clear();
        this.genePreviouslyTestedComment.sendKeys(comment);
    }

    public void expandCaseResolution()
    {
        this.caseResolution.click();
    }

    public void setCaseSolved()
    {
        this.caseSolved.click();
    }

    public void setIDsAndNotes(String pID, String gID, String notes)
    {
        this.pubmedID.clear();
        this.pubmedID.sendKeys(pID);
        this.geneID.clear();
        this.geneID.sendKeys(gID);
        this.resolutionNotes.clear();
        this.resolutionNotes.sendKeys(notes);
    }

    public void setPatientIdentifier(String value)
    {
        this.patientIdentifier.clear();
        this.patientIdentifier.sendKeys(value);
    }

    public void familyHealthConditions(String value)
    {
        this.familyHealthConditions.clear();
        this.familyHealthConditions.sendKeys(value);
    }

    public void expandMedicalHistory()
    {
        this.expandMedicalHistory.click();
    }

    public void expandMeasurements()
    {
        this.expandMeasurements.click();
    }

    public void createNewMeasurementsEntry()
    {
        this.newEntryMeasurements.click();
    }

    /* setting measurements */

    public void setMeasurementWeight(String value)
    {
        // this.getDriver().waitUntilElementIsVisible(By.id("PhenoTips.MeasurementsClass_0_weight"));
        this.measurementWeight.clear();
        this.measurementWeight.click();
        this.measurementWeight.sendKeys(value);
    }

    public void setMeasurementHeight(String value)
    {
        this.measurementHeight.clear();
        this.measurementHeight.click();
        this.measurementHeight.sendKeys(value);
    }

    public void setMeasurementArmSpan(String value)
    {
        this.measurementArmSpan.clear();
        this.measurementArmSpan.click();
        this.measurementArmSpan.sendKeys(value);
    }

    public void setMeasurementSittingHeight(String value)
    {
        this.measurementSittingHeight.clear();
        this.measurementSittingHeight.click();
        this.measurementSittingHeight.sendKeys(value);
    }

    public void setMeasurementHeadCircumference(String value)
    {
        this.measurementHeadCircumference.clear();
        this.measurementHeadCircumference.sendKeys(value);
    }

    public void setMeasurementPhiltrumLength(String value)
    {
        this.measurementPhiltrumLength.clear();
        this.measurementPhiltrumLength.sendKeys(value);
    }

    public void setMeasurementLeftEarLength(String value)
    {
        this.measurementLeftEarLength.clear();
        this.measurementLeftEarLength.sendKeys(value);
    }

    public void setMeasurementRightEarLength(String value)
    {
        this.measurementRightEarLength.clear();
        this.measurementRightEarLength.sendKeys(value);
    }

    public void setMeasurementOuterCanthalDistance(String value)
    {
        this.measurementOuterCanthalDistance.clear();
        this.measurementOuterCanthalDistance.sendKeys(value);
    }

    public void setMeasurementInnerCanthalDistance(String value)
    {
        this.measurementInnerCanthalDistance.clear();
        this.measurementInnerCanthalDistance.sendKeys(value);
    }

    public void setMeasuremtnePalpebralFissureLength(String value)
    {
        this.measurementPalpebralFissureLength.clear();
        this.measurementPalpebralFissureLength.sendKeys(value);
    }

    public void setMeasurementInterpupilaryDistance(String value)
    {
        this.measurementInterpupilaryDistance.clear();
        this.measurementInterpupilaryDistance.sendKeys(value);
    }

    public void setMeasurementLeftHandLength(String value)
    {
        this.measurementLeftHandLength.clear();
        this.measurementLeftHandLength.sendKeys(value);
    }

    public void setMeasurementLeftPalmLength(String value)
    {
        this.measurementLeftPalmLength.clear();
        this.measurementLeftPalmLength.sendKeys(value);
    }

    public void setMeasurementLeftFootLength(String value)
    {
        this.measurementLeftFootLength.clear();
        this.measurementLeftFootLength.sendKeys(value);
    }

    public void setMeasurementRightHandLength(String value)
    {
        this.measurementRightHandLength.clear();
        this.measurementRightHandLength.sendKeys(value);
    }

    public void setMeasurementRightPalmLength(String value)
    {
        this.measurementRightPalmLength.clear();
        this.measurementRightPalmLength.sendKeys(value);
    }

    public void setMeasurementRightFootLength(String value)
    {
        this.measurementRightFootLength.clear();
        this.measurementRightFootLength.sendKeys(value);
    }

    public boolean checkIfGrowthChartsAreShowing()
    {
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, birth to 36 months, boys']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public String checkIfGrowthChartsAreShowingByText()
    {
        return this.checkIfGrowthChartsAreShowingByText.getText();
    }

    public void expandGenotypeInformation()
    {
        this.expandGenotypeInformation.click();
    }

    ///////////////////////////////////

    // public void setGenotypeInformationComments(String value)
    // {
    // this.getDriver().waitUntilElementIsVisible(By.id("PhenoTips.InvestigationClass_0_comments"));
    // this.genotypeInformationComments.clear();
    // this.genotypeInformationComments.sendKeys(value);
    // }

    // public void setGenotypeInformationGene(String value)
    // {
    // this.genotypeInformationGene.clear();
    // this.genotypeInformationGene.sendKeys(value);
    // }

    public void setPatientClinicallyNormal()
    {
        this.patientIsClinicallyNormal.click();
    }

    public void expandClinicalSymptomsAndPhysicalFindings()
    {
        this.clinicalSymptomsAndPhysicalFindings.click();
    }

    public boolean checkIfClinicalSymptomsAndPhysicalFindingsExpanded(By by)
    {
        try {
            getDriver().findElement(By.id("PhenoTips.PatientClass_0_unaffected"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void selectPhenotype(String id, boolean positive)
    {
        BaseElement
            .getUtil()
            .findElementWithoutWaiting(
                getDriver(),
                By.cssSelector("label[for='PhenoTips.PatientClass_0_" + (positive ? "" : "negative_") + "phenotype_"
                    + id + "']")).click();
        ;
    }

    public void setHypotelorismYes()
    {
        selectPhenotype("HP:0000601", true);
    }

    public void setStature3rdPercentile()
    {
        selectPhenotype("HP:0004322", true);
    }

    public void setWeight3rdPercentile()
    {
        selectPhenotype("HP:0004325", true);

    }

    public void setHeadCircumference3rdPercentile()
    {
        selectPhenotype("HP:0000252", true);

    }

    public void setHemihypertrophyYes()
    {
        selectPhenotype("HP:0001528", true);

    }

    public void setCraniosynostosisYes()
    {

        selectPhenotype("HP:0001363", true);

    }

    public void setCleftUpperLipNO()
    {
        selectPhenotype("HP:0000204", false);

    }

    public void setCleftPalateYes()
    {
        selectPhenotype("HP:0000175", true);

    }

    public void setAbnormalFacialShapeYes()
    {
        selectPhenotype("HP:0001999", true);

    }

    public void setVisualImpairmentYes()
    {
        selectPhenotype("HP:0000505", true);

    }

    public void setAbnormalityOfTheCorneaNO()
    {
        selectPhenotype("HP:0000481", false);

    }

    public void setColobomaNO()
    {
        selectPhenotype("HP:0000589", false);

    }

    public void setSensorineuralNO()
    {
        selectPhenotype("HP:0000407", false);

    }

    public void setAbnormalityOfTheInnerEarYes()
    {
        selectPhenotype("HP:0000359", true);

    }

    public void setHyperpigmentationOfTheSkinYes()
    {
        selectPhenotype("HP:0000953", true);

    }

    public void setCapillaryHemangiomasNO()
    {
        selectPhenotype("HP:0005306", false);

    }

    public void setVentricularSeptalDefectNO()
    {
        selectPhenotype("HP:0001629", false);

    }

    public void setArrhythmiaYes()
    {
        selectPhenotype("HP:0011675", true);

    }

    public void setCongenitalDiaphragmaticHerniaYes()
    {
        selectPhenotype("HP:0000776", true);

    }

    public void setAbnormalityOfTheLungNO()
    {
        selectPhenotype("HP:0002088", false);

    }

    public void setLowerLimbUndergrowthYes()
    {
        selectPhenotype("HP:0009816", true);

    }

    public void setScoliosisYes()
    {
        selectPhenotype("HP:0002650", true);

    }

    public void setAbnormalityOfTheVertebralColumnNO()
    {
        selectPhenotype("HP:0000925", false);

    }

    public void setCholestasisYes()
    {
        selectPhenotype("HP:0001396", true);

    }

    public void setDiabetesMellitusNO()
    {
        selectPhenotype("HP:0000819", false);

    }

    public void setHorseshoeKidneyNO()
    {
        selectPhenotype("HP:0000085", false);

    }

    public void setHypospadiasYes()
    {
        selectPhenotype("HP:0000047", true);

    }

    public void setDelayedFineMotorDevelopmentYes()
    {
        selectPhenotype("HP:0010862", true);

    }

    public void setAttentionDeficitHyperactivityDisorderNO()
    {
        selectPhenotype("HP:0007018", false);

    }

    public void setAustismYes()
    {
        selectPhenotype("HP:0000717", true);

    }

    public void setSeizuresYes()
    {
        selectPhenotype("HP:0001250", true);

    }

    public void setSpinalDysraphismNO()
    {
        selectPhenotype("HP:0010301", false);

    }

    public void coarctationOfAortaDropDown()
    {
        this.coarctationOfAortaDropDown.click();
    }

    public String checkCoarctationOfAortaDropDown()
    {
        return this.checkCoarctationOfAortaDropDown.getText();
    }

    public void hemihypertrophyAddDetails()
    {
        this.hemihypertrophyAddDetails.click();
    }

    public void ageOfOnsetHemihypertrophy()
    {

        this.ageOfOnsetHemihypertrophy.click();
    }

    public void setNeonatalOnsetHemihypertrophy()
    {

        this.neonatalOnsetHemihypertrophy.click();
    }

    public void temporalPatternHemihypertrophy()
    {
        this.temporalPatternHemihypertrophy.click();
    }

    public void setSubacuteTemporalPatternHemihypertrophy()
    {
        this.subacuteTemporalPatternHemihypertrophy.click();
    }

    public void deleteCleftPalate()
    {
        this.deleteCleftPalate.click();
    }

    public void abnormalFacialShapeAddDetails()
    {
        this.abnormalFacialShapeAddDetails.click();
    }

    public void paceOfProgressionAbnormalFacialShape()
    {

        this.paceOfProgressionAbnormalFacialShape.click();
    }

    public void slowPaceOfProgressionAbnormalFacialShape()
    {

        this.slowPaceOfProgressionAbnormalFacialShape.click();
    }

    public void hypotelorismAddDetails()
    {
        this.hypotelorismAddDetails.click();
    }

    public void severityHypotelorism()
    {
        this.severityHypotelorism.click();
    }

    public void moderateSeverityHypotelorism()
    {
        this.moderateSeverityHypotelorism.click();
    }

    public void abnormalityOfTheInnerEarAddDetails()
    {
        this.abnormalityOfTheInnerEarAddDetails.click();
    }

    public void spatialPatternAbnormalityOfTheInnerEar()
    {
        this.spatialPatternAbnormalityOfTheInnerEar.click();
    }

    public void distalSpatialPatternAbnomalityOfTheInnerEar()
    {

        this.distalSpatialPatternAbnormalityOfTheInnerEar.click();
    }

    public void arrythmiaAddDetails()
    {
        this.arrhythmiaAddDetails.click();
    }

    public void arrythmiaComments(String value)
    {

        this.arrhythmiaComments.clear();
        this.arrhythmiaComments.sendKeys(value);
    }

    public void scoliosisAddDetails()
    {
        this.scoliosisAddDetails.click();
    }

    public void lateralityScoliosis()
    {

        this.lateralityScoliosis.click();
    }

    public void rightLateralityScoliosis()
    {

        this.rightLateralityScoliosis.click();
    }

    public void seizuresAddDetails()
    {
        this.seizuresAddDetails.click();
    }

    public void severitySeizures()
    {

        this.severitySeizures.click();
    }

    public void mildSeveritySeizures()
    {

        this.mildSeveritySeizures.click();
    }

    public void asymmetryOfTheEarsYes()
    {
        this.asymmetryOfTheEarsYes.click();
    }

    public void immunodeficiencyYes()
    {
        this.immunodeficiencyYes.click();
    }

    public void abnormalityOfTheCartoidArteriesYes()
    {
        this.abnormalityOfTheCartoidArteriesYes.click();
    }

    public void bifidTongueYes()
    {
        this.bifidTongueYes.click();
    }

    public void bifidRibsYes()
    {
        this.bifidRibsYes.click();
    }

    public void hypsarrhythmiaNO()
    {
        this.hypsarrhythmiaNO.click();
    }

    public void phenotypeQuickSearch(String value)
    {
        this.phenotypeQuickSearch.clear();
        this.phenotypeQuickSearch.sendKeys(value);
    }

    public void quickSearchCataractYes()
    {
        this.quickSearchCataractYes.click();
    }

    public void expandDiagnosis()
    {
        this.expandDiagnosis.click();
    }

    public void setDiagnosisAdditionalComments(String value)
    {
        this.diagnosisAdditionalComments.clear();
        this.diagnosisAdditionalComments.sendKeys(value);
    }

    public void setSmithLemliOptizSyndrome()
    {
        // this.getDriver().waitUntilElementIsVisible(By.id("result__270400"));
        this.smithLemliOptizSyndrome.click();

    }

    public void setWatsonSyndrome()
    {
        // this.getDriver().waitUntilElementIsVisible(By.id("result__193520"));
        this.watsonSyndrome.click();
    }

    public void setIndicationForReferral(String value)
    {
        this.indicationForReferral.clear();
        this.indicationForReferral.sendKeys(value);
    }

    public void setPrenatalGrowthPatternsOther(String value)
    {
        this.prenatalGrowthPatternsOther.clear();
        this.prenatalGrowthPatternsOther.sendKeys(value);
    }

    public void setPositvieFerricChlorideTestYes()
    {

        this.positiveFerricChlorideTestYes.click();
    }

    public void setPrenatalDevelopmentOrBirthOther(String value)
    {
        this.prenatalDevelopmentOrBirthOther.clear();
        this.prenatalDevelopmentOrBirthOther.sendKeys(value);
    }

    public void setDysplasticTestesYes()
    {
        // this.getDriver().waitUntilElementIsVisible(By
        // .xpath("//*[@class = 'resultContainer']//*[@class = 'yes'][//input[@value = 'HP:0008733']]"));
        this.dysplasticTestesYes.click();
    }

    public String getWeightPctl()
    {
        return this.weightPctl.getText();
    }

    public String getHeightPctl()
    {
        return this.heightPctl.getText();
    }

    public String getBMIPctl()
    {
        return this.bmiPctl.getText();
    }

    public String getSittingPctl()
    {
        return this.sittingPctl.getText();
    }

    public String getHeadCircumferencePctl()
    {
        return this.headCircumferencePctl.getText();
    }

    public String getPhiltrumPctl()
    {
        return this.philtrumPctl.getText();
    }

    public String getLeftEarPctl()
    {
        return this.leftEarPctl.getText();
    }

    public String getRightEarPctl()
    {
        return this.rightEarPctl.getText();
    }

    public String getOuterCanthalPctl()
    {
        return this.outerCanthalPctl.getText();
    }

    public String getInnerCanthalPctl()
    {
        return this.innerCanthalPctl.getText();
    }

    public String getPalpebralFissurePctl()
    {
        return this.palpebralFissurePctl.getText();
    }

    public String getInterpupilaryPctl()
    {
        return this.interpupilaryPctl.getText();
    }

    public String getLeftPalmPctl()
    {
        return this.leftPalmPctl.getText();
    }

    public String getLeftFootPctl()
    {
        return this.leftFootPctl.getText();
    }

    public String getRightPalmPctl()
    {
        return this.rightPalmPctl.getText();
    }

    public String getRightFootPctl()
    {
        return this.rightFootPctl.getText();
    }

    public String getBMINumber()
    {
        return this.bmiNumber.getText();
    }

    public void setOMIMDisorder(String value)
    {
        this.OMIMDisorderBar.clear();
        this.OMIMDisorderBar.sendKeys(value);
    }

    public void setOMIMDisorderWolfSyndrome()
    {

        this.OMIMDisorderWolfSyndrome.click();
    }

    public void excludeLowerLimbUndergrowth()
    {
        this.OMIMBoxLowerLimbUnderGrowth.click();
    }

    public String checkLowerLimbUndergrowthExcluded()
    {
        return this.OMIMBoxLowerLimbUnderGrowthExcluded.getText();
    }

    public String checkSmithLemliInOMIM()
    {
        return this.checkSmithLemliInOMIM.getText();
    }

    public void setCriDuChatSyndromeFromBottom()
    {
        // this.getDriver().waitUntilElementIsVisible(By.id("result__123450"));
        this.criDuChatSyndrome.click();
    }

    public String checkCriDuChatAppears()
    {
        return this.checkCriDuChatAppears.getText();
    }

    public boolean checkCriDuChatDisappearsFromTop(By by)
    {
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'fieldset omim_id']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkShortStatureLightningBoltAppearsOnRight()
    {

        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-item'][.//input[@value = 'HP:0004322']]//*[contains(@class, 'fa-bolt')]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkHypotelorismLightningBoltAppearsOnRight()
    {

        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-item'][.//input[@value = 'HP:0000601']]//*[contains(@class, 'fa-bolt')]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkCriDuChatDisappearsFromBottom()
    {
        getDriver()
            .findElement(
                By.xpath(
                    "//*[@class = 'fieldset omim_id ']//*[@class = 'displayed-value']//*[@class = 'accepted-suggestion'][@value = '123450']//*[@class = 'value'][text()='#123450 CRI-DU-CHAT SYNDROME ']"))
            .isSelected();
        return true;
    }

    public void setCriDuChatFromTop()
    {
        // this.getDriver().waitUntilElementIsVisible(By.id("PhenoTips.PatientClass_0_omim_id_123450"));
        this.criDuChatOMIMTop.click();
    }

    public void setPreauricularPitYes()
    {
        selectPhenotype("HP:0004467", true);

    }

    public boolean checkPreauricularPitAppearsOnRight()
    {
        // this.getDriver().waitUntilElementIsVisible(By
        // .xpath("//*[@class = 'summary-item'][//label[@class = 'yes']][//input[@value = 'HP:0004467']]//*[@class =
        // 'yes'][text() = 'Preauricular pit']"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'summary-item'][//label[@class = 'yes']][//input[@value = 'HP:0004467']]//*[@class = 'yes'][text() = 'Preauricular pit']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void setNystagmusNO()
    {
        selectPhenotype("HP:0000639", false);

    }

    public boolean checkNystagmusAppearsOnRightNO()
    {
        // this.getDriver().waitUntilElementIsVisible(By
        // .xpath("//*[@class = 'summary-item'][//label[@class = 'no']][//input[@value = 'HP:0000639']]//*[@class =
        // 'no'][text() = 'Nystagmus']"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'summary-item'][//label[@class = 'no']][//input[@value = 'HP:0000639']]//*[@class = 'no'][text() = 'Nystagmus']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkNystagmusReturnsToNA()
    {

        try {
            return getDriver()
                .findElement(
                    By.xpath(
                        "//*[contains(@class, 'term-entry')][.//*[@value = 'HP:0000639']]//label[contains(@class, 'na')]"))
                .getAttribute("class").contains("selected");
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public void deleteNystagmusFromRight()
    {
        this.deleteNystagmusRight.click();
    }

    public boolean checkPolyphagiaDissapearsFromRightInvestigateBox()
    {
        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[@class = 'background-search']//*[@class = 'phenotype'][//label[@class = 'yes']][//input[@value = 'HP:0002591']]//*[@class = 'initialized']//*[@class = 'yes-no-picker-label'][text() = 'polyphagia']"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'background-search']//*[@class = 'phenotype'][//label[@class = 'yes']][//input[@value = 'HP:0002591']]//*[@class = 'initialized']//*[@class = 'yes-no-picker-label'][text() = 'polyphagia']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }

    }

    public boolean checkImmunodeficiencyDissapearsFromRightInvestigateBox()
    {

        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0002721']]"));

        try {
            return !getUtil()
                .hasElement(
                    By.xpath(
                        "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0002721']]"));
        } catch (NoSuchElementException e) {
            return false;

        }

    }

    public boolean checkAbnormalityOfTheCartoidArteriesDissapearsFromRightInvestigateBox()
    {

        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0005344']]"));

        try {
            return !getUtil()
                .hasElement(
                    By.xpath(
                        "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0005344']]"));
        } catch (NoSuchElementException e) {
            return false;

        }

    }

    public boolean checkBifidTongueDissapearsFromRightInvestigateBox()
    {

        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0010297']]"));

        try {
            return !getUtil()
                .hasElement(
                    By.xpath(
                        "//*[contains(@class, 'background-search')]//label[contains(@class, 'yes')][.//input[@value = 'HP:0010297']]"));
        } catch (NoSuchElementException e) {
            return false;

        }

    }

    public void polyphagiaYes()
    {
        this.polyphagiaYes.click();
    }

    public void browseRelatedTermsCataract()
    {
        this.getDriver()
            .findElement(
                By.xpath(
                    "//*[contains(@class, 'suggestItems')]//*[contains(@class, 'suggestItem')][//*[text() = 'Cataract']]//*[contains(@class, 'xHelpButton')]"))
            .click();
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[@class = 'resultContainer']//li[contains(@class, 'xitem')]//*[contains(@class, 'xTooltip')]//*[contains(text(), 'Browse related terms')]"));
        this.browseRelatedTermsCataract.click();
    }

    public void abnormalityOfTheLensGoUP()
    {

        this.abnormalityOfTheLensGoUp.click();
    }

    public String abnormalityOfTheAnteriorSegmentOfTheEye()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[@class = 'ontology-tree']//*[@class = 'entry parent']//*[@class = 'value'][text() = 'Abnormality of the anterior segment of the eye']"));
        return this.abnormalityOfTheAnteriorSegmentOfTheEyeCheck.getText();
    }

    public void phacodonesisYes()
    {
        this.phacodonesisYes.click();
    }

    public void closeBrowseRelatedTerms()
    {
        this.closeBrowseRelatedTerms.click();
    }

    public void hideQuickSearchBarSuggestions()
    {
        this.hideQuickSearchBarSuggestions.click();
    }

    public boolean checkPhacodonesisAppearsOnRight()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[@class = 'summary-item'][//label[@class = 'yes']][//input[@value = 'HP:0012629']]//*[@class = 'yes'][text() = 'Phacodonesis']"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'summary-item'][//label[@class = 'yes']][//input[@value = 'HP:0012629']]//*[@class = 'yes'][text() = 'Phacodonesis']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void openDateOfMeasurements()
    {
        this.dateOfMeasurments.click();
    }

    public void setDate1()
    {
        this.date1.click();
    }

    public void setDate12()
    {
        this.date12.click();
    }

    public void setGastroschisisYes()
    {
        selectPhenotype("HP:0001543", true);

    }

    public boolean checkSmithLemliSyndrome()
    {
        this.getDriver().waitUntilElementIsVisible(By.id("result__270400"));
        try {
            getDriver().findElement(By.id("result__270400"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void moreInfoHypotelorism()
    {
        this.moreInfoHypotelorism.click();
    }

    public String checkMoreInfoOpened()
    {
        return this.checkMoreInfoHypotelorismOpened.getText();
    }

    public void closeMoreInfoHypotelorism()
    {
        this.closeMoreInfoHypotelorism.click();
    }

    public void moreInfoSex()
    {
        this.moreInfoSex.click();
    }

    public String checkMoreInfoSex()
    {
        return this.checkMoreInfoSex.getText();
    }

    public void closeMoreInfoSex()
    {
        this.closeMoreInfoSex.click();
    }

    public void moreInfoIndicationForReferral()
    {
        this.moreInfoIndicationForReferral.click();
    }

    public String checkMoreInfoIndicationForReferral()
    {

        return this.checkMoreInfoIndicationForReferral.getText();
    }

    public void closeMoreInfoIndicationForReferral()
    {
        this.closeMoreInfoIndicationForReferral.click();
    }

    public void moreInfoNewEntryFamilyStudy()
    {
        this.moreInfoNewEntryFamilyStudy.click();
    }

    public String checkMoreInfoFamilyStudy()
    {

        return this.checkMoreInfoNewEntryFamilyStudy.getText();
    }

    public void closeMoreInfoNewEntryFamilyStudy()
    {
        this.closeMoreInfoNewEntryFamilyStudy.click();
    }

    public void setPatientIsTheRelativeOf(String relative)
    {

        this.getDriver().waitUntilElementIsVisible(By.id("PhenoTips.RelativeClass_0_relative_type"));
        new Select(this.thisPatientIsThe).selectByVisibleText(relative);
    }

    public void relativeOfCurrentPatient(String value)
    {
        this.ofPatientWithIdentifier.clear();
        this.ofPatientWithIdentifier.sendKeys(value);
    }

    public void moreInfoFamilyHealthConditions()
    {
        this.moreInfoFamilyHealthConditions.click();
    }

    public String checkMoreInfoFamilyHealthConditions()
    {
        return this.checkMoreInfoFamilyHealthConditions.getText();
    }

    public void closeMoreInfoFamilyHealthConditions()
    {
        this.closeMoreInfoFamilyHealthConditions.click();
    }

    public void moreInfoGestation()
    {
        this.moreInfoGestation.click();
    }

    public String checkMoreInfoGestation()
    {
        return this.checkMoreInfoGestation.getText();
    }

    public void closeMoreInfoGestation()
    {
        this.closeMoreInfoGestation.click();
    }

    public void moreInfoGlobalAgeOfOnset()
    {
        this.moreInfoGlobalAgeOfOnset.click();
    }

    public String checkMoreInfoGlobalAgeOfOnset()
    {
        return this.checkMoreInfoGlobalAgeOfOnset.getText();
    }

    public void closeMoreInfoGlobalAgeOfOnset()
    {
        this.closeMoreInfoGlobalAgeOfOnset.click();
    }

    public void moreInfoNewEntryMeasurements()
    {
        this.moreInfoNewEntryMeasurements.click();
    }

    public String checkMoreInfoNewEntryMeasurements()
    {

        return this.checkMoreInfoNewEntryMeasurements.getText();
    }

    public void closeMoreInfoNewEntryMeasurements()
    {
        this.closeMoreInfoNewEntryMeasurements.click();
    }

    public void moreInfoOMIMDisorder()
    {
        this.moreInfoOMIMDisorder.click();
    }

    public String checkMoreInfoOMIMDisorder()
    {
        return this.checkMoreInfoOMIMDisorder.getText();
    }

    public void closeMoreInfoOMIMDisorder()
    {
        this.closeMoreInfoOMIMDisorder.click();
    }

    public void hideYouMayWantToInvestigate()
    {
        this.youMayWantToInvestigate.click();
    }

    public boolean checkYouMayWantToInvestigateHid()
    {
        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[@class = 'background-suggestions]//*[@class = 'phenotype'][@class = 'yes'][//input[@value = 'HP:0000878']]//*[@class = 'yes-no-picker']"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@class = 'background-suggestions]//*[@class = 'phenotype'][@class = 'yes'][//input[@value = 'HP:0000878']]//*[@class = 'yes-no-picker']"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }

    }

    public void moreInfoSelectObservedPhenotypes()
    {
        this.moreInfoSelectObservedPhenotypes.click();
    }

    public String checkMoreInfoSelectObservedPhenotypes()
    {
        return this.checkMoreInfoSelectObservedPhenotypes.getText();
    }

    public void closeMoreInfoSelectObservedPhenotypes()
    {
        this.closeMoreInfoSelectObservedPhenotypes.click();
    }

    public void collapseAllPhenotypes()
    {
        this.collapseAllPhenotypes.click();
    }

    public boolean checkAllPhenotypesCollapsed()
    {
        this.getDriver().waitUntilElementDisappears(
            By.cssSelector("label[for='PhenoTips.PatientClass_0_phenotype_HP:0001363]"));
        try {
            getDriver().findElement(By.cssSelector("label[for='PhenoTips.PatientClass_0_phenotype_HP:0001363]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public void expandAllPhenotypes()
    {
        this.expandAllPhenotypes.click();
    }

    public void setYearDateOfBirth(String year)
    {
        new Select(this.setYearDateOfBirth).selectByVisibleText(year);
    }

    public void setYearMeasurement(String year)
    {
        new Select(this.setYearDateOfBirth).selectByVisibleText(year);
    }

    public void setMonthDateOfBirth(String month)
    {
        new Select(this.setMonthDate).selectByVisibleText(month);
    }

    public void setMonthMeasurement(String month)
    {
        new Select(this.setMonthDate).selectByVisibleText(month);
    }

    public boolean checkDecreasedBodyWeight()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004325']]"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004325']]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkDecreasedBodyWeightDisappears()
    {
        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004325']]"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004325']]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkShortStature()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004322']]"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004322']]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public boolean checkShortStatureDisappears()
    {

        this.getDriver().waitUntilElementDisappears(By
            .xpath(
                "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004322']]"));
        try {
            getDriver()
                .findElement(
                    By.xpath(
                        "//*[@id = 'current-phenotype-selection']//*[@class = 'summary-group']//*[@class = 'yes'][.//input[@value = 'HP:0004322']]"));
            return true;
        } catch (NoSuchElementException e) {
            return false;

        }
    }

    public String checkChartTitleChangesMale()
    {

        return this.chartTitleBoys.getText();
    }

    public String checkChartTitleChangesFemale()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath(
                "//*[contains(@class, 'growth-charts-section')]//*[@id = 'charts']//*[contains(@class, 'chart-wrapper')]//*[text() = 'Weight for age, birth to 36 months, girls']"));
        return this.chartTitleGirls.getText();
    }

    public String checkChartTitleChangesOlderMale()
    {
        this.getDriver().waitUntilElementIsVisible(By
            .xpath("//*[@class = 'chart-title'][text() = 'Weight for age, 2 to 20 years, boys']"));
        return this.chartTitleOlderBoys.getText();
    }

    @Override
    public PatientRecordViewPage clickSaveAndView()
    {
        this.saveAndViewButton.click();
        return new PatientRecordViewPage();
    }

    @Override
    public void clickSaveAndContinue()
    {
        this.saveAndContinueButton.click();
    }

    public String getAgeMeasurements()
    {
        return this.getAgeMeasurements.getText();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected PatientRecordViewPage createViewPage()
    {
        return new PatientRecordViewPage();
    }

    @Override
    public void waitUntilPageJSIsLoaded()
    {
        getDriver().waitUntilJavascriptCondition("return window.Prototype != null && window.Prototype.Version != null");
    }
}
