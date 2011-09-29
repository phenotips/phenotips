package ut.cb.sv.gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import ut.cb.sv.go.GOTree;
import ut.cb.sv.go.GeneOntologyWrapper;
import ut.cb.util.maps.SetMap;

public class GeneFunctionData extends SetMap<GeneLocation, String>
{
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
}
