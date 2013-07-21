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

import org.phenotips.data.FeatureMetadatum;

import net.sf.json.JSONObject;

/**
 * Simple mock for a feature metadatum, responding with pre-specified values.
 * 
 * @version $Id$
 */
public class MockFeatureMetadatum implements FeatureMetadatum
{
    private final String id;

    private final String name;

    private final String type;

    public MockFeatureMetadatum(String id, String name, String type)
    {
        this.id = id;
        this.name = name;
        this.type = type;
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
    public JSONObject toJSON()
    {
        return null;
    }
}
