package ut.cb.sv.db.load.filter;



public class IgnoreEntryFilter extends AbstractEntryFilter
{
    public IgnoreEntryFilter(String featureIdentifier, String regex)
    {
        super(featureIdentifier, regex);
    }

    @Override
    public boolean accepts(String xpath, String value)
    {
        return !this.featureIdentifier.equals(xpath) || !this.valueRegexPattern.matcher(value).matches();
    }
}
