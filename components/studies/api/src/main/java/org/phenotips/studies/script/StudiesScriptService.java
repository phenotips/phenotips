package org.phenotips.studies.script;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Named;
import javax.inject.Singleton;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @version $Id$
 */

@Component
@Named("studies")
@Singleton
public class StudiesScriptService implements ScriptService
{

    public String searchStudies(String input, int resultsLimit, boolean returnAsJSON)
    {
        JSONArray studiesArray = new JSONArray();

        JSONObject studyJson = new JSONObject();
        studyJson.put("id", "s1");
        studyJson.put("textSummary", "s1 text");
        studiesArray.add(studyJson);

        studyJson = new JSONObject();
        studyJson.put("id", "s4");
        studyJson.put("textSummary", "s4 text");
        studiesArray.add(studyJson);

        JSONObject result = new JSONObject();
        result.put("matchedStudies", studiesArray);
        return result.toString();
    }
}
