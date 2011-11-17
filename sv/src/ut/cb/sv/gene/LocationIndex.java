package ut.cb.sv.gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ut.cb.sv.gene.RelativeLocationMap.RelativePosition;
import ut.cb.util.maps.SetMap;

public class LocationIndex
{
    Map<Chromosome, RelativeLocationMap<GeneLocation>> index =
        new HashMap<Chromosome, RelativeLocationMap<GeneLocation>>();

    protected LocationIndex(GeneFunctionData g)
    {
        // Memorize and sort all location bounds
        SetMap<Chromosome, Integer> bounds = new SetMap<Chromosome, Integer>();
        for (GeneLocation geneLoc : g.keySet()) {
            if (geneLoc == null || geneLoc.getChr() == null) {
                System.err.println(geneLoc);
                continue;
            }
            bounds.addTo(geneLoc.getChr(), geneLoc.getStart());
            bounds.addTo(geneLoc.getChr(), geneLoc.getEnd());
        }
        for (Chromosome chr : bounds.keySet()) {
            RelativeLocationMap<GeneLocation> map = new RelativeLocationMap<GeneLocation>();
            this.index.put(chr, map);
            for (Integer i : bounds.get(chr)) {
                map.reset(i);
            }
        }

        // Add gene locations in the index to their respective start and end positions
        for (GeneLocation geneLoc : g.keySet()) {
            RelativeLocationMap<GeneLocation> chrIndex = this.index.get(geneLoc.getChr());
            chrIndex.addTo(geneLoc.getStart(), RelativePosition.START, geneLoc);
            chrIndex.addTo(geneLoc.getEnd(), RelativePosition.END, geneLoc);
        }

        // Update the index by adding genes to the lists of the positions that fall inside their bounds
        for (Chromosome chr : this.index.keySet()) {
            RelativeLocationMap<GeneLocation> chrIndex = this.index.get(chr);
            Set<GeneLocation> current = new HashSet<GeneLocation>();
            for (Integer key : chrIndex.keySet()) {
                // chrIndex.remove(null);
                current.removeAll(chrIndex.safeGet(key, RelativePosition.END));
                chrIndex.addTo(key, RelativePosition.IN, current);
                current.addAll(chrIndex.safeGet(key, RelativePosition.START));
            }
        }
    }

    public Set<GeneLocation> getOverlappingGenes(Location l)
    {
        return getOverlappingGenes(l.getChr(), l.getStart(), l.getEnd());
    }

    public Set<GeneLocation> getOverlappingGenes(Chromosome chr, int start, int end)
    {
        Set<GeneLocation> result = new LinkedHashSet<GeneLocation>();
        RelativeLocationMap<GeneLocation> chrIndex = this.index.get(chr);
        if (chrIndex == null) {
            return result;
        }

        ArrayList<Integer> positions = new ArrayList<Integer>();
        positions.addAll(chrIndex.keySet());
        Collections.sort(positions);
        int from = Collections.binarySearch(positions, start);
        int to = Collections.binarySearch(positions, end);

        if (from < 0) {
            // start with the index immediately after the insertion point
            from = -(from + 1);
        }
        if (to < 0) {
            // stop at the index immediately before the insertion point
            to = -(to + 1) - 1;
        }
        for (int i = from; i <= to; ++i) {
            result.addAll(chrIndex.getAll(positions.get(i)));
        }

        if (from > to && from > 0 && from < positions.size()) {
            // both fall between two indexed bounds;
            result.addAll(chrIndex.get(positions.get(from - 1), RelativePosition.IN));
            result.addAll(chrIndex.get(positions.get(from), RelativePosition.IN));
        }
        return result;
    }
}
