/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 1:47:18 PM
 */
package com.xiplink.jira.git.linkrenderer;

import org.eclipse.jgit.revwalk.RevCommit;

import com.xiplink.jira.git.FileDiff;



/**
 * Used when the user does not specify any web links for Perforce - just return String values, no links.
 */
public class NullLinkRenderer implements GitLinkRenderer
{
    public String getRevisionLink(RevCommit revision)
    {
        return revision.getId().toString();
    }

    public String getChangePathLink(RevCommit revision, FileDiff logEntryPath)
    {
        return logEntryPath.getPath();
    }

    public String getCopySrcLink(RevCommit revision, FileDiff logEntryPath)
    {
        return logEntryPath//.getCopyPath() 
        + " #" + logEntryPath;//.getCopyRevision();
    }
}