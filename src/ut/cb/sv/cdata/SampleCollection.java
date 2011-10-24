package ut.cb.sv.cdata;

import java.util.Set;
import java.util.TreeMap;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;
import ut.cb.sv.gene.GeneFunctionData;

public class SampleCollection extends TreeMap<String, Sample>
{
    public static final String SAMPLE_ID_FEATURE_NAME = "SAMPLE_ID";

    public static final String GENDER_FEATURE_NAME = "GENDER";

    public static final String PHENOTYPE_FEATURE_NAME = "PHENOTYPE";

    public static final String CHROMOSOME_FEATURE_NAME = "CHR";

    public static final String LOCATION_START_FEATURE_NAME = "START";

    public static final String LOCATION_END_FEATURE_NAME = "STOP";

    public static final String VARIANT_TYPE_FEATURE_NAME = "TYPE";

    public SampleCollection(Database data)
    {
        GeneFunctionData gd = new GeneFunctionData();
        for (DatabaseEntry entry : data) {
            String id = (String) entry.get(SAMPLE_ID_FEATURE_NAME);
            Gender gender = Gender.getValue((String) entry.get(GENDER_FEATURE_NAME));
            if (id == null || gender == null) {
                continue;
            }
            Sample sample = this.get(id);
            if (sample == null) {
                sample = new Sample(id, gender);
                this.put(id, sample);
            }
            if (entry.get(PHENOTYPE_FEATURE_NAME) != null) {
                sample.addPhenotype((Set<String>) entry.get(PHENOTYPE_FEATURE_NAME));
            }
            Variant variant = new Variant((String) entry.get(CHROMOSOME_FEATURE_NAME),
                (Integer) entry.get(LOCATION_START_FEATURE_NAME), (Integer) entry
                .get(LOCATION_END_FEATURE_NAME), (String) entry.get(VARIANT_TYPE_FEATURE_NAME));
            variant.addGoFunctions(gd.getGOFunctionsOfOverlappingGenes(variant));
            sample.addVariant(variant);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        for (String id : this.keySet()) {
            str.append(this.get(id)).append("\n");
        }
        return str.toString();
    }
}
