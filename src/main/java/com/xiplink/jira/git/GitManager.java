/*
 * User: Mike
 * Date: Sep 30, 2004
 * Time: 8:14:04 AM
 */
package com.xiplink.jira.git;

import java.util.Collection;

import org.spearce.jgit.revwalk.RevCommit;


import com.opensymphony.module.propertyset.PropertySet;
import com.xiplink.jira.git.linkrenderer.GitLinkRenderer;

public interface GitManager {
	Collection<RevCommit> getLogEntries(String revision);
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

}