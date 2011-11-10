<%@ page import="java.util.Collection" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Enumeration" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<%@ include file="resources.jsp" %>
<%= displayTitle("Clinical data form validation") %>
</head>

<body>
<%= displayContentTitle("Clinical data form demo") %>

<%!
String parameterName = "";
boolean submitted = false;
boolean phenotypeGiven = false;

boolean parameterDisplayed = false;
String RESTRICTION_FIELD_MARKER = "_category";

%><%!

boolean hasErrors = false;

LinkedHashMap<String, String> specialParameters = new LinkedHashMap<String, String>(){{
  put("last_name", "Last name");
  put("first_name", "First name");
  put("date_of_birth", "Date of birth");
  put("gender", "Gender");
}};

public boolean ignoreParameter(String paramName) {
  return paramName == null || paramName.endsWith("__suggested") || paramName.equals(RESTRICTION_FIELD_MARKER);
}

public boolean isTextValueValid(String value) {
  return value != null && !"".equals(value.trim());
}

public String handleDateParameter(String paramName, String value) {
  if (!"date_of_birth".equals(paramName)) {
    return null;
  }
  String errorMessage = null;
  if (!isTextValueValid(value)) {
    errorMessage = "This parameter is required."; 
  } else if (!value.matches("^(0[1-9]|1[012])[/](0[1-9]|[12][0-9]|3[01])[/](19|20)\\d\\d$")) {
    errorMessage = "The date format must be mm/dd/yyyy."; 
  } else {
    String parts[] = value.split("/");
    int month = Integer.parseInt(parts[0]);
    int day = Integer.parseInt(parts[1]);
    int year = Integer.parseInt(parts[2]);
    if (
      ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) ||
      (month == 2 && (day > 29 || (year % 4 != 0 || year % 100 == 0 && year % 400 != 0) && day > 28)) 
      ) {
      errorMessage = "This is not a valid date.";
    }
  }
  if (errorMessage != null) {
    hasErrors = true;
  }
  return displaySubmittedParameter(paramName, value, errorMessage);
}

public String handleGenderParameter(String paramName, String value) {
  if (!"gender".equals(paramName)) {
    return null;
  }
  String errorMessage = null;
  if (!"male".equals(value) && !"female".equals(value)) {
    hasErrors = true;
    errorMessage = "The value of this parameter must be either 'male' or 'female'."; 
  }
  return displaySubmittedParameter(paramName, value, errorMessage);
}

public String displaySubmittedParameter(String paramName, String value, String errorMessage) {
  if (!isTextValueValid(value)) {
    value = "&ndash; <span class='hint'> (Not provided)</span>";
  }
  String valueValidationClass = "";
  if (errorMessage != null) {
    valueValidationClass = "wrongValue";
  } else {
    errorMessage = "";
  }
  return "<h2 class='" + valueValidationClass + " param-name'>" + specialParameters.get(paramName) + "</h2><div class='hint'>" + errorMessage + "</div><div>" + value + "</div>";
}
%><%
for(Enumeration e = request.getParameterNames(); e.hasMoreElements(); ){
  if(!ignoreParameter((String)e.nextElement())) {
    submitted = true;
    break;
  }
}
if (submitted) {%>
  <div class="emphasized-box warning">The data was NOT saved anywhere. This is just a proof of concept. 
                                      <a href="clinical-info.jsp">Go back to the form &laquo;</a>
  </div>
<% Object[] paramNames = specialParameters.keySet().toArray();

   for (int i = 0; i < paramNames.length; ++i) {
     parameterName = ((String)paramNames[i]);
     String value = request.getParameter(parameterName);
     String output = handleDateParameter(parameterName, value);
     if (output == null) {
       output = handleGenderParameter(parameterName, value);
     }
     if (output == null) {
       output = displaySubmittedParameter(parameterName, value, isTextValueValid(value)?null:"This parameter is required");
     }
     %><%= output %><%
   }
   
   for(Enumeration e = request.getParameterNames(); e.hasMoreElements(); ){
     parameterName = (String)e.nextElement();
     if(!ignoreParameter(parameterName) && !specialParameters.containsKey(parameterName)) {
       String[] vals = request.getParameterValues(parameterName);
       parameterDisplayed = false;
       for (int i = 0; i < vals.length; i++) {
         if (vals[i] == "") {
           continue;
         }
	 phenotypeGiven = true;
         if (!parameterDisplayed) {
%>
  <div><h2 class="param-name"><%= parameterName %></h2>
  <ul>
<%          parameterDisplayed = true;
         }
%>
    <li><%= vals[i] %></li>
<%     }
       if (parameterDisplayed) {
%>
  </ul></div>
<%     }
     }
   }
   if (!phenotypeGiven) {
     hasErrors = true;%>
     <div class="error">No phenotype provided for this patient.</div>  
 <%}
   if (hasErrors) {%>
     <div class="error">The data is invalid and/or incomplete.
     Please <a href="clinical-info.jsp">go back to the form</a> and enter the required data.</div>
   <%}
} else {
  response.sendRedirect("clinical-info.jsp");
}%>
</body>
</html>
