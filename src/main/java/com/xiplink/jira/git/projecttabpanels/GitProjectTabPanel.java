package com.xiplink.jira.git.projecttabpanels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.spearce.jgit.revwalk.RevCommit;

import webwork.action.ActionContext;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanel;
import com.atlassian.jira.plugin.projectpanel.ProjectTabPanelModuleDescriptor;
import com.atlassian.jira.plugin.projectpanel.impl.GenericProjectTabPanel;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.action.browser.Browser;
import com.opensymphony.user.User;
import com.xiplink.jira.git.MultipleGitRepositoryManager;
import com.xiplink.jira.git.revisions.RevisionIndexer;

/**
 * This class provides a tab panel for the JIRA project view.
 *
 * @author Rolf Staflin
 * @version $Id$
 */
public class GitProjectTabPanel extends GenericProjectTabPanel implements ProjectTabPanel {

	private static Logger log = Logger.getLogger(GitProjectTabPanel.class);

	private ProjectTabPanelModuleDescriptor descriptor;
	private MultipleGitRepositoryManager multipleGitRepositoryManager;
	private VersionManager versionManager;
	private PermissionManager permissionManager;

	/**
	 * Constants for the wildcard "version number".
	 */
	public static final int ALL_VERSIONS = -1;

	/**
	 * The number of commits to show in the tab.
	 */
	public static final int NUMBER_OF_REVISIONS = 20;

	/**
	 * Set this to <code>true</code> if you want to include
	 * archived versions in the "Select version" drop-down list.
	 */
	public static final boolean INCLUDE_ARCHIVED_VERSIONS = false;

	/**
	 * Constructor. Picocontainer is used to automatically supply all needed parameters.
	 *
	 * @param multipleGitRepositoryManager
	 *                          The manager that keeps track of all the git repositories
	 * @param versionManager		This manager is used to look up all the versions of the current project
	 * @param permissionManager This manager is used to check that the user has permission to view git data.
	 */
	public GitProjectTabPanel(MultipleGitRepositoryManager multipleGitRepositoryManager,
																	 VersionManager versionManager, PermissionManager permissionManager) {
		this.multipleGitRepositoryManager = multipleGitRepositoryManager;
		this.versionManager = versionManager;
		this.permissionManager = permissionManager;
	}

	/**
	 * Initializes the descriptor field. Must be called before <code>getHtml()</code>.
	 * The descriptor is the JIRA class that holds a reference to a ProjectTabPanel object
	 * and that merges a context with a Velocity template to create HTML.
	 *
	 * @param descriptor The descriptor that is in charge of this tab panel object.
	 */
	public void init(ProjectTabPanelModuleDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	/**
	 * This method uses the descriptor to create the actual HTML displayed in the tab.
	 * First, the selected version number, if any, is fetched from the browser. Then,
	 * the RevisionIndexer is used to retrieve the log messages for the latest commits
	 * to that version (or to the project as a whole if no particular version or version
	 * set is selected). Finally, the ProjectTabPanelModuleDescriptor is used to render
	 * the HTML for the tab contents.
	 *
	 * @param browser Holds context data
	 * @return HTML code ready for inclusion in the project tab.
	 */
	public String getHtml(Browser browser) {
		if (log.isDebugEnabled()) {
			log.debug(">getHtml(" + browser + ")");
		}

		final Map<String, Object> startingParams = EasyMap.build("action", browser);

		Project project = null;
		String key = null;
		User user = browser.getRemoteUser();
		try {
			project = browser.getProjectObject();
			startingParams.put("project", project);
			key = project.getKey();
			startingParams.put("projectKey", key);
		} catch (Exception e) {
			log.error("!getHtml() Couldn't retrieve current project!", e);
		}

		// Get selected versionNumber, if any
		Long versionNumber = getVersionRequestParameter();
		Version version = null;
		if (versionNumber != null && project != null) {
			startingParams.put("versionNumber", new Integer(versionNumber.intValue()));
//			if (versionNumber.longValue() > 0) { This check is redundant
				version = versionManager.getVersion(versionNumber);
				if (version != null) {
					startingParams.put("selectedVersion", version);
				}
//			}
		}

		// Get the list of recently updated issues and add it to the velocity context
		startingParams.put("commits", getRecentCommits(key, version, user));

		// Get all versions. Used for the "Select versionNumber" drop-down list
		Collection releasedVersions = versionManager.getVersionsReleased(project.getId(), INCLUDE_ARCHIVED_VERSIONS);
		startingParams.put("releasedVersions", releasedVersions);
		Collection unreleasedVersions = versionManager.getVersionsUnreleased(project.getId(), INCLUDE_ARCHIVED_VERSIONS);
		startingParams.put("unreleasedVersions", unreleasedVersions);
		startingParams.put("versionManager", versionManager);

		// Merge with velocity template and return HTML.
		return descriptor.getHtml("view", startingParams);
	}

	/**
	 * Looks up the latest commits for the curently selected project in each of the repositories.
	 *
	 * @param key		 The JIRA project key of the currently selected project.
	 * @param version The JIRA project version to get commits for. If <code>null</code> is passed in,
	 *                the latest commits for the project as a whole are returned instead.
	 * @param user		The remote user -- we need to check that the user has "View Version Control" permission for an issue
	 *                before we show a commit for it.
	 * @return A List of {@link GitProjectRevisionAction} objects, each of which holds a valid {@link gitLogEntry}.
	 *         The number of commits returned is decided by the constant <code>NUMBER_OF_REVISIONS</code>.
	 */
	private Collection<GitProjectRevisionAction> getRecentCommits(String key, Version version, User user) {
		if (log.isDebugEnabled()) {
			log.debug(">getRecentCommits(" + key + ", " + version + ")");
		}
		List<GitProjectRevisionAction> actions = new ArrayList<GitProjectRevisionAction>();

		try {
			Map<Long, Collection<RevCommit>> logEntries;
			RevisionIndexer indexer = multipleGitRepositoryManager.getRevisionIndexer();
			if (version == null) {
				logEntries = indexer.getLogEntriesByProject(key, user, NUMBER_OF_REVISIONS);
			} else {
				logEntries = indexer.getLogEntriesByVersion(version, user, NUMBER_OF_REVISIONS);
			}

			if (logEntries != null && logEntries.size() > 0) {
				for (Entry<Long, Collection<RevCommit>> entry : logEntries.entrySet()) {
					long repoId = entry.getKey().longValue();
					for (RevCommit logEntry : entry.getValue()) {
						actions.add(new GitProjectRevisionAction(logEntry, multipleGitRepositoryManager, descriptor, repoId));
					}
				}
			}
		}
		catch (Throwable t) {
			log.error("Error retrieving actions for project", t);
		}
		return actions;
	}

	/**
	 * Extracts the <code>selectedVersion</code> parameter from the HTTP request.
	 * The versions are selected by a drop-down list on the git commit tab.
	 *
	 * @return A Long containing the parameter value, or <code>null</code> if
	 *         the parameter was not set or an error occurred while parsing the parameter.
	 */
	private Long getVersionRequestParameter() {
		Long versionNumber = null;

		HttpServletRequest request = ActionContext.getRequest();

		if (request != null) {
			String selectedVersion = request.getParameter("selectedVersion");
			if (StringUtils.isNotBlank(selectedVersion)) {
				try {
					versionNumber = new Long(selectedVersion);
				} catch (NumberFormatException e) {
					log.error("Unknown version string: " + selectedVersion, e);
				}
			}
		}
		return versionNumber;
	}

	/**
	 * Determines if this tab should be shown or not. The ProjectActionSupport is used to
	 * get the remote user. The permission manager is then consulted as to whether the user has
	 * the <code>VIEW_VERSION_CONTROL</code> rights or not in the supplied project.
	 * <p/>
	 * If we are not indexing new revisions, the tab is never shown.
	 *
	 * @param projectActionSupport Used to get the current user
	 * @param project							This current project
	 * @return true if the tab should be shown, false if not
	 */
	public boolean showPanel(ProjectActionSupport projectActionSupport, Project project) {
		User user = projectActionSupport.getRemoteUser();
		return multipleGitRepositoryManager.isIndexingRevisions() &&
						permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, project, user);
	}

	/**
	 * @param versionManager the versionManager to set
	 */
	public void setVersionManager(VersionManager versionManager) {
		this.versionManager = versionManager;
	}

	/**
	 * @return the versionManager
	 */
	public VersionManager getVersionManager() {
		return versionManager;
	}
}
