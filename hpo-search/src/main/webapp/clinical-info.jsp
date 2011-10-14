<%@ page import="java.util.Collection" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.Enumeration" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
<%@ include file="resources.jsp" %>
<%= displayTitle("Clinical data form") %>
</head>

<body>
<%= displayContentTitle("Clinical data form") %>

<%
String parameterName = "";
boolean submitted = false;
boolean parameterDisplayed = false;
for(Enumeration e = request.getParameterNames(); e.hasMoreElements(); ){
   parameterName = (String)e.nextElement();
   if(parameterName != null && !parameterName.endsWith("__suggested") && request.getParameter(parameterName) != null) {
     String[] vals = request.getParameterValues(parameterName);
     parameterDisplayed = false;
     for (int i = 0; i < vals.length; i++) {
        if (vals[i] == "") {
          continue;
        }
        if (!submitted) {
%>
  <div class="col-3">
<%      }
        if (!parameterDisplayed) {
%>
  <div><h2 class="param-name"><%= parameterName %></h2>
  <ul>
<%          submitted = true;
            parameterDisplayed = true;
        }
%>
    <li><%= vals[i] %></li>
<%     }
     if (parameterDisplayed) {
%>
  </ul></div>
<%  }
  }
}
if (submitted) {%>
</div>
<%} else {%>

<form action="clinical-info.jsp" method="post">
<fieldset class="twothird-width clear patient-info">
  <legend>Patient Information</legend>

  <fieldset>
  <label class="section" for="last_name">Patient name:</label>
  <div class="half-width">
    <label for="last_name" class="hint">Last name</label>
    <input type="text" name="last_name" id="last_name"/>
  </div>
  <div class="half-width">
    <label for="first_name" class="hint">First name</label>
    <input type="text" name="first_name" id="first_name"/>
  </div>
  </fieldset>
  <fieldset>
  <div class="half-width clear">
    <label class="section" for="date_of_birth">Date of birth: <span class="hint">(mm/dd/yyyy)</span></label>
    <input type="text" name="date_of_birth" id="date_of_birth"/>
  </div>
  </fieldset>
  <fieldset>
  <div class="half-width clear">
    <label>Gender: </label>
    <label for="gender_male"><input type="radio" name="gender" id="gender_male" value="male"/>Male</label> 
    <label for="gender_female"><input type="radio" name="gender" id="gender_female" value="female"/>Female</label>
  </div>
  </fieldset>
</fieldset>

<fieldset class="clinical-info">
  <legend>Clinical Information</legend>

  <fieldset class="group-other">
    <legend class="section">Quick phenotype search</legend>
    <label for="quick-phenotype-search">Enter a free text and choose among suggested ontology terms</label>
    <input type='text' name='phenotype' class='suggested multi suggest-hpo fullFormCheck' value='' size='16' id='quick-phenotype-search'/>
  </fieldset>

  <div class="col-3">
<%!
public boolean isHPId(String id)
{
  return id.matches("[0-9]{7}");
}
public boolean isHPOther(String id)
{
  return id.equals("_other");
}
public boolean isNonHPCheckBox(String id)
{
  return !isHPOther(id) && id.startsWith("_c_");
}
public boolean isNonHPInput(String id)
{
  return !isHPOther(id) && id.startsWith("_i_");
}
public boolean isSubsection(String id)
{
  return !isHPId(id) && !id.startsWith("_");
}

public String handleSection(LinkedHashMap<String, Object> sectionData)
{
  String result = "";
  Object[] fieldIds = sectionData.keySet().toArray();
  for (int i = 0; i < fieldIds.length; ++i) {
    result += handleField((String)fieldIds[i], sectionData);
  }
  return result;
}
public String handleField(String id, LinkedHashMap<String, Object> data)
{
  String name = "phenotype";
  Object label = (data.get(id) == null ? id : data.get(id));
  if (isHPId(id)) {
    return generateCheckBox(name, "HP:" + id, (String)label);
  } else if (isHPOther(id)) {
    return generateInput(name, (String)label, true);
  } else if (isNonHPCheckBox(id)) {
    return generateCheckBox(name, id.substring(3), (String)label);
  } else if (isNonHPInput(id)) {
    return generateInput(id.substring(3), (String)label, false);
  } else if (isSubsection(id)) {
    return "<label class='section'>" + id + "</label><div class='subsection'>" + handleSection((LinkedHashMap<String, Object>)data.get(id)) + "</div>";
  }
  return "";
}

public String generateCheckBox(String name, String value, String label)
{
  String id = name + (value.startsWith("_") ? "" : "_") + value;
  return "<label for='" + id + "'><input type='checkbox' name='" + name + "' id='" + id + "' value='" + value + "'/>" + label + "</label><br/>";
}
public String generateInput(String name, String label, boolean suggested)
{
  String result = "";
  String id = name + "_" + Math.random();
  String displayedLabel = (suggested ? "Other" : label);
  if (displayedLabel.matches("^\\(.*\\)$")) {
    displayedLabel = "<span class='hint'>" + displayedLabel + "</span>";
  } else {
    displayedLabel += ":";
  }
  result = "<label for='" + id + "'" + (suggested ? " class='label-other-" + name + "'" : "") + ">" + displayedLabel + "</label>";
  if (suggested) {
    result +="<p class='hint'>(enter a free text and choose among suggested ontology terms)</p>";
  }
  result +="<input type='text' name='" + name + "'" +(suggested ? "' class='suggested multi suggest-hpo generateCheckboxes'" : "") + " value='' size='16' id='" + id + "'/>";
  return result;
}
%>

<%
  LinkedHashMap<String, LinkedHashMap<String, Object>> sections = new LinkedHashMap<String, LinkedHashMap<String, Object>>() {
    {
      put ("Prenatal and perinatal history", new LinkedHashMap<String, Object>(){{
        put("0010880", "Increased nuchal translucency");
        put("0010942", "Echogenic intracardiac focus");
        put("0010945", "Fetal phyelectasis");
        put("0010943", "Echogenic fetal bowel");
        //put("_c_thickened_nuchal_fold", "Thickened nuchal fold"); // 0000474, 0000477 ?
        put("0000474", "Thickened nuchal fold");
        put("0010952", "Fetal ventriculomegaly");
        put("0002190", "Choroid plexus cyst");
        //put("_c_short_humerus_femur", "Short humerus and/or femur");
        put("0003014", "Short humerus");
        put("0003097", "Short femur");
        put("0001562", "Oligohydramnios");
        put("0001561", "Polyhydramnios");
        put("0001511", "IUGR");
        put("0001622", "Premature birth");
        put("_other", "");
      }});

      put ("Family history", new LinkedHashMap<String, Object>(){{
        put("_c_miscarriages", "Parents with at least 3 miscarriages");
        put("_c_relatives", "Other relatives with similar clinical history");
        put("_i_relative_details", "(please give details here)");
      }});
      put ("Growth parameters", new LinkedHashMap<String, Object>(){{
        put("Weight for age", new LinkedHashMap<String, Object>(){{
          put("0004325", "<3rd");
          put("0001513", ">97th");
        }});
        put("Weight for age", new LinkedHashMap<String, Object>(){{
          put("0004325", "<3rd");
          put("0001513", ">97th");
        }});
        put("Weight for age", new LinkedHashMap<String, Object>(){{
          put("0004325", "<3rd");
          put("0001513", ">97th");
        }});
        put("0001535", "Failure to thrive");
        put("_other", null);
      }});

      put ("Development", new LinkedHashMap<String, Object>(){{
        put("0007228", "Global development delay");
        put("0010862", "Fine motor delay");
        put("0002194", "Gross motor delay");
        put("0000750", "Impaired language development");
        put("_other", null);
      }});

      put ("Cognitive", new LinkedHashMap<String, Object>(){{
        put("Mental retardation", new LinkedHashMap<String, Object>(){{
          put("0001256", "Mild");
          put("0002342", "Moderate");
          put("0010864", "Severe");
        }});
        put("0001328", "Learning disability");
        put("_other", null);
        put("_i_iqdq", "List IQ/DQ if known");
      }});
      put ("Behavioral", new LinkedHashMap<String, Object>(){{
        put("0007018", "Attention deficit hyperactivity disorder");
        put("0000717", "Autism");
        put("0000729", "Pervasive developmental delay");
        put("0010865", "Oppositional defiant disorder");
        put("0000722", "Obsessive-compulsive disorder");
        put("0002368", "Psychiatric disorders");
        put("_other", null);
      }});
      put ("Neurological", new LinkedHashMap<String, Object>(){{
        put("0001290", "Hypotonia");
        put("0002197", "Seizures");
        put("0001251", "Ataxia");
        put("0001332", "Dystonia");
        put("0002072", "Chorea");
        put("0001257", "Spasticity");
        put("0100021", "Cerebral paralysis");
        put("0010301", "Neural tube defect");
        put("0007319", "Malformation of the CNS");
        put("_other", null);
      }});
      put ("Ear and Eye Defects", new LinkedHashMap<String, Object>(){{
        put("0000618", "Blindness");
        put("0000589", "Coloboma");
        put("0000404", "Deafness");
        put("0008572", "External ear malformation");
        put("_other", null);
      }});
      put ("Craniofacial", new LinkedHashMap<String, Object>(){{
        put("0000204", "Cleft lip");
        put("0000175", "Cleft palate");
        put("0001363", "Craniosynostosis");
        put("0001999", "Facial dysmorphism");
        put("_other", null);
      }});
      put ("Cutaneous", new LinkedHashMap<String, Object>(){{
        put("0000953", "Hyperpigmentation");
        put("0001010", "Hypopigmentation");
        put("_other", null);
      }});
      put ("Cardiac", new LinkedHashMap<String, Object>(){{
        put("0001631", "ASD");
        put("0001629", "VSD");
        put("0001674", "AV canal defect");
        put("0001680", "Coarctation of aorta");
        put("0004383", "Hypoplastic left heart");
        put("0001636", "Tetralogy of fallot");
        put("_other", null);
      }});
      put ("Musculoskeletal", new LinkedHashMap<String, Object>(){{
        put("0002817", "Abnormality of the upper limb");
        put("0002814", "Abnormality of the lower limb");
        put("Syndactyly", new LinkedHashMap<String, Object>(){{
          put("0006101", "Finger");
          put("0001770", "Toe");
        }});
        put("Polydactyly (hands/feet)", new LinkedHashMap<String, Object>(){{
          put("0100258", "Preaxial");
          put("0100259", "Postaxial");
        }});
        put("Oligodactyly", new LinkedHashMap<String, Object>(){{
          put("0001180", "Hands");
          put("0001849", "Feet");
        }});
        put("0002650", "Scoliosis");
        put("0000925", "Vertebral Anomaly");
        put("0001371", "Contractures");
        put("0001762", "Club foot");
        put("_other", null);
      }});
      put ("Gastrointestinal", new LinkedHashMap<String, Object>(){{
        put("0002575", "Tracheoesophageal fistula");
        put("0000776", "Diaphragmatic hernia");
        put("0001543", "Gastroschisis");
        put("0001539", "Omphalocele");
        put("0002021", "Pyloric stenosis");
        put("0002251", "Hirschsprung disease");
        put("_other", null);
      }});
      put ("Genitourinary", new LinkedHashMap<String, Object>(){{
        put("0000792", "Kidney malformation");
        put("0000126", "Hydronephrosis");
        put("0000062", "Ambiguous genitalia");
        put("0000047", "Hypospadias");
        put("0000028", "Cryptorchidism");
        put("_other", null);
      }});
    }
  };

  Object[] sectionNames = sections.keySet().toArray();

  for (int i = 0; i < sectionNames.length; ++i) {%>
    <fieldset>
      <legend class="section"><%= sectionNames[i] %></legend>
      <%= handleSection(sections.get(sectionNames[i])) %>
    </fieldset>
<%}%>
</div>
</fieldset>
<div><input type="submit" value="Submit"/></div>
</form>
<%}%>
</body>
</html>
