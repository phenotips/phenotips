package ut.cb.sv.go;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

public class GOTree extends Tree
{

    /**
     * Map from GO ID to locations
     */
    HashMap<String, HashSet<String>> idToLocs;

    /**
     * The file containing genome locations.
     */
    public String fileGenLocation;

    /**
     * Constructor
     * 
     * @param fileGenLocation the location of the file mapping GO IDs to genome locations.
     */
    public GOTree(String fileGenLocation) throws Exception
    {

        super();
        this.fakeRoot.setDescription("...");
        this.fileGenLocation = fileGenLocation;
        // Map GO ID to locations on genome.
        this.idToLocs = super.idToLocs;
        mapIDtoLocations();

    }

    /**
     * Map GO ID to Uniprot ID.
     * 
     * @param filename the name of the file containing the information.
     * @return a map of GO ID to Uniprot ID.
     */
    private void mapIDtoLocations() throws Exception
    {

        String line;

        // Get file.
        // File is assumed to have on each line, "uniprotID\tX" where X may be
        // a GO ID.
        File file = new File(this.fileGenLocation);

        FileReader fileReader = new FileReader(file);
        BufferedReader buffer = new BufferedReader(fileReader);

        // While there is still something to be read...
        while ((line = buffer.readLine()) != null) {

            String[] split = line.split("\t");

            // Get the key
            String key = split[0];
            // key = key.replace(':', '_');

            // Get the value
            HashSet<String> value = this.idToLocs.get(key);

            // If the key has not been put yet, create a map.
            if (value == null) {

                value = new HashSet<String>();
                this.idToLocs.put(key, value);
            }

            String location = split[1] + "\t" + split[2] + "\t" +
                split[3] + "\t" + split[4];
            value.add(location);
        }

        fileReader.close();
        buffer.close();
    }
}
