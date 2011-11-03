package ut.cb.sv.db.load.filter;

import java.lang.reflect.InvocationTargetException;

public class EntryFilterFactory
{

    protected final static String ENTRY_IGNORE_FILTER_MARKER = "Ignore:";

    protected final static String ENTRY_ACCEPT_FILTER_MARKER = "Accept:";

    protected static final String ID_VALUE_SEPARATOR = "=";

    protected static final String DEFAULT_FILTER_CLASS_PREFIX = EntryFilter.class.getPackage().getName();

    protected static final String DEFAULT_FILTER_CLASS_SUFFIX = EntryFilter.class.getSimpleName();

    public boolean isEntryFilterLine(String line)
    {
        return line.startsWith(ENTRY_ACCEPT_FILTER_MARKER) || line.startsWith(ENTRY_IGNORE_FILTER_MARKER);
    }

    public EntryFilter createFilter(String filterLine)
    {
        String prefix;
        if (filterLine.startsWith(ENTRY_IGNORE_FILTER_MARKER)) {
            prefix = ENTRY_IGNORE_FILTER_MARKER;
        } else if (filterLine.startsWith(ENTRY_ACCEPT_FILTER_MARKER)) {
            prefix = ENTRY_ACCEPT_FILTER_MARKER;
        } else {
            return null;
        }
        return createFilter(prefix.substring(0, prefix.length() - 1), filterLine
            .substring(prefix.length() + 1));
    }

    public EntryFilter createFilter(String type, String filterLine)
    {
        String[] parts = filterLine.split(ID_VALUE_SEPARATOR);
        if (parts.length != 2) {
            return null;
        }
        try {
            return (EntryFilter) (Class.forName(DEFAULT_FILTER_CLASS_PREFIX + '.' + type + DEFAULT_FILTER_CLASS_SUFFIX)
                .getConstructor(String.class, String.class).newInstance(parts[0].trim(), parts[1].trim()));
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return null;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
