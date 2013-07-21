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
package org.phenotips.data.similarity.internal.mocks;

import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;

import java.util.Collections;
import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Simple mock for a patient feature, responding with pre-specified values.
 * 
 * @version $Id$
 */
public class MockFeature implements Feature
{
    private final String id;

    private final String name;

    private final String type;

    private final boolean present;

    private final Map<String, FeatureMetadatum> meta;

    public MockFeature(String id, String name, String type, boolean present)
    {
        this(id, name, type, Collections.<String, FeatureMetadatum> emptyMap(), present);
    }

    public MockFeature(String id, String name, String type, Map<String, FeatureMetadatum> meta, boolean present)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.meta = meta;
        this.present = present;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getType()
    {
        return this.type;
    }

    @Override
    public boolean isPresent()
    {
        return this.present;
    }

    @Override
    public Map<String, ? extends FeatureMetadatum> getMetadata()
    {
        return this.meta;
    }

    @Override
    public JSONObject toJSON()
    {
        return null;
    }
}
