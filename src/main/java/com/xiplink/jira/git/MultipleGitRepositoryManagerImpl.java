package com.xiplink.jira.git;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.security.PermissionManager;
import com.opensymphony.module.propertyset.PropertySet;
import com.xiplink.jira.git.revisions.RevisionIndexService;
import com.xiplink.jira.git.revisions.RevisionIndexer;

/**
 * This is a wrapper class for many GitManager. Configured via {@link GPropertiesLoader#PROPERTIES_FILE_NAME}
 * 
 * @author Dylan Etkin
 * @see GitManager
 */
public class MultipleGitRepositoryManagerImpl implements MultipleGitRepositoryManager {
	private static Logger log = Logger.getLogger(MultipleGitRepositoryManagerImpl.class);

	public static final String APP_PROPERTY_PREFIX = "jira.plugins.git";
	public static final String REPO_PROPERTY = "jira.plugins.git.repo";

	public static final String LAST_REPO_ID = "last.repo.id";

	public static final long FIRST_REPO_ID = 1;

	private PropertySet pluginProperties;

	private Map<Long, GitManager> managerMap = new HashMap<Long, GitManager>();
	private RevisionIndexer revisionIndexer;
	private JiraPropertySetFactory jiraPropertySetFactory;

	private long lastRepoId;

	public MultipleGitRepositoryManagerImpl(ApplicationProperties applicationProperties, VersionManager versionManager,
			IssueManager issueManager, PermissionManager permissionManager,
			JiraPropertySetFactory jiraPropertySetFactory)

	{
		this.jiraPropertySetFactory = jiraPropertySetFactory;

		managerMap = loadGitManagers();

		boolean revisionIndexing = false;
		for (GitManager mgr : managerMap.values()) {
			if (mgr.isRevisionIndexing()) {
				revisionIndexing = true;
				break;
			}
		}

		if (!revisionIndexing) // they might have removed the property - let's check there is no service anyway
		{
			try {
				RevisionIndexService.remove();
			} catch (Exception e) {
				throw new InfrastructureException("Failure removing revision indexing service", e);
			}
		} else {
			// create revision indexer once we know we have succeed initializing our repositories
			revisionIndexer = new RevisionIndexer(this, applicationProperties, versionManager, issueManager,
					permissionManager);
		}
	}

	/**
	 * loads a map of long id to GitManager from persistent storage or if that doesn't exist, system properties
	 */
	protected Map<Long, GitManager> loadGitManagers() {

		Map<Long, GitManager> managers = loadManagersFromJiraProperties();

		if (managers.isEmpty()) {
			log.info("Could not find any git repositories configured, trying to load from System Properties.");
			try {
				// Try loading from the properties file.
				managers = loadFromProperties();
			} catch (Throwable t) {
				log.error("Could not load properties from " + GPropertiesLoader.PROPERTIES_FILE_NAME, t);
				throw new InfrastructureException("Could not load properties from "
						+ GPropertiesLoader.PROPERTIES_FILE_NAME, t);
			}
		}

		return managers;
	}

	/**
	 * The git configuration properties are stored in the application properties. It's not the best place to store
	 * collections of information, like multiple repositories, but it will work. Keys for the properties look like:
	 * <p/>
	 * jira.plugins.git.<repoId>;<property name>
	 * <p/>
	 * Using this scheme we can get all the properties and put them into buckets corresponding to the repoId. Then when
	 * we have all the properties we can go about building the GitProperties objects and creating our
	 * GitRepositoryManagers.
	 */
	private Map<Long, GitManager> loadManagersFromJiraProperties() {

		pluginProperties = jiraPropertySetFactory.buildCachingDefaultPropertySet(APP_PROPERTY_PREFIX, true);

		lastRepoId = pluginProperties.getLong(LAST_REPO_ID);

		// create the GitManagers
		Map<Long, GitManager> managers = new LinkedHashMap<Long, GitManager>();
		for (long i = FIRST_REPO_ID; i <= lastRepoId; i++) {
			GitManager mgr = createManagerFromPropertySet(i, jiraPropertySetFactory.buildCachingPropertySet(
					REPO_PROPERTY, new Long(i), true));
			if (mgr != null)
				managers.put(new Long(i), mgr);
		}
		return managers;
	}

	protected GitManager createManagerFromPropertySet(long index, PropertySet properties) {
		try {
			if (properties.getKeys().size() == 0)
				return null;

			return new GitManagerImpl(index, properties);
		} catch (IllegalArgumentException e) {
			log.error("Error creating GitManager " + index
					+ ". Probably was missing a required field (e.g., repository name or root). Skipping it.", e);
			return null;
		}
	}

	/**
	 * loads GitManagers from SystemProperties the legacy way
	 */
	protected Map<Long, GitManager> loadFromProperties() {
		Map<Long, GitManager> managers = new HashMap<Long, GitManager>();

		try {
			List<GitProperties> properties = GPropertiesLoader.getGitProperties();
			for (GitProperties property : properties) {
				try {
					GitManager mgr = createRepository(property);
					managers.put(new Long(mgr.getId()), mgr);
				} catch (Throwable t) {
					log.warn("Problem adding a git manager", t);
				}
			}
		} catch (Exception e) {
			log.warn("Problem adding a git manager", e);
		}

		return managers;
	}

	public GitManager createRepository(GProperties properties) {
		long repoId;
		synchronized (this) {
			repoId = ++lastRepoId;
			pluginProperties.setLong(LAST_REPO_ID, lastRepoId);
		}

		PropertySet set = jiraPropertySetFactory.buildCachingPropertySet(REPO_PROPERTY, new Long(repoId), true);
		GitManager gitManager = new GitManagerImpl(repoId, GProperties.Util.fillPropertySet(properties, set));

		managerMap.put(new Long(gitManager.getId()), gitManager);
		if (isIndexingRevisions()) {
			revisionIndexer.addRepository(gitManager);
		}

		return gitManager;
	}

	public GitManager updateRepository(long repoId, GProperties properties) {
		GitManager gitManager = getRepository(repoId);
		gitManager.update(properties);
		return gitManager;
	}

	public void removeRepository(long repoId) {
		GitManager original = managerMap.get(new Long(repoId));
		if (original == null) {
			return;
		}
		try {
			managerMap.remove(new Long(repoId));

			// would like to just call remove() but this version doesn't appear to have that, remove all of it's
			// properties instead
			Collection<String> keys = new ArrayList<String>(original.getProperties().getKeys());
			for (String string : keys) {
				String key = string;
				original.getProperties().remove(key);
			}

			if (revisionIndexer != null)
				revisionIndexer.removeEntries(original);
		} catch (Exception e) {
			throw new InfrastructureException("Could not remove repository index", e);
		}
	}

	public boolean isIndexingRevisions() {
		return revisionIndexer != null;
	}

	public RevisionIndexer getRevisionIndexer() {
		return revisionIndexer;
	}

	public Collection<GitManager> getRepositoryList() {
		return managerMap.values();
	}

	public GitManager getRepository(long id) {
		return managerMap.get(new Long(id));
	}

	public void start() throws Exception {
		if (isIndexingRevisions()) {
			getRevisionIndexer().start();
		}
	}
}
