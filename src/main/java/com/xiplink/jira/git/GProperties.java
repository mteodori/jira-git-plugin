package com.xiplink.jira.git;

import com.opensymphony.module.propertyset.PropertySet;


public interface GProperties {
	String getRoot();

	String getDisplayName();

	String getUsername();

	String getPassword();

	Boolean getRevisionIndexing();

	Integer getRevisionCacheSize();

	String getPrivateKeyFile();

    String getWebLinkType();

    String getChangesetFormat();

	String getFileAddedFormat();

	String getViewFormat();

	String getFileModifiedFormat();

	String getFileReplacedFormat();

	String getFileDeletedFormat();

	static class Util {
		static PropertySet fillPropertySet(GProperties properties, PropertySet propertySet) {
			propertySet.setString(MultipleGitRepositoryManager.GIT_ROOT_KEY, properties.getRoot());
			propertySet.setString(MultipleGitRepositoryManager.GIT_REPOSITORY_NAME, properties.getDisplayName() != null ? properties.getDisplayName() : properties.getRoot());
			propertySet.setString(MultipleGitRepositoryManager.GIT_USERNAME_KEY, properties.getUsername());
			propertySet.setString(MultipleGitRepositoryManager.GIT_PASSWORD_KEY, GitManagerImpl.encryptPassword(properties.getPassword()));
			propertySet.setBoolean(MultipleGitRepositoryManager.GIT_REVISION_INDEXING_KEY, properties.getRevisionIndexing().booleanValue());
			propertySet.setInt(MultipleGitRepositoryManager.GIT_REVISION_CACHE_SIZE_KEY, properties.getRevisionCacheSize().intValue());
			propertySet.setString(MultipleGitRepositoryManager.GIT_PRIVATE_KEY_FILE, properties.getPrivateKeyFile());
            propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_TYPE, properties.getWebLinkType());
            propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_PATH_KEY, properties.getViewFormat()); /* git-190 */
            propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_CHANGESET, properties.getChangesetFormat());
			propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_ADDED, properties.getFileAddedFormat());
			propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_MODIFIED, properties.getFileModifiedFormat());
			propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_REPLACED, properties.getFileReplacedFormat());
			propertySet.setString(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_DELETED, properties.getFileDeletedFormat());
			return propertySet;
		}
	}
}
