/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.xwiki.locks;

import org.xwiki.users.User;

import java.util.Date;
import java.util.Set;

/**
 * @version $Id$
 * @since 1.3M1
 */
public class DocumentLock
{
    /**
     * user.
     */
    protected User user;

    /**
     * date.
     */
    protected Date date;

    /**
     * reason.
     */
    protected String reason;

    /**
     * actions.
     */
    protected Set<String> actions;

    /**
     * canOverride.
     */
    protected boolean canOverride;

    /**
     * Constructor for DocumentLock class object.
     *
     * @param user user
     * @param date date
     * @param reason reason
     * @param actions actions
     * @param canOverride canOverride
     */
    public DocumentLock(User user, Date date, String reason, Set<String> actions, boolean canOverride)
    {
        this.setUser(user);
        this.setDate(date);
        this.setActions(actions);
        this.setCanOverride(canOverride);
        this.setReason(reason);
    }

    /**
     * Get user.
     *
     * @return user
     */
    public User getLockingUser()
    {
        return this.user;
    };

    /**
     * Get date.
     *
     * @return date
     */
    public Date getLockDate()
    {
        return this.date;
    };

    /**
     * Get reason.
     *
     * @return reason
     */
    public String getReason()
    {
        return this.reason;
    };

    /**
     * Get actions.
     *
     * @return actions
     */
    public Set<String> getLockedActions()
    {
        return this.actions;
    };

    /**
     * Get canOverride.
     *
     * @return canOverride
     */
    public boolean canOverride()
    {
        return this.canOverride;
    }

    /**
     * Get user.
     *
     * @return user
     */
    public User getUser()
    {
        return user;
    }

    /**
     * Set date.
     *
     * @param user user
     */
    public void setUser(User user)
    {
        this.user = user;
    }

    /**
     * Get date.
     *
     * @return date
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Set date.
     *
     * @param date date
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * Set actions.
     *
     * @param actions actions
     */
    public void setActions(Set<String> actions)
    {
        this.actions = actions;
    }

    /**
     * Set CanOverride.
     *
     * @param canOverride canOverride
     */
    public void setCanOverride(boolean canOverride)
    {
        this.canOverride = canOverride;
    }

    /**
     * Set reason.
     *
     * @param reason reason
     */
    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
