package ut.cb.sv.db.load.filter;



public class AcceptEntryFilter extends AbstractEntryFilter
{
    public AcceptEntryFilter(String featureIdentifier, String regex)
    {
        super(featureIdentifier, regex);
    }

    @Override
    public boolean accepts(String featureIdentifier, String value)
    {
        return !this.featureIdentifier.equals(featureIdentifier) || this.valueRegexPattern.matcher(value).matches();
    }

}
