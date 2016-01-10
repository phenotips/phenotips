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
package org.phenotips.data;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about the medication taken by a patient.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Unstable
public final class Medication
{
    /** The XClass used for storing medication data. */
    public static final EntityReference CLASS_REFERENCE =
        new EntityReference("MedicationDataClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * @see Medication#getName()
     */
    public static final String NAME = "name";

    /**
     * @see Medication#getGenericName()
     */
    public static final String GENERIC_NAME = "genericName";

    /**
     * @see Medication#getDose()
     */
    public static final String DOSE = "dose";

    /**
     * @see Medication#getFrequency()
     */
    public static final String FREQUENCY = "frequency";

    /**
     * @see Medication#getDuration()
     */
    public static final String DURATION = "duration";

    /**
     * @see Medication#getEffect()
     */
    public static final String EFFECT = "effect";

    /**
     * @see Medication#getNotes()
     */
    public static final String NOTES = "notes";

    /** The brand name of the medicine. */
    private final String name;

    /** The generic name of the main ingredient. */
    private final String genericName;

    /** The dose administered. */
    private final String dose;

    /** The frequency of the doses. */
    private final String frequency;

    /** The amount of time that this medicine has been given. */
    private final Period duration;

    /** The effect observed for this medicine. */
    private final MedicationEffect effect;

    /** Custom notes about the medication. */
    private final String notes;

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(Medication.class);

    /**
     * Basic constructor receiving all the required information.
     *
     * @param name see {@link #getName()}, may be {@code null}
     * @param genericName see {@link #getGenericName()}, may be {@code null}
     * @param dose see {@link #getDose()}, may be {@code null}
     * @param frequency see {@link #getFrequency()}, may be {@code null}
     * @param duration see {@link #getDuration()}, may be {@code null}
     * @param effect see {@link #getEffect()}, may be {@code null}
     * @param notes see {@link #getNotes()}, may be {@code null}
     */
    public Medication(final String name,
        final String genericName,
        final String dose,
        final String frequency,
        final Period duration,
        final String effect,
        final String notes)
    {
        this.name = name;
        this.genericName = genericName;
        this.dose = dose;
        this.frequency = frequency;
        this.duration = duration;
        if (StringUtils.isBlank(effect)) {
            this.effect = null;
        } else {
            MedicationEffect e = null;
            try {
                e = MedicationEffect.fromString(effect);
            } catch (IllegalArgumentException ex) {
                // Shouldn't happen
            }
            this.effect = e;
        }
        this.notes = notes;
    }

    /**
     * Constructor parsing back a JSON, in the same format as {@link #toJSON()} produces.
     *
     * @param json a JSON object in the format produced by {@link #toJSON()}; must not be {@code null}
     * @throws IllegalArgumentException if {@code json} is {@code null}
     */
    public Medication(JSONObject json)
    {
        if (json == null) {
            throw new IllegalArgumentException("The json parameter must not be null");
        }
        this.name = StringUtils.defaultIfBlank(json.optString(NAME), null);
        this.genericName = StringUtils.defaultIfBlank(json.optString(GENERIC_NAME), null);
        this.dose = StringUtils.defaultIfBlank(json.optString(DOSE), null);
        this.frequency = StringUtils.defaultIfBlank(json.optString(FREQUENCY), null);
        String effectStr = json.optString(EFFECT);
        if (StringUtils.isBlank(effectStr)) {
            this.effect = null;
        } else {
            MedicationEffect e = null;
            try {
                e = MedicationEffect.fromString(effectStr);
            } catch (IllegalArgumentException ex) {
                // Shouldn't happen
                this.logger.info("Unknown medication effect: {}", effectStr);
            }
            this.effect = e;
        }
        String durationStr = json.optString(DURATION);
        if (StringUtils.isBlank(durationStr)) {
            this.duration = null;
        } else {
            this.duration = ISOPeriodFormat.standard().parsePeriod("P" + durationStr);
        }
        this.notes = StringUtils.defaultIfBlank(json.optString(NOTES), null);
    }

    /**
     * Serialize as JSON, in the following format.
     *
     * <pre>
     * {
     *   "name": "Nurofen",
     *   "genericName: "ibuprofen",
     *   "dose": "200mg",
     *   "frequency": "8h",
     *   "duration": "2Y6M",
     *   "effect": "slightImprovement",
     *   "notes": "Makes the patient's headaches a lot more bearable"
     * }
     * </pre>
     *
     * @return a JSON object with all the specified fields; any missing/empty field is not set at all in the output
     */
    public JSONObject toJSON()
    {
        JSONObject result = new JSONObject();
        if (StringUtils.isNotBlank(this.name)) {
            result.put(NAME, this.name);
        }
        if (StringUtils.isNotBlank(this.genericName)) {
            result.put(GENERIC_NAME, this.genericName);
        }
        if (StringUtils.isNotBlank(this.dose)) {
            result.put(DOSE, this.dose);
        }
        if (StringUtils.isNotBlank(this.frequency)) {
            result.put(FREQUENCY, this.frequency);
        }
        if (this.duration != null && !Period.ZERO.equals(this.duration)) {
            result.put(DURATION, this.duration.toString().substring(1));
        }
        if (this.effect != null) {
            result.put(EFFECT, this.effect.toString());
        }
        if (StringUtils.isNotBlank(this.notes)) {
            result.put(NOTES, this.notes);
        }
        return result;
    }

    /**
     * The brand name of the medicine.
     *
     * @return a string, may be {@code null} or empty
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * The generic name of the main ingredient.
     *
     * @return a string, may be {@code null} or empty
     */
    public String getGenericName()
    {
        return this.genericName;
    }

    /**
     * The dose administered.
     *
     * @return a string, may be {@code null} or empty
     */
    public String getDose()
    {
        return this.dose;
    }

    /**
     * The frequency of the doses.
     *
     * @return a string, may be {@code null} or empty
     */
    public String getFrequency()
    {
        return this.frequency;
    }

    /**
     * The amount of time that this medicine has been given.
     *
     * @return a period, may be {@code null}
     */
    public Period getDuration()
    {
        return this.duration;
    }

    /**
     * The effect observed for this medicine.
     *
     * @return an enum instance, may be {@code null}
     */
    public MedicationEffect getEffect()
    {
        return this.effect;
    }

    /**
     * Custom notes about the medication.
     *
     * @return a string, may be {@code null} or empty
     */
    public String getNotes()
    {
        return this.notes;
    }
}
