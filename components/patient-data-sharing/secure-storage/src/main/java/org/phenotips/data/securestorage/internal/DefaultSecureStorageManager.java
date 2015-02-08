/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.securestorage.internal;

import org.phenotips.data.securestorage.LocalLoginToken;
import org.phenotips.data.securestorage.PatientPushedToInfo;
import org.phenotips.data.securestorage.PatientSourceServerInfo;
import org.phenotips.data.securestorage.RemoteLoginData;
import org.phenotips.data.securestorage.SecureStorageManager;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Default implementation using Hibernate
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component
@Singleton
public class DefaultSecureStorageManager implements SecureStorageManager
{
    /** Handles persistence. */
    @Inject
    private HibernateSessionFactory sessionFactory;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public void removeRemoteLoginData(String localUserName, String serverName)
    {
        RemoteLoginData existing = getRemoteLoginData(localUserName, serverName);
        if (existing != null) {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            try {
                t.begin();
                this.logger.info("Removing stored token for [{}@{}]", localUserName, serverName);
                session.delete(existing);
                t.commit();
            } catch (HibernateException ex) {
                this.logger.error("Error removing stored token for [{}@{}]: [{}]", localUserName, serverName, ex);
                if (t!=null) {
                    t.rollback();
                }
            } finally {
                session.close();
            }
        }
    }

    @Override
    public void storeRemoteLoginData(String localUserName, String serverName,
        String remoteUserName, String remoteLoginToken)
    {
        RemoteLoginData existing = getRemoteLoginData(localUserName, serverName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        try {
            t.begin();
            if (existing != null)
            {
                // this.logger.debug("DEBUG: Updating token");
                existing.setRemoteUserName(remoteUserName);
                existing.setLoginToken(remoteLoginToken);
                session.update(existing);
            }
            else
            {
                // this.logger.debug("DEBUG: Saving new token");
                session.save(new RemoteLoginData(localUserName, serverName, remoteUserName, remoteLoginToken));
            }
            t.commit();
        } catch (HibernateException ex) {
            this.logger.error("Error storing remote login for [{}@{}]: [{}]", localUserName, serverName, ex);
            if (t!=null) {
                t.rollback();
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void storeLocalLoginToken(String userName, String sourceServerName, String loginToken)
    {
        LocalLoginToken existing = getLocalLoginToken(userName, sourceServerName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        try {
            t.begin();
            if (existing != null)
            {
                this.logger.info("Updating token for [{}@{}]", userName, sourceServerName);
                existing.setLoginToken(loginToken);
                session.update(existing);
            }
            else
            {
                this.logger.info("Saving new token for [{}@{}]", userName, sourceServerName);
                session.save(new LocalLoginToken(userName, sourceServerName, loginToken));
            }
            t.commit();
        } catch (HibernateException ex) {
            this.logger.error("Error storing local login token for [{}@{}]: [{}]", userName, sourceServerName, ex);
            if (t!=null) {
                t.rollback();
            }
        } finally {
            session.close();
        }
    }

    @Override
    public RemoteLoginData getRemoteLoginData(String localUserName, String serverName)
    {
        if (localUserName == null || serverName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        try {
            RemoteLoginData data = (RemoteLoginData) session.createCriteria(RemoteLoginData.class)
                .add(Restrictions.eq("localUserName", localUserName))
                .add(Restrictions.eq("serverName", serverName))
                .uniqueResult();

            if (data == null) {
                this.logger.info("Remote login token not found or more than one found for [{}@{}]", localUserName, serverName);
                return null;
            }

            this.logger.debug("Token found for [{}@{}]", localUserName, serverName);
            return data;
        } catch (HibernateException ex) {
            this.logger.error("Error getting remote login token for [{}@{}]: [{}]", localUserName, serverName, ex);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public LocalLoginToken getLocalLoginToken(String userName, String sourceServerName)
    {
        if (userName == null || sourceServerName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        try {
            LocalLoginToken data = (LocalLoginToken) session.createCriteria(LocalLoginToken.class)
                .add(Restrictions.eq("localUserName", userName))
                .add(Restrictions.eq("sourceServerName", sourceServerName))
                .uniqueResult();

            if (data == null) {
                this.logger.info("Local token not found or more than one found for [{}@{}]", userName, sourceServerName);
                return null;
            }

            //this.logger.debug("Local token found for [{}@{}]", userName, sourceServerName);
            return data;
        } catch (HibernateException ex) {
            this.logger.error("Error getting local login token for [{}@{}]: [{}]", userName, sourceServerName, ex);
        } finally {
            session.close();
        }
        return null;
    }

    @Override
    public void removeAllLocalTokens(String sourceServerName)
    {
        throw new RuntimeException();
    }

    @Override
    public void storePatientSourceServerInfo(String patientGUID, String sourceServerName)
    {
        PatientSourceServerInfo existing = getPatientSourceServerInfo(patientGUID);

        if (existing != null)
        {
            if (!existing.getSourceServerName().equals(sourceServerName)) {
                this.logger.warn("Multiple servers pushing the same patient: "
                    + "remote server is already defined as {} and is different from {}",
                    existing.getSourceServerName(), sourceServerName);
            }
        }
        else
        {
            Session session = this.sessionFactory.getSessionFactory().openSession();
            Transaction t = session.beginTransaction();
            try {
                t.begin();
                this.logger.info("Saving remote source server for [{}] = [{}]", patientGUID, sourceServerName);
                session.save(new PatientSourceServerInfo(patientGUID, sourceServerName));
                t.commit();
            } catch (HibernateException ex) {
                this.logger.error("Error saving remote soource server for [{}]: [{}]", patientGUID, ex);
            } finally {
                session.close();
            }
        }
    }

    @Override
    public PatientSourceServerInfo getPatientSourceServerInfo(String patientGUID)
    {
        Session session = this.sessionFactory.getSessionFactory().openSession();
        PatientSourceServerInfo data = (PatientSourceServerInfo) session.createCriteria(PatientSourceServerInfo.class)
            .add(Restrictions.eq("patientGUID", patientGUID))
            .uniqueResult();

        if (data == null) {
            this.logger.debug("No remote source server defined for [{}]", patientGUID);
            return null;
        }

        this.logger.debug("Remote source server found for [{}]: [{}]", patientGUID, data.getSourceServerName());
        return data;
    }

    @Override
    public void storePatientPushInfo(String localPatientID, String remoteServerName,
        String remotePatientGUID, String remotePatientID, String remotePatientURL)
    {
        if (localPatientID == null || remoteServerName == null) {
            return;
        }

        PatientPushedToInfo existing = getPatientPushInfo(localPatientID, remoteServerName);

        Session session = this.sessionFactory.getSessionFactory().openSession();
        Transaction t = session.beginTransaction();
        t.begin();

        if (existing != null)
        {
            this.logger.debug("Updating patient push info for [{}]: [{}@{}] -> [{}@{}]", localPatientID,
                existing.getRemotePatientID(), existing.getRemoteServerName(), remotePatientID, remoteServerName);
            existing.setLastPushTimeToNow();
            existing.setRemotePatientID(remotePatientID);
            existing.setRemotePatientGUID(remotePatientGUID);
            existing.setRemotePatientURL(remotePatientURL);
            session.update(existing);
        }
        else
        {
            this.logger.debug("Saving new patient push info [{}]: [{}@{}]", localPatientID,
                remotePatientID, remoteServerName);
            session.save(new PatientPushedToInfo(localPatientID, remoteServerName,
                remotePatientGUID, remotePatientID, remotePatientURL));
        }
        t.commit();
    }

    @Override
    public PatientPushedToInfo getPatientPushInfo(String localPatientID, String remoteServerName)
    {
        if (localPatientID == null || remoteServerName == null) {
            return null;
        }

        Session session = this.sessionFactory.getSessionFactory().openSession();
        PatientPushedToInfo data = (PatientPushedToInfo) session.createCriteria(PatientPushedToInfo.class)
            .add(Restrictions.eq("localPatientID", localPatientID))
            .add(Restrictions.eq("remoteServerName", remoteServerName))
            .uniqueResult();

        if (data == null) {
            this.logger.debug("Never pushed [{}] to [{}]", localPatientID, remoteServerName);
            return null;
        }

        this.logger.debug("[{}] was previously pushed to [{}@{}]", localPatientID, data.getRemotePatientID(),
            remoteServerName);
        return data;
    }
}
