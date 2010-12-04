/*
 * User: Mike
 * Date: Sep 16, 2004
 * Time: 2:00:52 PM
 */
package com.xiplink.jira.git.issuetabpanels.changes;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.revwalk.RevCommit;


import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.util.JiraKeyUtils;
import com.xiplink.jira.git.FileDiff;
import com.xiplink.jira.git.GitConstants;
import com.xiplink.jira.git.MultipleGitRepositoryManager;
import com.xiplink.jira.git.linkrenderer.GitLinkRenderer;

/**
 * One item in the 'Git Commits' tab.
 */
public class GitRevisionAction extends AbstractIssueAction {

	protected final RevCommit revision;
	protected final long repoId;
	protected final IssueTabPanelModuleDescriptor descriptor;
	protected MultipleGitRepositoryManager multipleGitRepositoryManager;
	protected Date timePerformed;
    protected String branch;

	public GitRevisionAction(RevCommit logEntry, MultipleGitRepositoryManager multipleGitRepositoryManager,
			IssueTabPanelModuleDescriptor descriptor, long repoId, String branch) {
		super(descriptor);
		this.multipleGitRepositoryManager = multipleGitRepositoryManager;
		this.descriptor = descriptor;
		/* git-93 */
		this.revision = logEntry;
		this.timePerformed = new Date(revision.getCommitTime() * 1000L);
		this.repoId = repoId;
        this.branch = branch;
	}

	protected void populateVelocityParams(Map params) {
		params.put("git", this);
	}

	public GitLinkRenderer getLinkRenderer() {
		return multipleGitRepositoryManager.getRepository(repoId).getLinkRenderer();
	}

	public String getRepositoryDisplayName() {
		return multipleGitRepositoryManager.getRepository(repoId).getDisplayName();
	}

	public Date getTimePerformed() {
		return timePerformed;
	}

    public String getTimePerformedFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZZ");
        return sdf.format(timePerformed);
    }

	public long getRepoId() {
		return repoId;
	}

    public String getBranch() {
        return branch;
    }

    public String getUsername() {
		return revision.getAuthorIdent().getName();
	}

	public RevCommit getRevision() {
		return revision;
	}

	public boolean isAdded(FileDiff logEntryPath) {
		return GitConstants.ADDED.equals(logEntryPath.change);
	}

	public boolean isModified(FileDiff logEntryPath) {
		return GitConstants.MODIFICATION.equals(logEntryPath.change);
	}

	public boolean isDeleted(FileDiff logEntryPath) {
		return GitConstants.DELETED.equals(logEntryPath.change);
	}

	public FileDiff[] getChangedPaths() {
		return multipleGitRepositoryManager.getRepository(repoId).getFileDiffs(revision.getId().name());
	}

	/**
	 * Converts all lower case JIRA issue keys to upper case so that they can be
	 * correctly rendered in the Velocity macro, makelinkedhtml.
	 * 
	 * @param logMessageToBeRewritten
	 *            The git log message to be rewritten.
	 * @return The rewritten git log message.
	 * @see <a href="http://jira.atlassian.com/browse/git-93">git-93</a>
	 */
	protected String rewriteLogMessage(final String logMessageToBeRewritten) {
		String logMessage = logMessageToBeRewritten;
		final String logMessageUpperCase = StringUtils.upperCase(logMessage);
		final Set<String> issueKeys = new HashSet<String>(JiraKeyUtils.getIssueKeysFromString(logMessageUpperCase));

		for (String issueKey : issueKeys) {
			int indexOfIssueKey;
			int lastIndexOfIssueKey = 0;

			while (lastIndexOfIssueKey < logMessageUpperCase.length()
					&& -1 != (indexOfIssueKey = logMessageUpperCase.indexOf(issueKey, lastIndexOfIssueKey))) {
				logMessage = logMessage.replaceFirst(logMessage.substring(indexOfIssueKey, indexOfIssueKey
						+ issueKey.length()), issueKey);
				lastIndexOfIssueKey = indexOfIssueKey + issueKey.length();
			}
		}

		return logMessage;
	}
}
