/*
 * User: Mike
 * Date: Oct 1, 2004
 * Time: 4:58:40 PM
 */
package com.xiplink.jira.git.revisions;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.service.ServiceManager;
import com.atlassian.jira.util.JiraKeyUtils;
import com.opensymphony.user.User;
import com.opensymphony.util.TextUtils;
import com.xiplink.jira.git.GPropertiesLoader;
import com.xiplink.jira.git.GitManager;
import com.xiplink.jira.git.MultipleGitRepositoryManager;
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
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class RevisionIndexer {

    private static Logger log = Logger.getLogger(RevisionIndexer.class);
    private static final String REVISIONS_INDEX_DIRECTORY = "jira-git-revisions";
    // These are names of the fields in the Lucene documents that contain revision info.
    public static final String FIELD_REVISIONNUMBER = "revision";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_AUTHOR = "author";
    public static final String FIELD_COMMITTER = "committer";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_ISSUEKEY = "key";
    public static final String FIELD_PROJECTKEY = "project";
    public static final String FIELD_REPOSITORY = "repository";
    public static final String FIELD_BRANCH = "branch";
    public static final StandardAnalyzer ANALYZER = new StandardAnalyzer();
    public static final int MAX_REVISIONS = 100;
    private final MultipleGitRepositoryManager multipleGitRepositoryManager;
    private final VersionManager versionManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final ServiceManager serviceManager;
    private final IndexPathManager indexPathManager;
    private final LuceneIndexAccessor indexAccessor;

    public RevisionIndexer(
            MultipleGitRepositoryManager multipleGitRepositoryManager,
            VersionManager versionManager,
            IssueManager issueManager,
            PermissionManager permissionManager,
            ServiceManager serviceManager,
            IndexPathManager indexPathManager) {

        this(multipleGitRepositoryManager, versionManager, issueManager, permissionManager,
                serviceManager, new DefaultLuceneIndexAccessor(), indexPathManager);
    }

    RevisionIndexer(
            MultipleGitRepositoryManager multipleGitRepositoryManager,
            VersionManager versionManager,
            IssueManager issueManager,
            PermissionManager permissionManager,
            ServiceManager serviceManager,
            LuceneIndexAccessor accessor,
            IndexPathManager indexPathManager) {
        this.multipleGitRepositoryManager = multipleGitRepositoryManager;
        this.versionManager = versionManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.indexAccessor = accessor;
        this.serviceManager = serviceManager;
        this.indexPathManager = indexPathManager;
    }

    public void start() {
        try {
            createIndexIfNeeded();
            RevisionIndexService.install(serviceManager); // ensure the changes index service
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
                return true;
            } catch (Exception e) {
                log.error("Could not create the index directory for the Git plugin.", e);
                return false;
            }
        } else {
            return indexExists;
        }
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
        String rootIndexPath = indexPathManager.getPluginIndexRootPath();
        if (rootIndexPath != null) {
            indexPath = rootIndexPath + System.getProperty("file.separator") + REVISIONS_INDEX_DIRECTORY;
        } else {
            log.warn("At the moment the root index path of jira is not set, so we can not form an index path for the git plugin.");
        }

        return indexPath;
    }

    /**
     * This method updates the index, creating it if it does not already exist.
     *
     * @throws IndexException
     *             if there is some problem in the indexing subsystem meaning indexes cannot be updated.
     */
    public void updateIndex() throws IndexException, IOException {
        updateIndex(new BranchFilter() {
            public Collection<String> filter(Collection<String> branches) {
                return branches;
            }
        });
    }

    public void updateIndex(final String branchName) throws IndexException, IOException {
        updateIndex(new BranchFilter() {
            public Collection<String> filter(Collection<String> branches) {
                if(branches.contains(branchName)) {
                    return Collections.singletonList(branchName);
                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    private interface BranchFilter {
        Collection<String> filter(Collection<String> branches);
    }

    private void updateIndex(BranchFilter branchFilter) throws IndexException, IOException {
        if (createIndexIfNeeded()) {
            Collection<GitManager> repositories = multipleGitRepositoryManager.getRepositoryList();

            // temp log comment
            if (log.isDebugEnabled()) {
                log.debug("repos size = " + repositories.size());
            }

            for (GitManager gitManager : repositories) {
                try {
                    // if the repository isn't active, try activating it. if it still not accessible, skip it
                    if (!gitManager.isActive()) {
                        gitManager.activate();

                        if (!gitManager.isActive()) {
                            continue;
                        }
                    }

                    gitManager.fetch();

                    long repoId = gitManager.getId();

                    Map<String, String> allBranches = gitManager.getBranches();
                    Collection<String> branchesNames = branchFilter.filter(allBranches.keySet());

                    for (String branchName : branchesNames) {
                        updateBranchIndex(repoId, branchName, allBranches, gitManager);
                    }
                } catch (IOException e) {
                    log.warn("Unable to index repository '" + gitManager.getDisplayName() + "'", e);
                } catch (RuntimeException e) {
                    log.warn("Unable to index repository '" + gitManager.getDisplayName() + "'", e);
                }
            }
        }
    }

    private void updateBranchIndex(long repoId, String branchName, Map<String, String> allBranches, GitManager gitManager)
            throws IOException, IndexException {

        String branchId = allBranches.get(branchName);
        String latestIndexedRevision =
                gitManager.getProperties().getString(MultipleGitRepositoryManager.GIT_BRANCH_INDEXED_REVISION + branchName);

        if (log.isDebugEnabled()) {
            log.info("Branch: " + branchName);
        }

        if (branchId.equals(latestIndexedRevision)) {
            if (log.isDebugEnabled()) {
                log.info("Branch index is up-to-date");
            }

            return;
        }

        String headId = gitManager.getRefId(Constants.HEAD);
        if((latestIndexedRevision == null) && !branchId.equals(headId)) {
            RevCommit base = gitManager.getMergeBase(headId, branchId);
            latestIndexedRevision = (base != null ? base.getId().getName() : null);

            if (log.isDebugEnabled()) {
                log.info("Branch was never indexed. Assuming start point: " + latestIndexedRevision);
            }
        } else {
         if (log.isDebugEnabled()) {
            log.info("Latest indexed revision is: " + latestIndexedRevision);
        }
        }

        if (log.isDebugEnabled()) {
            log.info("Updating to: " + branchId);
        }

        Collection<RevCommit> logEntries = gitManager.getLogEntries(latestIndexedRevision, branchId);

        IndexWriter writer = indexAccessor.getIndexWriter(getIndexPath(), false, ANALYZER);

        try {
            IndexReader reader = indexAccessor.getIndexReader(getIndexPath());

            try {
                for (RevCommit logEntry : logEntries) {
                    if (TextUtils.stringSet(logEntry.getFullMessage()) && isKeyInString(logEntry)) {
                        if (!hasDocument(repoId, branchName, logEntry.getId(), reader)) {

                            Document doc = getDocument(repoId, branchName, logEntry);
//                            if (log.isDebugEnabled()) {
//                                log.debug("Indexing repository=" + repoId + "; branch=" + branchName + "; revision="
//                                        + logEntry.getId());
//                            }
                            writer.addDocument(doc);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } finally {
            writer.close();
        }

        gitManager.getProperties().setString(MultipleGitRepositoryManager.GIT_BRANCH_INDEXED_REVISION + branchName, branchId);
    }

    protected boolean isKeyInString(RevCommit logEntry) {
        final String logMessageUpperCase = StringUtils.upperCase(logEntry.getFullMessage());
        return JiraKeyUtils.isKeyInString(logMessageUpperCase);
    }

    /**
     * Work out whether a given change, for the specified repository, is already in the index or not.
     */
    private boolean hasDocument(long repoId, String branchName, ObjectId revisionNumber, IndexReader reader) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        try {
            TermQuery repoQuery = new TermQuery(new Term(FIELD_REPOSITORY, Long.toString(repoId)));
            TermQuery branchQuery = new TermQuery(new Term(FIELD_BRANCH, branchName));
            TermQuery revQuery = new TermQuery(new Term(FIELD_REVISIONNUMBER, revisionNumber.name()));
            BooleanQuery repoAndRevQuery = new BooleanQuery();

            repoAndRevQuery.add(repoQuery, BooleanClause.Occur.MUST);
            repoAndRevQuery.add(branchQuery, BooleanClause.Occur.MUST);
            repoAndRevQuery.add(revQuery, BooleanClause.Occur.MUST);

            Hits hits = searcher.search(repoAndRevQuery);

            if (hits.length() == 1) {
                return true;
            } else if (hits.length() == 0) {
                return false;
            } else {
                log.error("Found MORE than one document for repository=" + repoId +
                            "; branch=" + branchName + "; revision=" + revisionNumber);
                return true;
            }
        } finally {
            searcher.close();
        }
    }

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
    protected Document getDocument(long repoId, String branchName, RevCommit logEntry) {
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
        doc.add(new Field(FIELD_BRANCH, branchName, Field.Store.YES, Field.Index.UN_TOKENIZED));

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
    public List<RevisionInfo> getLogEntriesByRepository(Issue issue)
            throws IndexException, IOException {
        
        if (log.isDebugEnabled()) {
            log.debug("Retrieving revisions for issue: " + issue.getKey());
        }

        if (!indexDirectoryExists()) {
            log.warn("The indexes for the Git plugin have not yet been created.");
            return null;
        }

        String key = issue.getKey();

        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
        IndexSearcher searcher = new IndexSearcher(reader);

        try {
            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(FIELD_ISSUEKEY, key)), BooleanClause.Occur.MUST);

            Hits hits = searcher.search(query);
            List<RevisionInfo> logEntries = new ArrayList<RevisionInfo>(hits.length());

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
                    RevisionInfo revInfo = new RevisionInfo();
                    revInfo.setRepositoryId(repositoryId);
                    revInfo.setBranch(doc.get(FIELD_BRANCH));
                    revInfo.setCommit(logEntry);

                    logEntries.add(revInfo);
                }
            }

            // sort by commit time - can we sort topologically? should
            // we?
            Collections.sort(logEntries, new Comparator<RevisionInfo>() {

                public int compare(RevisionInfo o, RevisionInfo o1) {
                    long r = o.getCommit().getCommitTime();
                    long r1 = o1.getCommit().getCommitTime();
                    if (r == r1) {
                        return 0;
                    } else if (r > r1) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });

            return logEntries;
        } finally {
            searcher.close();
            reader.close();
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
    public List<RevisionInfo> getLogEntriesByProject(String projectKey, User user, int numberOfEntries)
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
        }

        // Set up and perform a search for all documents having the supplied projectKey,sorted in descending date
        // order
        Sort sort = new Sort(FIELD_DATE, true);
        Term term = new Term(FIELD_PROJECTKEY, projectKey);
        TermQuery query = new TermQuery(term);

        List<RevisionInfo> logEntries = new ArrayList<RevisionInfo>();
        final IndexReader reader = indexAccessor.getIndexReader(getIndexPath());
        IndexSearcher searcher = new IndexSearcher(reader);
        try {
            Hits hits = searcher.search(query, sort);

            if (hits == null) {
                log.info("getLogEntriesByProject() No matches -- returning null.");
                return null;
            }
            // Build the result map
            logEntries = new ArrayList<RevisionInfo>(hits.length());
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

                    RevisionInfo revInfo = new RevisionInfo();
                    revInfo.setRepositoryId(repositoryId);
                    revInfo.setBranch(doc.get(FIELD_BRANCH));
                    revInfo.setCommit(logEntry);

                    logEntries.add(revInfo);
                    
                    commitsEntered++;
                }
            }
        } finally {
            searcher.close();
            reader.close();
        }

        return logEntries;
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
    public List<RevisionInfo> getLogEntriesByVersion(Version version, User user, int numberOfEntries)
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

        // Construct a query with all the issue keys. Make sure to increase the maximum number of clauses if needed.
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
        List<RevisionInfo> logEntries = new ArrayList<RevisionInfo>();

        try {
            // Run the query and sort by date in descending order
            Sort sort = new Sort(FIELD_DATE, true);
            Hits hits = searcher.search(query, sort);

            if (hits == null) {
                log.info("getLogEntriesByVersion() No matches -- returning null.");
                return null;
            }

            logEntries = new ArrayList<RevisionInfo>(hits.length());
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

                    RevisionInfo revInfo = new RevisionInfo();
                    revInfo.setRepositoryId(repositoryId);
                    revInfo.setBranch(doc.get(FIELD_BRANCH));
                    revInfo.setCommit(logEntry);

                    logEntries.add(revInfo);
                    
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
        }
        
        long repoId = gitInstance.getId();

        final IndexWriter writer = indexAccessor.getIndexWriter(getIndexPath(), false, null);

        try {
            writer.deleteDocuments(new Term(FIELD_REPOSITORY, Long.toString(repoId)));
        } finally {
            writer.close();
        }
    }
}
