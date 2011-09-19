package ut.cb.sv.db.feature;

import ut.cb.sv.db.Database;

/**
 * A {@link CategoricalFeature} designated as the classification label of the entries in a {@link Database}.
 * 
 * @version $Id$
 */
public class LabelFeature extends CategoricalFeature
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
