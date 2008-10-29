/*
 * User: Mike
 * Date: Sep 16, 2004
 * Time: 1:57:17 PM
 */
package com.xiplink.jira.git.issuetabpanels.changes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.spearce.jgit.revwalk.RevCommit;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.action.IssueActionComparator;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import com.opensymphony.user.User;
import com.xiplink.jira.git.MultipleGitRepositoryManager;

public class GitRevisionsTabPanel extends AbstractIssueTabPanel {
	private static Logger log = Logger.getLogger(GitRevisionsTabPanel.class);

	protected final MultipleGitRepositoryManager multipleGitRepositoryManager;
	private PermissionManager permissionManager;

	public GitRevisionsTabPanel(MultipleGitRepositoryManager multipleGitRepositoryManager, PermissionManager permissionManager) {
		this.multipleGitRepositoryManager = multipleGitRepositoryManager;
		this.permissionManager = permissionManager;
	}

	public List<GitRevisionAction> getActions(Issue issue, User remoteUser) {
		try {
			Map<Long, List<RevCommit>> logEntries = multipleGitRepositoryManager.getRevisionIndexer().getLogEntriesByRepository(issue);

			// This is a bit of a hack to get the error message across
			if (logEntries == null) {
				GenericMessageAction action = new GenericMessageAction(getText("no.index.error.message"));
				return EasyList.build(action);
			} else if (logEntries.size() == 0) {
				GenericMessageAction action = new GenericMessageAction(getText("no.log.entries.message"));
				return EasyList.build(action);
			} else {
				List<GitRevisionAction> actions = new ArrayList<GitRevisionAction>(logEntries.size());
				for (Entry<Long, List<RevCommit>> entry : logEntries.entrySet()) {
					long repoId = entry.getKey().longValue();

					for (RevCommit logEntry : entry.getValue()) {
						actions.add(new GitRevisionAction(logEntry, multipleGitRepositoryManager, descriptor, repoId));
					}
				}
				Collections.sort(actions, IssueActionComparator.COMPARATOR);
				return actions;
			}
		}
		catch (Throwable t) {
			log.error("Error retrieving actions for : " + issue.getKey(), t);
		}

		return Collections.emptyList();
	}

    protected String getText(String key) {
        return descriptor.getI18nBean().getText(key);
    }

    public boolean showPanel(Issue issue, User remoteUser) {
		return multipleGitRepositoryManager.isIndexingRevisions() &&
						permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, remoteUser);
	}
}