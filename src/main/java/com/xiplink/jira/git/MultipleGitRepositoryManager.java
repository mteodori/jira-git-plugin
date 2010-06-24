package com.xiplink.jira.git;

import java.util.Collection;

import com.atlassian.jira.extension.Startable;
import com.xiplink.jira.git.revisions.RevisionIndexer;

/**
 * Main component of the Git plugin.
 */
public interface MultipleGitRepositoryManager extends Startable {
	String GIT_ROOT_KEY = "git.root";
	String GIT_ORIGIN_KEY = "git.origin";
	String GIT_REPOSITORY_NAME = "git.display.name";
	String GIT_REVISION_INDEXING_KEY = "revision.indexing";
	String GIT_REVISION_CACHE_SIZE_KEY = "revision.cache.size";
	String GIT_BRANCH_INDEXED_REVISION = "branch.";

	String GIT_LINKFORMAT_TYPE = "linkformat.type";
	String GIT_LINKFORMAT_CHANGESET = "linkformat.changeset";
	String GIT_LINKFORMAT_FILE_ADDED = "linkformat.file.added";
	String GIT_LINKFORMAT_FILE_MODIFIED = "linkformat.file.modified";
	String GIT_LINKFORMAT_FILE_REPLACED = "linkformat.file.replaced";
	String GIT_LINKFORMAT_FILE_DELETED = "linkformat.file.deleted";

	String GIT_LINKFORMAT_PATH_KEY = "linkformat.copyfrom";

	String GIT_LOG_MESSAGE_CACHE_SIZE_KEY = "logmessage.cache.size";

	boolean isIndexingRevisions();

	RevisionIndexer getRevisionIndexer();

	/**
	 * Returns a Collection of GitManager instances, one for each
	 * repository.
	 * 
	 * @return the managers.
	 */
	Collection<GitManager> getRepositoryList();

	GitManager getRepository(long repoId);

	GitManager createRepository(GProperties props);

	GitManager updateRepository(long repoId, GProperties props);

	void removeRepository(long repoId);

    void clearLastIndexedRevisions(long repoId);
}
