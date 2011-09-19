package ut.cb.sv.db.load.filter;

import java.util.regex.Pattern;


public abstract class AbstractEntryFilter implements EntryFilter
{
    protected String featureIdentifier;

    protected Pattern valueRegexPattern;

    public AbstractEntryFilter(String featureIdentifier, String regex)
    {
        this.featureIdentifier = featureIdentifier;
        this.valueRegexPattern = Pattern.compile(regex);
    }

    public abstract boolean accepts(String featureIdentifier, String value);
}
