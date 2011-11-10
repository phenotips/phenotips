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
<%= displayContentTitle("Clinical data form demo") %>

<form id="clinical-info-form" action="clinical-info-validation.jsp" method="post">
<fieldset class="twothird-width clear patient-info chapter">
  <legend class="chapter-title">Patient Information</legend>

  <fieldset>
  <label class="section clear" for="last_name">Patient name:</label>
  <div class="half-width last_name">
    <label for="last_name" class="hint">Last name</label>
    <input type="text" name="last_name" id="last_name"/>
  </div>
  <div class="half-width first_name">
    <label for="first_name" class="hint">First name</label>
    <input type="text" name="first_name" id="first_name"/>
  </div>
  </fieldset>
  <fieldset>
  <div class="half-width date_of_birth">
    <label class="section" for="date_of_birth">Date of birth <span class="hint">(mm/dd/yyyy)</span>:</label>
  </div>
  <div class="half-width date_of_birth">
    <input type="text" name="date_of_birth" id="date_of_birth"/>
  </div>
  </fieldset>
  <fieldset>
  <div class="half-width gender">
    <label class="section">Gender: </label>
  </div>
  <div class="half-width gender">
    <label for="gender_male"><input type="radio" name="gender" id="gender_male" value="male"/>Male</label> 
    <label for="gender_female"><input type="radio" name="gender" id="gender_female" value="female"/>Female</label>
  </div>
  </fieldset>
  <fieldset>
  <div class="half-width health_card">
    <label class="section" for="health_card">Provincial health card #:</label>
  </div>
  <div class="half-width health_card">
    <input type="text" name="health_card" id="health_card"/>
  </div>
  </fieldset>
  <fieldset class="family_study">
  <div class="half-width relative">
    <label class="section">Family study:</label>
    <label for="relative_mother"><input type="radio" name="relative" id="relative_mother" value="mother"/>Mother</label>
    <label for="relative_father"><input type="radio" name="relative" id="relative_father" value="father"/>Father</label>
    <label for="relative_sibling"><input type="radio" name="relative" id="relative_sibling" value="sibling"/>Sibling</label>
    <label class="section" for="relative_of">of</label>
  </div>
  <div class="half-width relative_of">
    <label for="relative_of" class="hint">Patient (MRN)</label>
    <input type="text" name="relative_of" id="relative_of"/>
  </div>
  </fieldset>
</fieldset>
  
<%!
String OTHER_FIELD_MARKER = "_other";
String RESTRICITON_FIELD_MARKER = "_category";
String DETAILS_MESSAGE = " <div class='hint'>(Please list specific abnormalities in the \"Other\" box)</div>";

public boolean isHPId(String id)
{
  return id.matches("[0-9]{7}");
}
public boolean isHPOther(String id)
{
  return id.equals("OTHER_FIELD_MARKER");
}
public boolean isNonHPCheckBox(String id)
{
  return !isHPOther(id) && id.startsWith("_c_");
}
public boolean isNonHPInput(String id)
{
  return !isHPOther(id) && id.startsWith("_i_");
}
public boolean isFreeText(String id)
{
  return !isHPOther(id) && id.startsWith("_t_");
}
public boolean isSubsection(String id)
{
  return !isHPId(id) && !id.startsWith("_");
}

public String handleSection(String dbFieldName, LinkedHashMap<String, Object> sectionData)
{
  
  String result = "";
  boolean hasOtherInput = false;
  String restriction;
  if (sectionData.containsKey(OTHER_FIELD_MARKER)) {
    hasOtherInput = true;
    sectionData.remove(OTHER_FIELD_MARKER);
  }
  restriction = (String)sectionData.remove(RESTRICITON_FIELD_MARKER);
  Object[] fieldIds = sectionData.keySet().toArray();
  for (int i = 0; i < fieldIds.length; ++i) {
    result += handleField(dbFieldName, (String)fieldIds[i], sectionData);
  }
  if (hasOtherInput) {
    result  = "<div class='phenotypes-main half-width'>" + result + "</div>";
    result += "<div class='phenotypes-other half-width'>" + generateInput(dbFieldName, OTHER_FIELD_MARKER, true);
    if (restriction != null) {
      result += "<input type='hidden' name='" + RESTRICITON_FIELD_MARKER + "' value='" + restriction + "' />";
    }
    result += "</div>";
    result += "<div class='clear'></div>";
  } else {
    result  = "<div class='phenotypes-main'>" + result + "</div>";
  }
  return result;
}
public String handleField(String dbFieldName, String id, LinkedHashMap<String, Object> data)
{
  Object label = (data.get(id) == null ? id : data.get(id));
  if (isHPId(id)) {
    return generateCheckBox(dbFieldName, "HP:" + id, (String)label);
  } else if (isHPOther(id)) {
    return generateInput(dbFieldName, (String)label, true);
  } else if (isNonHPCheckBox(id)) {
    return generateCheckBox(dbFieldName, id.substring(3), (String)label);
  } else if (isNonHPInput(id)) {
    return generateInput(id.substring(3), (String)label, false);
  } else if (isSubsection(id)) {
    return "<label class='section'>" + id + "</label><div class='subsection'>" + handleSection(dbFieldName, (LinkedHashMap<String, Object>)data.get(id)) + "</div>";
  } else if (isFreeText(id)) {
    return generateFreeText(id.substring(3), (String)label);
  }
  return "";
}

public String generateCheckBox(String name, String value, String label)
{
  String id = name + (value.startsWith("_") ? "" : "_") + value;
  String message = "";
  if (label.endsWith(DETAILS_MESSAGE)) {
    message = DETAILS_MESSAGE;
    label = label.substring(0, label.indexOf(DETAILS_MESSAGE));
  }
  return "<label for='" + id + "'><input type='checkbox' name='" + name + "' id='" + id + "' value='" + value + "'/>" + label + "</label><br/>" + message;
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
  result = "<label for='" + id + "'" + (suggested ? " class='label-other label-other-" + name + "'" : "") + ">" + displayedLabel + "</label>";
  if (suggested) {
    result +="<p class='hint'>(enter a free text and choose among suggested ontology terms)</p>";
  }
  result +="<input type='text' name='" + name + "'" +(suggested ? "' class='suggested multi suggest-hpo generateCheckboxes'" : "") + " value='' size='16' id='" + id + "'/>";
  return result;
}

public String generateFreeText(String name, String label)
{
  String result = "";
  String id = name + "_" + Math.random();
  result = "<label for='" + id + "'" + ">" + label + "</label>";
  result +="<textarea name='" + name + "' rows='8' cols='40' id='" + id + "'></textarea>";
  return result;
}
%>

<%
  LinkedHashMap<String, LinkedHashMap<String, Object>> sections = new LinkedHashMap<String, LinkedHashMap<String, Object>>() {
    {
       put ("Behavior, Cognition and Development", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0001263,HP:0000708,HP:0001263,HP:0001286");
        put("0007228", "Global development delay");
        put("0010862", "Fine motor delay");
        put("0002194", "Gross motor delay");
        put("0000750", "Language delay");
        put("0001328", "Learning disability");
        put("Mental retardation", new LinkedHashMap<String, Object>(){{
          put("0001256", "Mild");
          put("0002342", "Moderate");
          put("0010864", "Severe");
        }});
        put("0007018", "Attention deficit hyperactivity disorder");
        put("0000717", "Autism");
        put("0000729", "Pervasive developmental delay");
        put("0002368", "Psychiatric disorders" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Neurological", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000707");
        put("0001290", "Hypotonia");
        put("0002197", "Seizures");
        put("0001251", "Ataxia");
        put("0001332", "Dystonia");
        put("0002072", "Chorea");
        put("0001257", "Spasticity");
        put("0100021", "Cerebral paralysis");
        put("0010301", "Neural tube defect");
        put("0007319", "Malformation of the CNS " + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Growth parameters", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0001507");
        put("Weight for age", new LinkedHashMap<String, Object>(){{
          put("0004325", "<3rd");
          put("0001513", ">97th");
        }});
        put("Stature for age", new LinkedHashMap<String, Object>(){{
          put("0004322", "<3rd");
          put("0000098", ">97th");
        }});
        put("Head circumference for age", new LinkedHashMap<String, Object>(){{
          put("0000252", "<3rd");
          put("0000256", ">97th");
        }});
        put("0001535", "Hemihypertrophy");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Cardiac", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0001627");
        put("0001631", "ASD");
        put("0001629", "VSD");
        put("0001674", "AV canal defect");
        put("0001680", "Coarctation of aorta");
        put("0001636", "Tetralogy of fallot");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Craniofacial", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000152");
        put("0001363", "Craniosynostosis");
        put("0000204", "Cleft lip");
        put("0000175", "Cleft palate");
	put("0000308", "Microretrognathia");
	put("0000278", "Retrognathia");
        put("0001999", "Facial dysmorphism" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Eye Defects", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000478");
        put("0000618", "Blindness");
        put("0000589", "Coloboma");
        put("0000286", "Epicanthus");
        put("0000492", "Eyelid abnormality" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Ear Defects", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000598");
        put("0000404", "Deafness");
        put("0004467", "Peauricular pit");
        put("0000384", "Preauricular skin tag");
        put("0000356", "Outer ear abnormality" + DETAILS_MESSAGE);
        put("0000359", "Inner ear abnormality" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Cutaneous", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000951");
        put("0000953", "Hyperpigmentation");
        put("0001010", "Hypopigmentation");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Respiratory", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0002086,HP:0000765");
        put("0000776", "Diaphragmatic hernia");
        put("0002088", "Lung abnormality" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Musculoskeletal", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000924,HP:0003549");
        put("0002817", "Abnormality of the upper limb");
        put("0002814", "Abnormality of the lower limb");
        put("Camptodactyly", new LinkedHashMap<String, Object>(){{
          put("0100490", "Finger");
          put("0001836", "Toe");
        }});
        put("Syndactyly", new LinkedHashMap<String, Object>(){{
          put("0006101", "Finger");
          put("0001770", "Toe");
        }});
        put("Polydactyly", new LinkedHashMap<String, Object>(){{
          put("0001161", "Finger");
          put("0001829", "Toe");
        }});
        put("Oligodactyly", new LinkedHashMap<String, Object>(){{
          put("0001180", "Hands");
          put("0001849", "Feet");
        }});
        put("0002650", "Scoliosis");
        put("0000925", "Vertebral Anomaly");
        put("0001371", "Contractures");
        put("0001762", "Club foot");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Gastrointestinal", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0001438");
	put("0002032", "Esophageal atresia");
        put("0002575", "Tracheoesophageal fistula");
        //put("0000776", "Diaphragmatic hernia");
        put("0001543", "Gastroschisis");
        put("0001539", "Omphalocele");
        put("0002021", "Pyloric stenosis");
        //put("0002251", "Hirschsprung disease");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Genitourinary", new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000119");
        put("0000792", "Kidney malformation" + DETAILS_MESSAGE);
        put("0000126", "Hydronephrosis");
        put("0000062", "Ambiguous genitalia");
        put("0000047", "Hypospadias");
        put("0000028", "Cryptorchidism");
        put(OTHER_FIELD_MARKER, null);
      }});
      put ("Other", new LinkedHashMap<String, Object>(){{
      //  put(OTHER_FIELD_MARKER, null);
        put("_t_comments", "Additional comments");
      }});
    }
  };
  %>
  <fieldset class="clinical-info chapter">
  <legend class="chapter-title">Phenotypic description (Clinical symptoms)</legend>

  <div id="quick-search-box">
  <fieldset class="group-other quick-search-box emphasized-box">
    <h2 class="section">Quick phenotype search</h2>
    <label for="quick-phenotype-search">Enter a free text and choose among suggested ontology terms</label>
    <input type='text' name='phenotype' class='suggested multi suggest-hpo quickSearch' value='' size='16' id='quick-phenotype-search'/>
  </fieldset>
  </div>

  <div class="twothird-width">
  <%

  Object[] sectionNames = sections.keySet().toArray();

  for (int i = 0; i < sectionNames.length; ++i) {%>
    <fieldset class="phenotype-group">
      <legend class="section"><%= sectionNames[i] %></legend>
      <%= handleSection("phenotype", sections.get(sectionNames[i])) %>
    </fieldset>
<%}%>
  </div>
  </fieldset>
  
  <fieldset class="clinical-info chapter">
  <legend class="chapter-title">Prenatal and Perinatal History</legend>
  <div class="twothird-width">
    <fieldset class="phenotype-group">

<%LinkedHashMap<String, Object> prenatal_history = new LinkedHashMap<String, Object>(){{
        put(RESTRICITON_FIELD_MARKER, "HP:0000118"); //0001197
        put("0001562", "Oligohydramnios");
        put("0001622", "Premature birth");
        put("0001561", "Polyhydramnios");
        put("0001511", "IUGR");
	put("_c_struct_abn", "Fetal structural abnormality");
	put("_c_markers", "Fetal soft markers in obstetric ultrasound" + DETAILS_MESSAGE);
        put(OTHER_FIELD_MARKER, "");
      }};
%><%= handleSection("prenatal_phenotype", prenatal_history) %>
  
    </fieldset>
  </div>
  </fieldset>
  
  <fieldset class="clinical-info chapter">
  <legend class="chapter-title">Family History</legend>
  <div class="twothird-width">
    <fieldset class="phenotype-group">
  
<%LinkedHashMap<String, Object> family_history = new LinkedHashMap<String, Object>(){{
        put("_c_miscarriages", "Parents with at least 3 miscarriages");
        put("_c_cosanguinity", "Cosanguinity");
        //put("_c_relatives", "Other relatives with similar clinical history");
        //put("_i_relative_details", "(please give details here)");
	put("_t_relative_conditions", "List health conditions found in family (describe the relationship with proband):");
      }};
%><%= handleSection("family_history", family_history) %>

    </fieldset>
  </div>
  </fieldset>
  
<div><input type="submit" value="Submit"/></div>
</form>
</body>
</html>
