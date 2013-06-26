/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 1:44:30 PM
 */
package com.xiplink.jira.git.linkrenderer;

import org.eclipse.jgit.revwalk.RevCommit;

import com.xiplink.jira.git.FileDiff;

public interface GitLinkRenderer
{
    // Method names end in "Html" to disable HTML escaping
    // https://developer.atlassian.com/display/JIRADEV/Velocity+Templates

    String getRevisionLinkHtml(RevCommit revision);

    String getChangePathLinkHtml(RevCommit revision, FileDiff changePath);

    public String getCopySrcLinkHtml(RevCommit revision, FileDiff changePath);
}