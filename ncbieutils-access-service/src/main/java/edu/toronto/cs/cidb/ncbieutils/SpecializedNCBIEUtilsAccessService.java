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
package edu.toronto.cs.cidb.ncbieutils;

import java.util.List;
import java.util.Map;

public class SpecializedNCBIEUtilsAccessService extends NCBIEUtilsAccessService
{
    @Override
    public String getSuggestions(String query)
    {
        return super.getSuggestions(query);
    }

    @Override
    public String getName(String id)
    {
        return super.getName(id);
    }

    @Override
    public Map<String, String> getNames(List<String> idList)
    {
        return super.getNames(idList);
    }

    @Override
    public String getCorrectedQuery(String query)
    {
        return super.getCorrectedQuery(query);
    }

    @Override
    public List<String> getMatches(String query)
    {
        return super.getMatches(query);
    }

    @Override
    public String getSummaries(List<String> idList)
    {
        return super.getSummaries(idList);
    }

    @Override
    public SpecializedNCBIEUtilsAccessService get(final String name)
    {
        return null;
    }
}
