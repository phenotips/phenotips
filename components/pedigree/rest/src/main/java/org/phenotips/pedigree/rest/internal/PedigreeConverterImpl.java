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
package org.phenotips.pedigree.rest.internal;


import org.json.JSONArray;
import org.json.JSONObject;
import org.phenotips.pedigree.rest.PedigreeConverter;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.rest.XWikiResource;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resource to convert pedigree PED files to PhenoTips pedigree SimpleJSON format.
 *
 * @version $Id$
 * @since 1.4m2
 */
@Component
@Named("org.phenotips.pedigree.rest.internal.PedigreeConverterImpl")
@Singleton
public class PedigreeConverterImpl extends XWikiResource implements PedigreeConverter{
    @Inject
    private Logger logger;

    @Override
    public Response convertPedigree(String ped) {
        if (ped == null) {
            this.logger.error("PED file is empty");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONArray json = conversionTool(ped);
        if (json == null) {
            this.logger.error("PED file is not compatible");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        //Does not return as a file type with family ID
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

     /* PED format:
     * (from http://pngu.mgh.harvard.edu/~purcell/plink/data.shtml#ped)
     *   Family ID
     *   Individual ID
     *   Paternal ID
     *   Maternal ID`
     *   Sex (1=male; 2=female; other=unknown)
     *   Phenotype
     *
     *   Phenotype, by default, should be coded as:
     *      -9 unknown
     *       0 unknown
     *       1 unaffected
     *       2 affected
     *       */
    // Converts PED to JSON format
    private JSONArray conversionTool(String ped) {

        JSONArray json = new JSONArray();
        List<String> inputLine = new LinkedList<>();

        //Separate the PED text file by lines
        Matcher m = Pattern.compile("[^\r\n]+", Pattern.MULTILINE | Pattern.DOTALL).matcher(ped);
        while(m.find()) {
            inputLine.add(m.group());
        }

        // Populate the data to the variables from the PED in each line
        for(String line : inputLine) {
            String[] dataSplit = line.split("\\s+");
            //Check if valid by 6 info pieces
            if (dataSplit.length != 6) {
                return null; //Throw error, not valid PED
            }
            //Start populating the JSON here
            JSONObject jsonObject = new JSONObject().put("id", dataSplit[1]);
            if(!dataSplit[2].equals("0")) {
                jsonObject.put("father", dataSplit[2]);
            }
            if(!dataSplit[3].equals("0")) {
                jsonObject.put("mother", dataSplit[3]);
            }
            if(dataSplit[4].equals("1")) {
                jsonObject.put("sex", "male");
            }
            else if(dataSplit[4].equals("2")) {
                jsonObject.put("sex", "female");
            }
            else {
                jsonObject.put("sex", "unknown");
            }
            if (dataSplit[5].equals("1")) {
                jsonObject.put("carrierStatus", "unaffected").put("disorders", new JSONArray().put("unaffected"));
            }
            else if (dataSplit[5].equals("2")) {
                jsonObject.put("carrierStatus", "affected").put("disorders", new JSONArray().put("affected"));
            }
            else {
                // Data missing or not documented correctly in the PED file will be considered "unknown"
                jsonObject.put("carrierStatus", "unknown").put("disorders", new JSONArray().put("unknown"));
            }
            json.put(jsonObject);
        }

        return json;
    }
}
