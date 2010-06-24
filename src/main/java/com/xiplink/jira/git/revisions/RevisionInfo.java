package com.xiplink.jira.git.revisions;

import org.eclipse.jgit.revwalk.RevCommit;

public class RevisionInfo {
    private long repositoryId;
    private String branch;
    private RevCommit commit;

    public long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public RevCommit getCommit() {
        return commit;
    }

    public void setCommit(RevCommit commit) {
        this.commit = commit;
    }
}
