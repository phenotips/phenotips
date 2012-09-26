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
package edu.toronto.cs.cidb.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

/**
 * Computes the percentiles for Body Mass Index (BMI) and head circumference of a patient.
 * 
 * @version $Id$
 */
@Component
@Named("percentile")
@Singleton
public class PercentileTools implements ScriptService, Initializable
{
    /** Tool used for computing the percentile corresponding to a given z-score. */
    private static final NormalDistribution NORMAL = new NormalDistributionImpl();

    /** The name of the resource file holding the BMI LMS table. */
    private static final String BMI_FILE = "bmiage.csv";

    /** The name of the resource file holding the ear length for age LMS table. */
    private static final String EAR_LENGTH_FILE = "elage.csv";

    /** The name of the resource file holding the hand length for age LMS table. */
    private static final String HAND_LENGTH_FILE = "hlage.csv";

    /** The name of the resource file holding the palm length for age LMS table. */
    private static final String PALM_LENGTH_FILE = "plage.csv";

    /** The name of the resource file holding the foot length for age LMS table. */
    private static final String FOOT_LENGTH_FILE = "flage.csv";

    /** The name of the resource file holding the head circumference LMS table. */
    private static final String HC_FILE = "hcage.csv";

    /** The name of the resource file holding the height for age LMS table. */
    private static final String HEIGHT_FILE = "htage.csv";

    /** The name of the resource file holding the inner canthal distance LMS table. */
    private static final String ICD_FILE = "icdage.csv";

    /** The name of the resource file holding the interpupilary distance LMS table. */
    private static final String IPD_FILE = "ipdage.csv";

    /** The name of the resource file holding the outer canthal distance LMS table. */
    private static final String OCD_FILE = "ocdage.csv";

    /** The name of the resource file holding the palpebral fissure length LMS table. */
    private static final String PFL_FILE = "pflage.csv";

    /** The name of the resource file holding the weight for age LMS table. */
    private static final String WEIGHT_FILE = "wtage.csv";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * Triplet storing the median (M), the generalized coefficient of variation (S), and the power in the Box-Cox
     * transformation (L) values used to compute the percentile corresponding to a given value.
     */
    private static class LMS
    {
        /** L value, the power. */
        private double l;

        /** M value, the median. */
        private double m;

        /** S value, the generalized coefficient of variation. */
        private double s;

        /**
         * Constructor specifying all three values of the triplet.
         * 
         * @param l L value, the power
         * @param m M value, the median
         * @param s S value, the generalized coefficient of variation
         */
        public LMS(double l, double m, double s)
        {
            this.l = l;
            this.m = m;
            this.s = s;
        }

        @Override
        public String toString()
        {
            return "[" + this.l + ", " + this.m + ", " + this.s + "]";
        }
    }

    /** Table storing the BMI LMS triplets for each month of the normal development of boys. */
    private List<LMS> bmiForAgeBoys;

    /** Table storing the BMI LMS triplets for each month of the normal development of girls. */
    private List<LMS> bmiForAgeGirls;

    /** Table storing the ear length LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> earLengthForAge;

    /** Table storing the head circumference LMS triplets for each month of the normal development of boys. */
    private List<LMS> hcForAgeBoys;

    /** Table storing the hand length LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> handLengthForAge;

    /** Table storing the foot length LMS triplets for each month of the normal development of boys. */
    private List<LMS> footLengthForAgeBoys;

    /** Table storing the foot length LMS triplets for each month of the normal development of girls. */
    private List<LMS> footLengthForAgeGirls;

    /** Table storing the palm length LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> palmLengthForAge;

    /** Table storing the head circumference LMS triplets for each month of the normal development of girls. */
    private List<LMS> hcForAgeGirls;

    /** Table storing the height LMS triplets for each month of the normal development of boys. */
    private List<LMS> heightForAgeBoys;

    /** Table storing the height LMS triplets for each month of the normal development of girls. */
    private List<LMS> heightForAgeGirls;

    /** Table storing the inner canthal distance LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> icdForAge;

    /** Table storing the interpupilary distance LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> ipdForAge;

    /** Table storing the outer canthal distance LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> ocdForAge;

    /** Table storing the palpebral fissure length LMS triplets for each month of the normal development (both sexes). */
    private List<LMS> palpebralFissureLengthForAge;

    /** Table storing the weight LMS triplets for each month of the normal development of boys. */
    private List<LMS> weightForAgeBoys;

    /** Table storing the weight LMS triplets for each month of the normal development of girls. */
    private List<LMS> weightForAgeGirls;

    @Override
    public void initialize() throws InitializationException
    {
        this.bmiForAgeBoys = new ArrayList<LMS>(241);
        this.bmiForAgeGirls = new ArrayList<LMS>(241);
        this.earLengthForAge = new ArrayList<LMS>(241);
        this.handLengthForAge = new ArrayList<LMS>(241);
        this.palmLengthForAge = new ArrayList<LMS>(241);
        this.footLengthForAgeBoys = new ArrayList<LMS>(241);
        this.footLengthForAgeGirls = new ArrayList<LMS>(241);
        this.hcForAgeBoys = new ArrayList<LMS>(37);
        this.hcForAgeGirls = new ArrayList<LMS>(37);
        this.heightForAgeBoys = new ArrayList<LMS>(241);
        this.heightForAgeGirls = new ArrayList<LMS>(241);
        this.icdForAge = new ArrayList<LMS>(241);
        this.ipdForAge = new ArrayList<LMS>(241);
        this.ocdForAge = new ArrayList<LMS>(241);
        this.palpebralFissureLengthForAge = new ArrayList<LMS>(241);
        this.weightForAgeBoys = new ArrayList<LMS>(241);
        this.weightForAgeGirls = new ArrayList<LMS>(241);
        readData(BMI_FILE, this.bmiForAgeBoys, this.bmiForAgeGirls);
        readData(EAR_LENGTH_FILE, this.earLengthForAge, null);
        readData(HAND_LENGTH_FILE, this.handLengthForAge, null);
        readData(PALM_LENGTH_FILE, this.palmLengthForAge, null);
        readData(FOOT_LENGTH_FILE, this.footLengthForAgeBoys, this.footLengthForAgeGirls);
        readData(HC_FILE, this.hcForAgeBoys, this.hcForAgeGirls);
        readData(HEIGHT_FILE, this.heightForAgeBoys, this.heightForAgeGirls);
        readData(ICD_FILE, this.icdForAge, null);
        readData(IPD_FILE, this.ipdForAge, null);
        readData(OCD_FILE, this.ocdForAge, null);
        readData(PFL_FILE, this.palpebralFissureLengthForAge, null);
        readData(WEIGHT_FILE, this.weightForAgeBoys, this.weightForAgeGirls);
    }

    /**
     * Get the BMI percentile for the given weight and height.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param weightInKilograms the measured weight, in kilograms
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *        children, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getBMIPercentile(boolean male, int ageInMonths, double weightInKilograms, double heightInCentimeters)
    {
        LMS lms = getLMSForAge(male ? this.bmiForAgeBoys : this.bmiForAgeGirls, ageInMonths);
        double bmi = getBMI(weightInKilograms, heightInCentimeters);
        return valueToPercentile(bmi, lms);
    }

    /**
     * Compute the BMI (Body-Mass Index) for the given weigh and height. The formula is {@code weight / (height^2)}
     * multiplied by 10000 (to convert centimeters into meters).
     * 
     * @param weightInKilograms the measured weight, in kilograms
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *        children, in centimeters
     * @return the BMI value
     */
    public double getBMI(double weightInKilograms, double heightInCentimeters)
    {
        if (heightInCentimeters <= 0 || weightInKilograms <= 0) {
            return 0;
        }
        return weightInKilograms * 10000 / (heightInCentimeters * heightInCentimeters);
    }

    /**
     * Get the BMI that would correspond to the given BMI for age percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the BMI (in kilograms per square meter) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentileBMI(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(male ? this.bmiForAgeBoys : this.bmiForAgeGirls, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the ear length for age percentile for the given height and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param earLengthInCentimeters the measured ear length, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getEarLengthPercentile(boolean male, int ageInMonths, double earLengthInCentimeters)
    {
        LMS lms = getLMSForAge(this.earLengthForAge, ageInMonths);
        return valueToPercentile(earLengthInCentimeters, lms);
    }

    /**
     * Get the ear length that would correspond to the given height for age percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the ear length (in centimeters) that falls in the middle of the target percentile, with the exception of
     *         the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentileEarLength(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.earLengthForAge, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the height for age percentile for the given height and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param heightInCentimeters the measured length for children under 24 months, or measured height for older
     *        children, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getHeightPercentile(boolean male, int ageInMonths, double heightInCentimeters)
    {
        LMS lms = getLMSForAge(male ? this.heightForAgeBoys : this.heightForAgeGirls, ageInMonths);
        return valueToPercentile(heightInCentimeters, lms);
    }

    /**
     * Get the height that would correspond to the given height for age percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the height (in centimeters) that falls in the middle of the target percentile, with the exception of the
     *         open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentileHeight(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(male ? this.heightForAgeBoys : this.heightForAgeGirls, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the weight for age percentile for the given weight and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param weightInKilograms the measured weight, in kilograms
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getWeightPercentile(boolean male, int ageInMonths, double weightInKilograms)
    {
        LMS lms = getLMSForAge(male ? this.weightForAgeBoys : this.weightForAgeGirls, ageInMonths);
        return valueToPercentile(weightInKilograms, lms);
    }

    /**
     * Get the weight that would correspond to the given weight for age percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the weight (in kilograms) that falls in the middle of the target percentile, with the exception of the
     *         open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentileWeight(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(male ? this.weightForAgeBoys : this.weightForAgeGirls, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the head circumference for age percentile for the given head circumference and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param headCircumferenceInCentimeters the measured head circumference, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getHCPercentile(boolean male, int ageInMonths, double headCircumferenceInCentimeters)
    {
        LMS lms = getLMSForAge(male ? this.hcForAgeBoys : this.hcForAgeGirls, ageInMonths);
        return valueToPercentile(headCircumferenceInCentimeters, lms);
    }

    /**
     * Get the head circumference that would correspond to the given HC for age percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the head circumference (in centimeters) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentileHC(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(male ? this.hcForAgeBoys : this.hcForAgeGirls, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the inner canthal distance percentile for the given distance and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param innerCanthalDistanceInCentimeters the measured inner canthal distance, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getInnerCanthalDistancePercentile(boolean male, int ageInMonths, double innerCanthalDistanceInCentimeters)
    {
        LMS lms = getLMSForAge(this.icdForAge, ageInMonths);
        return valueToPercentile(innerCanthalDistanceInCentimeters, lms);
    }

    /**
     * Get the inner canthal distance that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the inner canthal distance (in centimeters) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentileInnerCanthalDistance(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.icdForAge, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the interpupilary distance percentile for the given distance and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param interpupilaryDistanceInCentimeters the measured inner canthal distance, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getInterpupilaryDistancePercentile(boolean male, int ageInMonths,
        double interpupilaryDistanceInCentimeters)
    {
        LMS lms = getLMSForAge(this.ipdForAge, ageInMonths);
        return valueToPercentile(interpupilaryDistanceInCentimeters, lms);
    }

    /**
     * Get the interpupilary distance that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the interpupilary distance (in centimeters) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentileInterpupilaryDistance(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.ipdForAge, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the outer canthal distance percentile for the given distance and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param outerCanthalDistanceInCentimeters the measured outer canthal distance, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getOuterCanthalDistancePercentile(boolean male, int ageInMonths, double outerCanthalDistanceInCentimeters)
    {
        LMS lms = getLMSForAge(this.ocdForAge, ageInMonths);
        return valueToPercentile(outerCanthalDistanceInCentimeters, lms);
    }

    /**
     * Get the outer canthal distance that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the outer canthal distance (in centimeters) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentileOuterCanthalDistance(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.ocdForAge, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the palpebral fissure length percentile for the given length and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param palpebralFissureLengthInCentimeters the measured fissure length, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getPalpebralFissureLengthPercentile(boolean male, int ageInMonths,
        double palpebralFissureLengthInCentimeters)
    {
        LMS lms = getLMSForAge(this.palpebralFissureLengthForAge, ageInMonths);
        return valueToPercentile(palpebralFissureLengthInCentimeters, lms);
    }

    /**
     * Get the palpebral fissure length that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the palpebral fissure length (in centimeters) that falls in the middle of the target percentile, with the
     *         exception of the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25,
     *         respectively 99.75 percentage is returned
     */
    public double getPercentilePalpebralFissureLength(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.palpebralFissureLengthForAge, ageInMonths);
        if (lms == null) {
            return -1;
        }
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the hand length percentile for the given length and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param handLengthInCentimeters the measured hand length, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getHandLengthPercentile(boolean male, int ageInMonths, double handLengthInCentimeters)
    {
        LMS lms = getLMSForAge(this.handLengthForAge, ageInMonths);
        return valueToPercentile(handLengthInCentimeters, lms);
    }

    /**
     * Get the hand length that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the hand length (in centimeters) that falls in the middle of the target percentile, with the exception of
     *         the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentileHandLength(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.handLengthForAge, ageInMonths);
        if (lms == null) {
            return -1;
        }
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the palm length percentile for the given length and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param palmLengthInCentimeters the measured palm length, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getPalmLengthPercentile(boolean male, int ageInMonths, double palmLengthInCentimeters)
    {
        LMS lms = getLMSForAge(this.palmLengthForAge, ageInMonths);
        return valueToPercentile(palmLengthInCentimeters, lms);
    }

    /**
     * Get the palm length that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the palm length (in centimeters) that falls in the middle of the target percentile, with the exception of
     *         the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentilePalmLength(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge(this.palmLengthForAge, ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Get the foot length percentile for the given length and age.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param footLengthInCentimeters the measured foot length, in centimeters
     * @return a number between 0 and 100 (inclusive) specifying the percentile of this measurement
     */
    public int getFootLengthPercentile(boolean male, int ageInMonths, double footLengthInCentimeters)
    {
        LMS lms = getLMSForAge((male ? this.footLengthForAgeBoys : this.footLengthForAgeGirls), ageInMonths);
        return valueToPercentile(footLengthInCentimeters, lms);
    }

    /**
     * Get the foot length that would correspond to the given percentile.
     * 
     * @param male {@code true} for boys, {@code false} for girls
     * @param ageInMonths the age of the measurement, in months
     * @param targetPercentile a number between 0 and 100 (inclusive) specifying the target percentile
     * @return the foot length (in centimeters) that falls in the middle of the target percentile, with the exception of
     *         the open ended 0 and 100 percentiles, for which the value corresponding to the 0.25, respectively 99.75
     *         percentage is returned
     */
    public double getPercentileFootLength(boolean male, int ageInMonths, int targetPercentile)
    {
        LMS lms = getLMSForAge((male ? this.footLengthForAgeBoys : this.footLengthForAgeGirls), ageInMonths);
        return percentileToValue(targetPercentile, lms.m, lms.l, lms.s);
    }

    /**
     * Convert the percentile number into a string grossly describing the value.
     * 
     * @param percentile a number between 0 and 100
     * @return the percentile description
     */
    public String getFuzzyValue(int percentile)
    {
        String returnValue = "normal";
        if (percentile <= 3) {
            returnValue = "extreme-below-normal";
        } else if (percentile <= 10) {
            returnValue = "below-normal";
        } else if (percentile >= 97) {
            returnValue = "extreme-above-normal";
        } else if (percentile >= 90) {
            returnValue = "above-normal";
        }
        return returnValue;
    }

    /**
     * Compute the percentile corresponding to a given absolute value, compared to a normal distribution specified by
     * the given Box-Cox triplet.
     * 
     * @param x the absolute value to fit into the normal distribution
     * @param lms the parameters defining the normal distribution
     * @return a number between 0 and 100 (inclusive) specyfing the percentile of this measurement
     */
    public int valueToPercentile(double x, LMS lms)
    {
        if (lms == null) {
            return -1;
        }
        return valueToPercentile(x, lms.m, lms.l, lms.s);
    }

    /**
     * Compute the percentile corresponding to a given absolute value, compared to a normal distribution specified by
     * the given Box-Cox triplet.
     * 
     * @param x the absolute value to fit into the normal distribution
     * @param m the M value, the median
     * @param l the L value, the power
     * @param s the S value, the generalized coefficient of variation
     * @return a number between 0 and 100 (inclusive) specyfing the percentile of this measurement
     */
    public int valueToPercentile(double x, double m, double l, double s)
    {
        double z = (l != 0) ? ((Math.pow(x / m, l) - 1) / (l * s)) : (Math.log(x / m) / s);
        try {
            double p = NORMAL.cumulativeProbability(z) * 100;
            return (int) Math.round(p);
        } catch (MathException ex) {
            return 0;
        }
    }

    /**
     * Compute the percentile corresponding to a given absolute value, compared to a normal distribution specified by
     * the given Box-Cox triplet.
     * 
     * @param x the absolute value to fit into the normal distribution
     * @param m the M value, the median
     * @param l the L value, the power
     * @param s the S value, the generalized coefficient of variation
     * @return a number between 0 and 100 (inclusive) specyfing the percentile of this measurement
     */
    public double percentileToValue(int percentile, double m, double l, double s)
    {
        double correctedPercentile = percentile;
        if (percentile <= 0) {
            correctedPercentile = 0.25;
        } else if (percentile >= 100) {
            correctedPercentile = 99.75;
        }
        try {
            double z = NORMAL.inverseCumulativeProbability(correctedPercentile / 100.0);
            double x = (l != 0) ? Math.pow(z * l * s + 1, 1 / l) * m : Math.exp(z * s) * m;
            return x;
        } catch (MathException ex) {
            return 0;
        }
    }

    /**
     * Read the LMS triplets for a specific feature from a resource file.
     * 
     * @param filename the name of the resource file from which to read the data
     * @param boysList the list where to place the triplets for boys
     * @param girlsList the list where to place the triplets for girls
     */
    private void readData(String filename, List<LMS> boysList, List<LMS> girlsList)
    {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                .getResourceAsStream(filename), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            // This should never happen, UTF-8 is always present
            in = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader()
                .getResourceAsStream(filename)));
        }
        String line;
        try {
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 5) {
                    continue;
                }
                int month = Integer.parseInt(tokens[1], 10);
                double l = Double.parseDouble(tokens[2]);
                double m = Double.parseDouble(tokens[3]);
                double s = Double.parseDouble(tokens[4]);
                LMS lms = new LMS(l, m, s);
                if ("1".equals(tokens[0])) {
                    while (month >= boysList.size()) {
                        boysList.add(null);
                    }
                    boysList.set(month, lms);
                } else {
                    while (month >= girlsList.size()) {
                        girlsList.add(null);
                    }
                    girlsList.set(month, lms);
                }
            }
        } catch (IOException ex) {
            // This shouldn't happen
            this.logger.error("Failed to read data table [{}]: {}",
                new Object[] {filename, ex.getMessage(), ex});
        }
    }

    private LMS getLMSForAge(List<LMS> list, int ageInMonths)
    {
        if (ageInMonths < 0) {
            return null;
        } else if (ageInMonths >= list.size()) {
            return list.get(list.size() - 1);
        }
        LMS result;
        result = list.get(ageInMonths);
        if (result == null) {
            int lowerAge = ageInMonths - 1;
            while (lowerAge >= 0 && list.get(lowerAge) == null) {
                --lowerAge;
            }
            if (lowerAge < 0) {
                return null;
            }
            int upperAge = ageInMonths + 1;
            while (upperAge < list.size() && list.get(upperAge) == null) {
                ++upperAge;
            }
            LMS lowerLMS = list.get(lowerAge);
            LMS upperLMS = list.get(upperAge);
            double delta = ((double) ageInMonths - lowerAge) / (upperAge - lowerAge);
            result = new LMS(lowerLMS.l + (upperLMS.l - lowerLMS.l) * delta,
                lowerLMS.m + (upperLMS.m - lowerLMS.m) * delta,
                lowerLMS.s + (upperLMS.s - lowerLMS.s) * delta);
        }
        return result;
    }
}
