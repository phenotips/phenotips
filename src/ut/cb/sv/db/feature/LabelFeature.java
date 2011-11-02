package ut.cb.sv.db.feature;

import ut.cb.sv.db.Database;

/**
 * A {@link CategoryFeature} designated as the classification label of the entries in a {@link Database}.
 * 
 * @version $Id$
 */
public class LabelFeature extends CategoryFeature
{

    /** {@inheritDoc} */
    public LabelFeature(String name)
    {
        super(name);
    }

    /** {@link LabelFeature} instances are label features :) */
    @Override
    public boolean isLabelFeature()
    {
        return true;
    }
}
