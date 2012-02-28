package com.xiplink.jira.git.revisions;

import com.atlassian.core.exception.InfrastructureException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;

/**
 * An implementation that uses JIRA's LuceneUtils for its guts.
 *
 * @since 0.9.12
 */
class DefaultLuceneIndexAccessor implements LuceneIndexAccessor {

    public IndexReader getIndexReader(String path) {
        try {
            return IndexReader.open(new NIOFSDirectory(new File(path)), true);
        } catch (Exception e) {
            throw new InfrastructureException(e);
        }
    }

    public IndexWriter getIndexWriter(String path, boolean create) {
        try {
            return new IndexWriter(new NIOFSDirectory(new File(path)), new StandardAnalyzer(org.apache.lucene.util.Version.LUCENE_29), create);
        } catch (Exception e) {
            throw new InfrastructureException(e);
        }
    }
}
