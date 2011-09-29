package ut.cb.sv.gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;
import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.go.GOTree;
import ut.cb.sv.go.GeneOntologyWrapper;
import ut.cb.util.maps.SetMap;

public class GeneFunctionData extends SetMap<GeneLocation, String>
{

    public static final String CHROMOSOME_MARKER = "CHR";

    public static final String STRETCHSTART_MARKER = "START";

    public static final String STRETCHEND_MARKER = "STOP";

    public static final String GO_FUNCTIONS_MARKER = "GO_functions";

    GOTree goTree;

    LocationIndex index;

    public GeneFunctionData()
    {
        this(GeneOntologyWrapper.DEFAULT_MAP_FILE);
    }

    public GeneFunctionData(String mapFile)
    {
        try {
            loadFile(mapFile);
            loadAllGeneFuntions(mapFile);
            buildLocationIndex();
        } catch (IOException ex) {
            System.err.println("Failed to load gene function data.");
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Failed to load gene ontology tree.");
            ex.printStackTrace();

        }
    }

    private void buildLocationIndex()
    {
        this.index = new LocationIndex(this);
    }

    private void loadFile(String mapFile) throws IOException
    {
        String line;
        BufferedReader in = new BufferedReader(new FileReader(mapFile));

        // While there is still something to be read...
        while ((line = in.readLine()) != null) {
            try {
                String[] pieces = line.split(Location.INPUT_SEPARATOR, 3);

                String termID = pieces[0];

                GeneLocation geneLoc = new GeneLocation(pieces[1], pieces[2]);
                if (geneLoc.isValid()) {
                    this.addTo(geneLoc, termID);
                } else {
                    System.err.println("Invalid entry: " + line);
                    System.err.println(geneLoc);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println(ex.getClass().getName() + ": " + ex.getMessage()
                    + ". Not enough pieces of information in " + line);
            }
        }
        in.close();
    }

    private void loadAllGeneFuntions(String mapFile) throws Exception
    {
        this.goTree = GeneOntologyWrapper.makeTree(mapFile);
        for (GeneLocation geneLoc : this.keySet()) {
            Set<String> crtFunctions = new LinkedHashSet<String>();
            crtFunctions.addAll(this.get(geneLoc));
            for (String f : crtFunctions) {
                this.get(geneLoc).addAll(this.goTree.getNodeAndAncestorsIds(f));
            }
        }
    }

    public Set<GeneLocation> getOverlappingGenes(Chromosome chr, int start, int end)
    {
        return this.index.getOverlappingGenes(chr, start, end);
    }

    public Set<String> getGOFunctionsOfOverlappingGenes(Chromosome chr, int start, int end)
    {
        Set<String> result = new HashSet<String>();
        for (GeneLocation geneLoc : this.index.getOverlappingGenes(chr, start, end)) {
            result.addAll(this.get(geneLoc));
        }
        return result;
    }

    public void addGOInfoToDatabase(Database data)
    {
        if (!data.hasFeature(CHROMOSOME_MARKER) ||
            !data.hasFeature(STRETCHSTART_MARKER) ||
            !data.hasFeature(STRETCHEND_MARKER)) {
            System.err
                .println("Cannot load GO funtions into database." +
                " Database entries do not have all the features necessary for defining genomic locations");
            return;
        }
        Feature goFunctionsInfo = new Feature(GO_FUNCTIONS_MARKER);
        data.addFeature(goFunctionsInfo);
        for (DatabaseEntry entry : data) {
            try {
                entry.addFeature(goFunctionsInfo, getGOFunctionsOfOverlappingGenes(
                    Chromosome.getValue((String) entry.get(CHROMOSOME_MARKER)),
                    (Integer) entry.get(STRETCHSTART_MARKER),
                    (Integer) entry.get(STRETCHEND_MARKER)));
            } catch (Exception ex) {
                System.err.println(ex.getClass().getName() + " " + ex.getMessage()
                    + ". Cannot load GO functions for:\n"
                    + entry.toString());
                System.err.println(Chromosome.getValue((String) entry.get(CHROMOSOME_MARKER)));
                System.err.println(entry.get(STRETCHSTART_MARKER));
                System.err.println(entry.get(STRETCHEND_MARKER));
                System.err.println(getOverlappingGenes(
                    Chromosome.getValue((String) entry.get(CHROMOSOME_MARKER)),
                    (Integer) entry.get(STRETCHSTART_MARKER),
                    (Integer) entry.get(STRETCHEND_MARKER)));
                System.exit(0);

            }
        }
    }
}
