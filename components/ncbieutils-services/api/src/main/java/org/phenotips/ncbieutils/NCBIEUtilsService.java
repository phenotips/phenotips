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
package org.phenotips.ncbieutils;

import java.util.List;
import java.util.Map;

/**
 * Script service exposing a few services provided by the online NCBI Entrez Utilities webserver. Generic service for
 * accessing the NCBI Entrez Utilities server. This is not exposed directly, but instances for specific databases can be
 * obtained through {@link NCBIEUtilsAccessService#get(String)}, available to scripts using
 * {@code $services.ncbi.get('dbname')}.
 *
 * @version $Id$
 * @since 1.0M8 (functionality available since 1.0M1)
 */
public interface NCBIEUtilsService
{
    List<Map<String, Object>> getSuggestions(final String query);

    List<Map<String, Object>> getSuggestions(final String query, final int rows, final int start);

    String getSuggestionsXML(final String query);

    String getSuggestionsXML(final String query, final int rows, final int start);

    String getName(String id);

    Map<String, String> getNames(List<String> idList);

    String getCorrectedQuery(String query);

    List<String> getMatches(final String query, final int rows, final int start);

    List<Map<String, Object>> getSummaries(List<String> idList);
}
