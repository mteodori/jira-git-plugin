/*
 * User: Mike
 * Date: Oct 1, 2004
 * Time: 4:58:40 PM
 */
package com.xiplink.jira.git.revisions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.revwalk.RevCommit;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.JiraKeyUtils;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import com.xiplink.jira.git.GPropertiesLoader;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;

import java.util.Collections;

public class RevisionIndexer {
	private static Logger log = Logger.getLogger(RevisionIndexer.class);
	private static final String NOT_INDEXED = "origin";

	private static final String REVISIONS_INDEX_DIRECTORY = "plugins" + System.getProperty("file.separator")
			+ "jira-git-revisions";

	// These are names of the fields in the Lucene documents that contain revision info.
	public static final String FIELD_REVISIONNUMBER = "revision";
	// public static final Term START_REVISION = new Term(FIELD_REVISIONNUMBER, "");
	public static final String FIELD_MESSAGE = "message";
	public static final String FIELD_AUTHOR = "author";
	public static final String FIELD_COMMITTER = "committer";
	public static final String FIELD_DATE = "date";
	public static final String FIELD_ISSUEKEY = "key";
	public static final String FIELD_PROJECTKEY = "project";
	public static final String FIELD_REPOSITORY = "repository";

	public static final StandardAnalyzer ANALYZER = new StandardAnalyzer();

	public static final int MAX_REVISIONS = 100;

	private final MultipleGitRepositoryManager multipleGitRepositoryManager;
	private final ApplicationProperties applicationProperties;
	private final VersionManager versionManager;
	private final IssueManager issueManager;
	private final PermissionManager permissionManager;
	private Hashtable<Long, String> latestIndexedRevisionTbl; // TODO String or ObjectId?
	private LuceneIndexAccessor indexAccessor;

	public RevisionIndexer(MultipleGitRepositoryManager multipleGitRepositoryManager,
			ApplicationProperties applicationProperties, VersionManager versionManager, IssueManager issueManager,
			PermissionManager permissionManager) {
		this(multipleGitRepositoryManager, applicationProperties, versionManager, issueManager, permissionManager,
				new DefaultLuceneIndexAccessor());
	}

	RevisionIndexer(MultipleGitRepositoryManager multipleGitRepositoryManager,
			ApplicationProperties applicationProperties, VersionManager versionManager, IssueManager issueManager,
			PermissionManager permissionManager, LuceneIndexAccessor accessor) {
		this.multipleGitRepositoryManager = multipleGitRepositoryManager;
		this.applicationProperties = applicationProperties;
		this.versionManager = versionManager;
		this.issueManager = issueManager;
		this.permissionManager = permissionManager;
		this.indexAccessor = accessor;
		initializeLatestIndexedRevisionCache();
	}

	public void start() {
		try {
			createIndexIfNeeded();
			RevisionIndexService.install(); // ensure the changes index service
			// is installed
		} catch (Throwable t) {
			log.error("Could not load properties from " + GPropertiesLoader.PROPERTIES_FILE_NAME, t);
			throw new InfrastructureException("Could not load properties from "
					+ GPropertiesLoader.PROPERTIES_FILE_NAME, t);
		}

	}

	/**
	 * This method will scan for the index directory, if it can resolve the path and the directory does not exist then
	 * it will create the index.
	 * 
	 * @return true if the index exists after this method ran, false if the index could not be created.
	 */
	private boolean createIndexIfNeeded() {
		if (log.isDebugEnabled()) {
			log.debug("RevisionIndexer.createIndexIfNeeded()");
		}

		boolean indexExists = indexDirectoryExists();
		if (getIndexPath() != null && !indexExists) {
			try {
				indexAccessor.getIndexWriter(getIndexPath(), true, ANALYZER).close();
				initializeLatestIndexedRevisionCache();
				return true;
			} catch (Exception e) {
				log.error("Could not create the index directory for the Git plugin.", e);
				return false;
			}
		} else {
			return indexExists;
		}
	}

	private void initializeLatestIndexedRevisionCache() {
		Collection<GitManager> repositories = multipleGitRepositoryManager.getRepositoryList();
		Iterator<GitManager> repoIter = repositories.iterator();
		latestIndexedRevisionTbl = new Hashtable<Long, String>();
		while (repoIter.hasNext()) {
			GitManager currentRepo = repoIter.next();
			initializeLatestIndexedRevisionCache(currentRepo);
		}
		if (log.isDebugEnabled()) {
			log.debug("Repository list size = " + repositories.size());
		}
	}

	private void initializeLatestIndexedRevisionCache(GitManager gitManager) {
		if (log.isDebugEnabled())
			log.debug("Repo \"" + gitManager.getId() + "\" is not indexed: adding NOT_INDEXED marker");
		latestIndexedRevisionTbl.put(new Long(gitManager.getId()), NOT_INDEXED);
	}

	private boolean indexDirectoryExists() {
		try {
			// check if the directory exists
			File file = new File(getIndexPath());

			return file.exists();
		} catch (Exception e) {
			return false;
		}
	}

	public String getIndexPath() {
		String indexPath = null;
		String rootIndexPath = applicationProperties.getString(APKeys.JIRA_PATH_INDEX);
		if (rootIndexPath != null) {
			indexPath = rootIndexPath + System.getProperty("file.separator") + REVISIONS_INDEX_DIRECTORY;
		} else {
			log
					.warn("At the moment the root index path of jira is not set, so we can not form an index path for the git plugin.");
		}

		return indexPath;
	}

	/**
	 * This method updates the index, creating it if it does not already exist. TODO: this monster really needs to be
	 * broken down - weed out the loop control
	 * 
	 * @throws IndexException
	 *             if there is some problem in the indexing subsystem meaning indexes cannot be updated.
	 */
	public void updateIndex() throws IndexException, IOException {
		if (createIndexIfNeeded()) {
			Collection<GitManager> repositories = multipleGitRepositoryManager.getRepositoryList();
			Iterator<GitManager> repoIter = repositories.iterator();

			// temp log comment
			if (log.isDebugEnabled()) {
				log.debug("repos size = " + repositories.size());
			}

			while (repoIter.hasNext()) {
				GitManager gitManager = repoIter.next();
				try {
					// if the repository isn't active, try activating it. if it still not accessible, skip it
					if (!gitManager.isActive()) {
						gitManager.activate();

						if (!gitManager.isActive()) {
							continue;
						}
					}

					
					
					long repoId = gitManager.getId();
					
					
					
					String latestIndexedRevision = "";

					if (getLatestIndexedRevision(repoId) != null) {
						latestIndexedRevision = getLatestIndexedRevision(repoId);
					} else {
						// no latestIndexedRevision, no need to update? This probably means that the repository have
						// been removed from the file system
						log.warn("Did not update index because null value in hash table for " + repoId);
						continue;
					}

					if (log.isDebugEnabled()) {
						log.debug("Updating revision index for repository=" + repoId);
					}

					if (latestIndexedRevision == null || "".equals(latestIndexedRevision)) {
						latestIndexedRevision = updateLastRevisionIndexed(repoId);
					}

					if (log.isDebugEnabled()) {
						log
								.debug("Latest indexed revision for repository=" + repoId + " is : "
										+ latestIndexedRevision);
					}
				
					gitManager.fetch();
					
					final Collection<RevCommit> logEntries = gitManager.getLogEntries(latestIndexedRevision);

					IndexWriter writer = indexAccessor.getIndexWriter(getIndexPath(), false, ANALYZER);

					try {

						final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());

						try {
							for (RevCommit logEntry : logEntries) {
								if (TextUtils.stringSet(logEntry.getFullMessage()) && isKeyInString(logEntry)) {
									if (!hasDocument(repoId, logEntry.getId(), reader)) {
										Document doc = getDocument(repoId, logEntry);
										if (log.isDebugEnabled()) {
											log.debug("Indexing repository=" + repoId + ", revision: "
													+ logEntry.getId());
										}
										writer.addDocument(doc);
										// TODO
										// if (logEntry.getRevision() > latestIndexedRevision) {
										latestIndexedRevision = logEntry.getId().name();
										// // update the in-memory cache git-71
										log.warn("update latest as " + latestIndexedRevision + " at "
												+ new Date(logEntry.getCommitTime() * 1000L));
										latestIndexedRevisionTbl.put(new Long(repoId), latestIndexedRevision);
										// }
									}
								}
							}
						} finally {
							reader.close();
						}
					} finally {
						writer.close();
					}
				} catch (IOException e) {
					log.warn("Unable to index repository '" + gitManager.getDisplayName() + "'", e);
				} catch (RuntimeException e) {
					log.warn("Unable to index repository '" + gitManager.getDisplayName() + "'", e);
				}
			} // while
		}
	}

	protected boolean isKeyInString(RevCommit logEntry) {
		final String logMessageUpperCase = StringUtils.upperCase(logEntry.getFullMessage());
		return JiraKeyUtils.isKeyInString(logMessageUpperCase);
	}

	protected String getLatestIndexedRevision(long repoId) {
		return latestIndexedRevisionTbl.get(new Long(repoId));
	}

	/**
	 * Work out whether a given change, for the specified repository, is already in the index or not.
	 */
	private boolean hasDocument(long repoId, ObjectId revisionNumber, IndexReader reader) throws IOException {
		IndexSearcher searcher = new IndexSearcher(reader);
		try {
			TermQuery repoQuery = new TermQuery(new Term(FIELD_REPOSITORY, Long.toString(repoId)));
			TermQuery revQuery = new TermQuery(new Term(FIELD_REVISIONNUMBER, revisionNumber.name()));
			BooleanQuery repoAndRevQuery = new BooleanQuery();

			repoAndRevQuery.add(repoQuery, BooleanClause.Occur.MUST);
			repoAndRevQuery.add(revQuery, BooleanClause.Occur.MUST);

			Hits hits = searcher.search(repoAndRevQuery);

			if (hits.length() == 1) {
				return true;
			} else if (hits.length() == 0) {
				return false;
			} else {
				log.error("Found MORE than one document for revision: " + revisionNumber + ", repository=" + repoId);
				return true;
			}
		} finally {
			searcher.close();
		}
	}

	private String updateLastRevisionIndexed(long repoId) throws IndexException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Updating last revision indexed.");
		}

		// find all log entries that have already been indexed for the specified repository
		// (i.e. all logs that have been associated with issues in JIRA)
		String latestIndexedRevision = latestIndexedRevisionTbl.get(new Long(repoId));
		// TODO need a means of getting this
		return latestIndexedRevision;
	}

	// private String updateLastRevisionIndexed(long repoId) throws
	// IndexException, IOException {
	// if (log.isDebugEnabled()) {
	// log.debug("Updating last revision indexed.");
	// }
	//
	// // find all log entries that have already been indexed for the specified
	// // repository
	// // (i.e. all logs that have been associated with issues in JIRA)
	// String latestIndexedRevision = latestIndexedRevisionTbl.get(new
	// Long(repoId));
	//
	// String indexPath = getIndexPath();
	// final IndexReader reader;
	// try {
	// reader = IndexReader.open(indexPath);
	// } catch (IOException e) {
	// log.error("Problem with path " + indexPath + ": " + e.getMessage(), e);
	// throw new IndexException("Problem with path " + indexPath + ": " +
	// e.getMessage(), e);
	// }
	// IndexSearcher searcher = new IndexSearcher(reader);
	//
	// try {
	// Hits hits = searcher.search(new TermQuery(new Term(FIELD_REPOSITORY,
	// Long.toString(repoId))));
	//
	// for (int i = 0; i < hits.length(); i++) {
	// Document doc = hits.doc(i);
	// final String revision = doc.get(FIELD_REVISIONNUMBER);
	// // if (revision > latestIndexedRevision) {
	// // TODO this is garbage - any revision? need better selection
	// // (date?)
	// latestIndexedRevision = revision;
	// // }
	//
	// }
	// log.debug("latestIndRev for " + repoId + " = " + latestIndexedRevision);
	// latestIndexedRevisionTbl.put(new Long(repoId), latestIndexedRevision);
	// } finally {
	// reader.close();
	// }
	//
	// return latestIndexedRevision;
	// }

	/**
	 * Creates a new Lucene document for the supplied log entry. This method is used when indexing revisions, not during
	 * retrieval.
	 * 
	 * @param repoId
	 *            ID of the repository that contains the revision
	 * @param logEntry
	 *            The Git log entry that is about to be indexed
	 * @return A Lucene document object that is ready to be added to an index
	 */
	protected Document getDocument(long repoId, RevCommit logEntry) {
		Document doc = new Document();

		// revision information
		doc.add(new Field(FIELD_MESSAGE, logEntry.getFullMessage(), Field.Store.YES, Field.Index.UN_TOKENIZED));

		if (logEntry.getAuthorIdent() != null) {
			doc.add(new Field(FIELD_AUTHOR, logEntry.getAuthorIdent().getName(), Field.Store.YES,
					Field.Index.UN_TOKENIZED));
		}
		if (logEntry.getCommitterIdent() != null) {
			doc.add(new Field(FIELD_COMMITTER, logEntry.getCommitterIdent().getName(), Field.Store.YES,
					Field.Index.UN_TOKENIZED));
		}

		doc.add(new Field(FIELD_REPOSITORY, Long.toString(repoId), Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field(FIELD_REVISIONNUMBER, logEntry.getId().name(), Field.Store.YES, Field.Index.UN_TOKENIZED));

		if (logEntry.getCommitTime() > 0) {
			doc.add(new Field(FIELD_DATE,
					DateTools.timeToString(logEntry.getCommitTime(), DateTools.Resolution.SECOND), Field.Store.YES,
					Field.Index.UN_TOKENIZED));
		}

		// relevant issue keys
		List<String> keys = getIssueKeysFromString(logEntry);

		// Relevant project keys. Used to avoid adding duplicate projects.
		Map<String, String> projects = new HashMap<String, String>();

		for (String issueKey : keys) {
			doc.add(new Field(FIELD_ISSUEKEY, issueKey, Field.Store.YES, Field.Index.UN_TOKENIZED));
			String projectKey = getProjectKeyFromIssueKey(issueKey);
			if (!projects.containsKey(projectKey)) {
				projects.put(projectKey, projectKey);
				doc.add(new Field(FIELD_PROJECTKEY, projectKey, Field.Store.YES, Field.Index.UN_TOKENIZED));
			}
		}

		return doc;
	}

	protected String getProjectKeyFromIssueKey(String issueKey) {
		final String issueKeyUpperCase = StringUtils.upperCase(issueKey);
		return JiraKeyUtils.getFastProjectKeyFromIssueKey(issueKeyUpperCase);
	}

	protected List<String> getIssueKeysFromString(RevCommit logEntry) {
		final String logMessageUpperCase = StringUtils.upperCase(logEntry.getFullMessage());
		return JiraKeyUtils.getIssueKeysFromString(logMessageUpperCase);
	}

	/**
	 * This method will return the log entries collected from Git categorized by the repository it came from. NOTE: a
	 * null map will be returned if the indexes for this plugin have not yet been initialized.
	 * 
	 * @param issue
	 *            the issue to get entries for.
	 * @return A map with key of repository id (long) and entry of a collections of Logs. Null if the repository has not
	 *         yet been initialized.
	 */
	public Map<Long, List<RevCommit>> getLogEntriesByRepository(Issue issue) throws IndexException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Retrieving revisions for : " + issue.getKey());
		}

		if (!indexDirectoryExists()) {
			log.warn("The indexes for the Git plugin have not yet been created.");
			return null;
		} else {
			String key = issue.getKey();

			final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
			IndexSearcher searcher = new IndexSearcher(reader);

			try {
				Hits hits = searcher.search(new TermQuery(new Term(FIELD_ISSUEKEY, key)));
				Map<Long, List<RevCommit>> logEntries = new HashMap<Long, List<RevCommit>>(hits.length());

				for (int i = 0; i < hits.length(); i++) {
					Document doc = hits.doc(i);
					long repositoryId = Long.parseLong(doc.get(FIELD_REPOSITORY));
					// repositoryId is UUID + location
					GitManager manager = multipleGitRepositoryManager.getRepository(repositoryId);
					String revision = doc.get(FIELD_REVISIONNUMBER);
					RevCommit logEntry = manager.getLogEntry(revision);
					if (logEntry == null) {
						log.error("Could not find log message for revision: " + doc.get(FIELD_REVISIONNUMBER));
					} else {
						// Look for list of map entries for repository
						List<RevCommit> entries = logEntries.get(new Long(repositoryId));
						if (entries == null) {
							entries = new ArrayList<RevCommit>();
							logEntries.put(new Long(repositoryId), entries);
						}
						entries.add(logEntry);
					}
				}

				for (List<RevCommit> entries : logEntries.values()) {
					// sort by commit time - can we sort topologically? should
					// we?
					Collections.sort(entries, new Comparator<RevCommit>() {
						public int compare(RevCommit o, RevCommit o1) {
							long r = o.getCommitTime();
							long r1 = o1.getCommitTime();
							if (r == r1) {
								return 0;
							} else if (r > r1) {
								return -1;
							} else {
								return 1;
							}
						}
					});
				}

				return logEntries;
			} finally {
				searcher.close();
				reader.close();
			}
		}
	}

	/**
	 * This method will return the log entries collected from Git categorized by the repository it came from. NOTE: a
	 * null map will be returned if the indexes for this plugin have not yet been initialized.
	 * 
	 * @param projectKey
	 *            The project key, used in all issue keys.
	 * @param user
	 *            The remote user -- we need to check that the user has "View Version Control" permission for an issue
	 *            before we show a commit for it.
	 * @param numberOfEntries
	 *            How many entries to fetch.
	 * @return A map with key of repository id (not display name) and entry of a collections of Logs. Null if the
	 *         repository has not yet been initialized.
	 * @throws com.atlassian.jira.issue.index.IndexException
	 *             if the Lucene index reader cannot be retrieved
	 * @throws java.io.IOException
	 *             if reading the Lucene index fails
	 */
	public Map<Long, Collection<RevCommit>> getLogEntriesByProject(String projectKey, User user, int numberOfEntries)
			throws IndexException, IOException {
		if (projectKey == null || numberOfEntries < 0) {
			throw new IllegalArgumentException("getLogEntriesByProject(" + projectKey + ", " + numberOfEntries + ")");
		}
		if (log.isDebugEnabled()) {
			log.debug("getLogEntriesByProject(" + projectKey + ", " + numberOfEntries + ")");
		}

		if (!indexDirectoryExists()) {
			log.warn("getLogEntriesByProject() The indexes for the Git plugin have not yet been created.");
			return null;
		} else {

			// Set up and perform a search for all documents having the supplied projectKey,sorted in descending date
			// order
			Sort sort = new Sort(FIELD_DATE, true);
			Term term = new Term(FIELD_PROJECTKEY, projectKey);
			TermQuery query = new TermQuery(term);

			Map<Long, Collection<RevCommit>> logEntries;
			final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
			IndexSearcher searcher = new IndexSearcher(reader);
			try {
				Hits hits = searcher.search(query, sort);

				if (hits == null) {
					log.info("getLogEntriesByProject() No matches -- returning null.");
					return null;
				}
				// Build the result map
				logEntries = new HashMap<Long, Collection<RevCommit>>(hits.length());
				int commitsEntered = 0;
				for (int i = 0; i < hits.length() && i < MAX_REVISIONS && commitsEntered < numberOfEntries; i++) {
					Document doc = hits.doc(i);
					String revision = doc.get(FIELD_REVISIONNUMBER);

					// Get all the issue keys mentioned in the commit.
					String[] issueKeys = doc.getValues(FIELD_ISSUEKEY);
					if (issueKeys == null) {
						log.warn("getLogEntriesByProject() Revision " + revision + " does not have any issues.");
						continue;
					}

					// Check that the user has view permission for at least one of the issues
					boolean hasPermission = false;
					for (int index = 0; !hasPermission && index < issueKeys.length; index++) {
						// Look up the issue
						Issue issue = issueManager.getIssueObject(issueKeys[index]);
						if (issue != null) {
							hasPermission = permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue,
									user);
						}
					}

					if (hasPermission) {
						long repositoryId = Long.parseLong(doc.get(FIELD_REPOSITORY));
						// repositoryId is UUID + location
						GitManager manager = multipleGitRepositoryManager.getRepository(repositoryId);
						RevCommit logEntry = manager.getLogEntry(revision);
						if (logEntry == null) {
							log.error("getLogEntriesByProject() Could not find log message for revision: " + revision);
							continue;
						}
						// Look up the list of map entries for this repository.
						// Create one if needed
						List<RevCommit> entries = (List<RevCommit>) logEntries.get(new Long(repositoryId));
						if (entries == null) {
							entries = new ArrayList<RevCommit>();
							logEntries.put(new Long(repositoryId), entries);
						}

						// Add this entry
						entries.add(logEntry);
						commitsEntered++;
					}
				}
			} finally {
				searcher.close();
				reader.close();
			}

			return logEntries;
		}
	}

	/**
	 * This method returns the log entries collected from Git categorized by the repository it came from. NOTE: a null
	 * map will be returned if the indexes for this plugin have not yet been initialized.
	 * <p/>
	 * This method uses the Version Manager to look up all issues affected by and fixed by the supplied {@link Version}.
	 * The Lucene index is then used to look up all the commits for all the issues.
	 * 
	 * @param version
	 *            the version to get entries for.
	 * @param user
	 *            The remote user -- we need to check that the user has "View Version Control" permission for an issue
	 *            before we show a commit for it.
	 * @param numberOfEntries
	 *            How many entries to fetch.
	 * @return A map with key of repository display name and entry of a collections of Logs. <code>null</code> if the
	 *         repository has not yet been initialized.
	 * @throws com.atlassian.jira.issue.index.IndexException
	 *             if the Lucene index reader cannot be retrieved
	 * @throws java.io.IOException
	 *             if reading the Lucene index fails
	 */
	public Map<Long, Collection<RevCommit>> getLogEntriesByVersion(Version version, User user, int numberOfEntries)
			throws IndexException, IOException {
		if (version == null || numberOfEntries < 0) {
			throw new IllegalArgumentException("getLogEntriesByVersion(" + version + ")");
		}
		if (log.isDebugEnabled()) {
			log.debug("getLogEntriesByVersion(" + version + ", " + numberOfEntries + ")");
		}

		if (!indexDirectoryExists()) {
			log.warn("getLogEntriesByVersion() The indexes for the Git plugin have not yet been created.");
			return null;
		}

		// Find all isuses affected by and fixed by any of the versions:
		Collection<GenericValue> issues = new HashSet<GenericValue>();

		try {
			issues.addAll(versionManager.getFixIssues(version));
			issues.addAll(versionManager.getAffectsIssues(version));
		} catch (GenericEntityException e) {
			log.error("getLogEntriesByVersion() Caught exception while looking up issues related to version "
					+ version.getName() + "!", e);
			// Keep going. We may have got some issues stored.
		}

		// Construct a query with all the issue keys. Make sure to increase the  maximum number of clauses if needed.
		int maxClauses = BooleanQuery.getMaxClauseCount();
		if (issues.size() > maxClauses) {
			BooleanQuery.setMaxClauseCount(issues.size());
		}
		BooleanQuery query = new BooleanQuery();

		for (GenericValue issue : issues) {
			String key = issue.getString(FIELD_ISSUEKEY);
			TermQuery termQuery = new TermQuery(new Term(FIELD_ISSUEKEY, key));
			query.add(termQuery, BooleanClause.Occur.SHOULD);
		}

		final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
		IndexSearcher searcher = new IndexSearcher(reader);
		Map<Long, Collection<RevCommit>> logEntries;

		try {
			// Run the query and sort by date in descending order
			Sort sort = new Sort(FIELD_DATE, true);
			Hits hits = searcher.search(query, sort);

			if (hits == null) {
				log.info("getLogEntriesByVersion() No matches -- returning null.");
				return null;
			}

			logEntries = new HashMap<Long, Collection<RevCommit>>(hits.length());
			int commitsEntered = 0;
			for (int i = 0; i < hits.length() && i < MAX_REVISIONS && commitsEntered < numberOfEntries; i++) {
				Document doc = hits.doc(i);
				long repositoryId = Long.parseLong(doc.get(FIELD_REPOSITORY));
				// repositoryId is UUID + location
				GitManager manager = multipleGitRepositoryManager.getRepository(repositoryId);
				String revision = doc.get(FIELD_REVISIONNUMBER);

				// Get all the issue keys mentioned in the commit.
				String[] issueKeys = doc.getValues(FIELD_ISSUEKEY);
				if (issueKeys == null) {
					log.warn("getLogEntriesByProject() Revision " + revision + " does not have any issues.");
					continue;
				}
				// Check that the user has view permission for at least one of the issues
				boolean hasPermission = false;
				for (int index = 0; !hasPermission && index < issueKeys.length; index++) {
					// Look up the issue
					Issue issue = issueManager.getIssueObject(issueKeys[index]);
					if (issue != null) {
						hasPermission = permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
					}
				}

				if (hasPermission) {
					RevCommit logEntry = manager.getLogEntry(revision);
					if (logEntry == null) {
						log.error("getLogEntriesByVersion() Could not find log message for revision: "
								+ doc.get(FIELD_REVISIONNUMBER));
					}
					// Add the entry to the list of map entries for the repository. Create a new list if needed
					List<RevCommit> entries = (List<RevCommit>) logEntries.get(new Long(repositoryId));
					if (entries == null) {
						entries = new ArrayList<RevCommit>();
						logEntries.put(new Long(repositoryId), entries);
					}
					entries.add(logEntry);
					commitsEntered++;
				}
			}
		} finally {
			searcher.close();
			reader.close();
			BooleanQuery.setMaxClauseCount(maxClauses);
		}

		return logEntries;
	}

	public void addRepository(GitManager gitInstance) {
		initializeLatestIndexedRevisionCache(gitInstance);
		try {
			updateIndex();
		} catch (Exception e) {
			throw new InfrastructureException("Could not index repository", e);
		}
	}

	public void removeEntries(GitManager gitInstance) throws IOException, IndexException {
		if (log.isDebugEnabled()) {
			log.debug("Deleteing revisions for : " + gitInstance.getRoot());
		}

		if (!indexDirectoryExists()) {
			log.warn("The indexes for the Git plugin have not yet been created.");
			return;
		} else {
			long repoId = gitInstance.getId();

			final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());

			try {
				reader.deleteDocuments(new Term(FIELD_REPOSITORY, Long.toString(repoId)));
				initializeLatestIndexedRevisionCache(gitInstance);
			} finally {
				reader.close();
			}
		}
	}
}