/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 1:44:30 PM
 */
package com.xiplink.jira.git.linkrenderer;

import org.spearce.jgit.revwalk.RevCommit;

import com.xiplink.jira.git.FileDiff;



public interface GitLinkRenderer
{
    String getRevisionLink(RevCommit revision);

    String getChangePathLink(RevCommit revision, FileDiff changePath);

    public String getCopySrcLink(RevCommit revision, FileDiff changePath);
}