package ut.cb.sv.db.load.filter;

public interface EntryFilter
{
    boolean accepts(String entryID, String entryValue);
}
