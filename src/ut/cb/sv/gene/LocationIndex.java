package ut.cb.sv.gene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ut.cb.sv.gene.RelativeLocationMap.RelativePosition;
import ut.cb.util.maps.SetMap;

public class LocationIndex
{
    Map<String, RelativeLocationMap<GeneLocation>> index = new HashMap<String, RelativeLocationMap<GeneLocation>>();

    protected LocationIndex(GeneFunctionData g)
    {
        // Memorize and sort all location bounds
        SetMap<String, Integer> bounds = new SetMap<String, Integer>();
        for (GeneLocation geneLoc : g.keySet()) {
            bounds.addTo(geneLoc.getChr(), geneLoc.getStart());
            bounds.addTo(geneLoc.getChr(), geneLoc.getEnd());
        }
        for (String chr : bounds.keySet()) {
            int sortedBounds[] = new int[bounds.get(chr).size()], j = 0;
            for (int i : bounds.get(chr)) {
                sortedBounds[j++] = i;
            }
            Arrays.sort(sortedBounds);
            RelativeLocationMap<GeneLocation> map = new RelativeLocationMap<GeneLocation>();
            this.index.put(chr, map);
            // just making sure the numbers appear in the right order...
            for (Integer i : sortedBounds) {
                map.addTo(i, (GeneLocation) null);
            }
        }

        // Add gene locations in the index to their respective start and end positions
        for (GeneLocation geneLoc : g.keySet()) {
            RelativeLocationMap<GeneLocation> chrIndex = this.index.get(geneLoc.getChr());
            chrIndex.addTo(geneLoc.getStart(), RelativePosition.START, geneLoc);
            chrIndex.addTo(geneLoc.getEnd(), RelativePosition.END, geneLoc);
        }

        // Update the index by adding genes to the lists of the positions that fall inside their bounds
        for (String chr : this.index.keySet()) {
            RelativeLocationMap<GeneLocation> chrIndex = this.index.get(chr);
            Set<GeneLocation> current = new HashSet<GeneLocation>();
            for (Integer key : chrIndex.keySet()) {
                chrIndex.remove(null);
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

    public Set<GeneLocation> getOverlappingGenes(String chr, int start, int end)
    {
        Set<GeneLocation> result = new LinkedHashSet<GeneLocation>();
        RelativeLocationMap<GeneLocation> chrIndex = this.index.get(chr);
        if (chrIndex == null) {
            return result;
        }

        ArrayList<Integer> positions = new ArrayList<Integer>();
        positions.addAll(chrIndex.keySet());
        int from = getClosestOverlappingPosition(positions, start, RelativePosition.START);
        int to = getClosestOverlappingPosition(positions, end, RelativePosition.END);
        if (from != to) {
            for (int i = from; i <= to; ++i) {
                result.addAll(chrIndex.getAll(positions.get(i)));
            }
        }
        return result;
    }

    protected int getClosestOverlappingPosition(ArrayList<Integer> positions, int position,
        RelativePosition relPos)
    {
        int start = 0, idx = positions.size() / 2, end = positions.size(), tmp;
        do {
            if (positions.get(idx) == position) {
                start = end = idx;
            } else if (positions.get(idx) > position) {
                tmp = idx;
                idx = (start + idx) / 2;
                end = tmp;
            } else {
                tmp = idx;
                idx = (end + idx) / 2;
                start = tmp;
            }
        } while (end - start > 1);

        if (start == end) {
            return start;
        }
        return relPos == RelativePosition.START ? end : start;
    }
}
