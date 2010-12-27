/*
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 8:14:04 AM
 */
package com.xiplink.jira.git;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jgit.revwalk.RevCommit;

import com.opensymphony.module.propertyset.PropertySet;
import com.xiplink.jira.git.linkrenderer.GitLinkRenderer;

public interface GitManager {
    Map<String,String> getBranches();
    RevCommit getMergeBase(String baseId, String branchId);
	Collection<RevCommit> getLogEntries(String fromRev, String toRev);
	RevCommit getLogEntry(String revision);
	long getId();
	String getDisplayName();
	String getRoot();
	String getOrigin();
	boolean isActive();
	String getInactiveMessage();
	void activate();
	boolean isRevisionIndexing();
	int getRevisioningCacheSize();
	
    ViewLinkFormat getViewLinkFormat();
	GitLinkRenderer getLinkRenderer();
	void update(GProperties properties);
	PropertySet getProperties();
	void fetch();
	
	FileDiff[] getFileDiffs(String revision);

    String getRefId(String refName) throws IOException;
}