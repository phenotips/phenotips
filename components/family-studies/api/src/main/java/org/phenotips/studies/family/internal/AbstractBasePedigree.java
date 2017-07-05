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
package org.phenotips.studies.family.internal;

import org.phenotips.studies.family.Pedigree;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;

/**
 * A common base for classes supporting different pedigree formats.
 *
 * Common functionality (mostly related to images, which have the same format) is placed in this class.
 *
 * @version $Id$
 * @since 1.3M4
 */
public abstract class AbstractBasePedigree implements Pedigree
{
    protected JSONObject data;

    protected String image = "";

    /**
     * Create a new pedigree using given data and image.
     *
     * @param data pedigree data
     * @param image SVG 'image'
     */
    public AbstractBasePedigree(JSONObject data, String image)
    {
        if (data == null || data.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.data = data;
        this.image = image;
    }

    @Override
    public JSONObject getData()
    {
        return this.data;
    }

    @Override
    public String getImage(String highlightCurrentPatientId)
    {
        return getImage(highlightCurrentPatientId, 0, 0);
    }

    @Override
    public String getImage(String highlightCurrentPatientId, int width, int height)
    {
        String svg = SvgUpdater.setCurrentPatientStylesInSvg(this.image, highlightCurrentPatientId);
        if (width > 0) {
            svg = SvgUpdater.setSVGWidth(svg, width);
        }
        if (height > 0) {
            svg = SvgUpdater.setSVGHeight(svg, height);
        }
        return svg;
    }

    @Override
    public String getProbandId()
    {
        return getProbandInfo().getLeft();
    }

    @Override
    public String getProbandPatientLastName()
    {
        String lastName = getProbandInfo().getRight();
        if (StringUtils.isBlank(lastName)) {
            return null;
        }
        return lastName;
    }

    @Override
    public void removeLink(String linkedPatientId)
    {
        // update SVG
        this.image = SvgUpdater.removeLink(this.image, linkedPatientId);

        // update JSON
        removeLinkFromPedigreeJSON(linkedPatientId);
    }

    /**
     * @return a pair <ProbandId, ProbandLastname>
     */
    protected abstract Pair<String, String> getProbandInfo();

    /**
     * Removes all links to the given PhenoTips patient form the pedigree JSON.
     */
    protected abstract void removeLinkFromPedigreeJSON(String linkedPatientId);
}
