/*
 * User: Mike
 * Date: Oct 1, 2004
 * Time: 5:06:44 PM
 */
package com.xiplink.jira.git.revisions;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.service.AbstractService;
import com.atlassian.jira.service.ServiceManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;

public class RevisionIndexService extends AbstractService
{
    public static final String REVISION_INDEX_SERVICE_NAME = "Git Revision Indexing Service";
    public static final long REVISION_INDEX_SERVICE_DELAY = 60 * 60 * 1000L;

    private static ServiceManager serviceManager;

    public void run()
    {
        try
        {
            MultipleGitRepositoryManager multipleGitRepositoryManager = getMultipleGitRepositoryManager();

            if (multipleGitRepositoryManager.getRevisionIndexer() != null)
            {
                multipleGitRepositoryManager.getRevisionIndexer().updateIndex();
            }
            else
            {
                log.warn("Tried to index changes but GitManager has no revision indexer?");
            }
        }
        catch (Throwable t)
        {
            log.error("Error indexing changes: " + t, t);
        }
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException
    {
        return getObjectConfiguration("gitREVISIONSERVICE", "services/plugins/git/revisionindexservice.xml", null);
    }

    public static void install() throws Exception
    {
        if (getServiceManager().getServiceWithName(REVISION_INDEX_SERVICE_NAME) == null)
        {
            getServiceManager().addService(REVISION_INDEX_SERVICE_NAME, RevisionIndexService.class.getName(), REVISION_INDEX_SERVICE_DELAY);
        }
    }

    public static void remove() throws Exception
    {
        if (getServiceManager().getServiceWithName(REVISION_INDEX_SERVICE_NAME) != null)
        {
            getServiceManager().removeServiceByName(REVISION_INDEX_SERVICE_NAME);
        }
    }

    public static void setServiceManager(ServiceManager serviceManager) {
        RevisionIndexService.serviceManager = serviceManager;
    }

    private static ServiceManager getServiceManager()
    {
        if (null == serviceManager)
            serviceManager = (ServiceManager) ComponentManager.getInstance().getContainer().getComponentInstance(ServiceManager.class);

        return serviceManager;
    }

    private MultipleGitRepositoryManager getMultipleGitRepositoryManager()
    {
        return (MultipleGitRepositoryManager) ComponentManager.getInstance().getContainer().getComponentInstance(MultipleGitRepositoryManager.class);
    }

    public boolean isUnique()
    {
        return true;
    }

    public boolean isInternal()
    {
        return true;
    }

    public String getDescription()
    {
        return "This service indexes Git revisions.";
    }
}