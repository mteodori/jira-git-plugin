package com.xiplink.jira.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.jira.InfrastructureException;

/**
 * exists to load GitManagers the old way so that the
 * MultipleGitRepositoryManagerImpl doesn't get krufted up with a bunch
 * of legacy code
 */
public class GPropertiesLoader {

	private static Logger log = Logger.getLogger(GPropertiesLoader.class);

	public static final String PROPERTIES_FILE_NAME = "git-jira-plugin.properties";

	public static List<GitProperties> getGitProperties() throws InfrastructureException {
		Properties allProps = System.getProperties();

		try {
			allProps.load(ClassLoaderUtils.getResourceAsStream(PROPERTIES_FILE_NAME,
					MultipleGitRepositoryManagerImpl.class));
		} catch (IOException e) {
			throw new InfrastructureException("Problem loading " + PROPERTIES_FILE_NAME + ".", e);
		}

		List<GitProperties> propertyList = new ArrayList<GitProperties>();
		GitProperties defaultProps = getGitProperty(-1, allProps);
		if (defaultProps != null) {
			propertyList.add(defaultProps);
		} else {
			log.error("Could not load properties from " + PROPERTIES_FILE_NAME);
			throw new InfrastructureException("Could not load properties from " + PROPERTIES_FILE_NAME);
		}
		GitProperties prop;
		int i = 1;
		do {
			prop = getGitProperty(i, allProps);
			i++;
			if (prop != null) {
				prop.fillPropertiesFromOther(defaultProps);
				propertyList.add(prop);
			}
		} while (prop != null);

		return propertyList;
	}

	protected static GitProperties getGitProperty(int index, Properties props) {
		String indexStr = "." + Integer.toString(index);
		if (index == -1) {
			indexStr = "";
		}

		if (props.containsKey(MultipleGitRepositoryManager.GIT_ROOT_KEY + indexStr)) {
			final String gitRootStr = props.getProperty(MultipleGitRepositoryManager.GIT_ROOT_KEY + indexStr);
			final String displayName = props.getProperty(MultipleGitRepositoryManager.GIT_REPOSITORY_NAME + indexStr);

//			final String linkPathFormat = props.getProperty(MultipleGitRepositoryManager.git_LINKFORMAT_PATH_KEY
//					+ indexStr);
			final String changesetFormat = props.getProperty(MultipleGitRepositoryManager.GIT_LINKFORMAT_CHANGESET
					+ indexStr);
			final String fileAddedFormat = props.getProperty(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_ADDED
					+ indexStr);
			final String fileModifiedFormat = props
					.getProperty(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_MODIFIED + indexStr);
			final String fileReplacedFormat = props
					.getProperty(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_REPLACED + indexStr);
			final String fileDeletedFormat = props.getProperty(MultipleGitRepositoryManager.GIT_LINKFORMAT_FILE_DELETED
					+ indexStr);

			final String username = props.getProperty(MultipleGitRepositoryManager.GIT_USERNAME_KEY + indexStr);
			final String password = props.getProperty(MultipleGitRepositoryManager.GIT_PASSWORD_KEY + indexStr);
			final String privateKeyFile = props.getProperty(MultipleGitRepositoryManager.GIT_PRIVATE_KEY_FILE
					+ indexStr);
			Boolean revisionIndexing = null;
			if (props.containsKey(MultipleGitRepositoryManager.GIT_REVISION_INDEXING_KEY + indexStr)) {
				revisionIndexing = Boolean.valueOf("true".equalsIgnoreCase(props
						.getProperty(MultipleGitRepositoryManager.GIT_REVISION_INDEXING_KEY + indexStr)));
			}
			Integer revisionCacheSize = null;
			if (props.containsKey(MultipleGitRepositoryManager.GIT_REVISION_CACHE_SIZE_KEY + indexStr)) {
				revisionCacheSize = new Integer(props
						.getProperty(MultipleGitRepositoryManager.GIT_REVISION_CACHE_SIZE_KEY + indexStr));
			}

			return new GitProperties().setRoot(gitRootStr).setDisplayName(displayName).setChangeSetFormat(
					changesetFormat).setFileAddedFormat(fileAddedFormat).setFileModifiedFormat(fileModifiedFormat)
					.setFileReplacedFormat(fileReplacedFormat).setFileDeletedFormat(fileDeletedFormat).setUsername(
							username).setPassword(password).setPrivateKeyFile(privateKeyFile).setRevisionIndexing(
							revisionIndexing).setRevisioningCacheSize(revisionCacheSize);

		} else {
			log.info("No " + MultipleGitRepositoryManager.GIT_ROOT_KEY + indexStr + " specified in "
					+ PROPERTIES_FILE_NAME);
			return null;
		}
	}
}
