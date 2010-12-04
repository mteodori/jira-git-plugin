/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (c) 2010, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.xiplink.jira.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

public class FileDiff {

    private final RevCommit commit;
    private DiffEntry diffEntry;

    private static ObjectId[] trees(final RevCommit commit) {
        final ObjectId[] r = new ObjectId[commit.getParentCount() + 1];
        for (int i = 0; i < r.length - 1; i++) {
            r[i] = commit.getParent(i).getTree().getId();
        }
        r[r.length - 1] = commit.getTree().getId();
        return r;
    }

    static FileDiff[] compute(final TreeWalk walk, final RevCommit commit)
            throws MissingObjectException, IncorrectObjectTypeException,
            CorruptObjectException, IOException {
        final ArrayList<FileDiff> r = new ArrayList<FileDiff>();

        if (commit.getParentCount() > 0) {
            walk.reset(trees(commit));
        } else {
            walk.reset();
            walk.addTree(new EmptyTreeIterator());
            walk.addTree(commit.getTree());
        }

        if (walk.getTreeCount() <= 2) {
            List<DiffEntry> entries = DiffEntry.scan(walk);
            for (DiffEntry entry : entries) {
                final FileDiff d = new FileDiff(commit, entry);
                r.add(d);
            }
        } else { // DiffEntry does not support walks with more than two trees
            final int nTree = walk.getTreeCount();
            final int myTree = nTree - 1;
            while (walk.next()) {
                if (matchAnyParent(walk, myTree)) {
                    continue;
                }

                final FileDiffForMerges d = new FileDiffForMerges(commit);
                d.path = walk.getPathString();
                int m0 = 0;
                for (int i = 0; i < myTree; i++) {
                    m0 |= walk.getRawMode(i);
                }
                final int m1 = walk.getRawMode(myTree);
                d.change = ChangeType.MODIFY;
                if (m0 == 0 && m1 != 0) {
                    d.change = ChangeType.ADD;
                } else if (m0 != 0 && m1 == 0) {
                    d.change = ChangeType.DELETE;
                } else if (m0 != m1 && walk.idEqual(0, myTree)) {
                    d.change = ChangeType.MODIFY; // there is no ChangeType.TypeChanged
                }
                d.blobs = new ObjectId[nTree];
                d.modes = new FileMode[nTree];
                for (int i = 0; i < nTree; i++) {
                    d.blobs[i] = walk.getObjectId(i);
                    d.modes[i] = walk.getFileMode(i);
                }
                r.add(d);
            }

        }

        final FileDiff[] tmp = new FileDiff[r.size()];
        r.toArray(tmp);
        return tmp;
    }

    private static boolean matchAnyParent(final TreeWalk walk, final int myTree) {
        final int m = walk.getRawMode(myTree);
        for (int i = 0; i < myTree; i++) {
            if (walk.getRawMode(i) == m && walk.idEqual(i, myTree)) {
                return true;
            }
        }
        return false;
    }

    public RevCommit getCommit() {
        return commit;
    }

    public String getPath() {
        if (ChangeType.DELETE.equals(diffEntry.getChangeType())) {
            return diffEntry.getOldPath();
        }
        return diffEntry.getNewPath();
    }

    public ChangeType getChange() {
        return diffEntry.getChangeType();
    }

    public ObjectId[] getBlobs() {
        List<ObjectId> objectIds = new ArrayList<ObjectId>();
        if (diffEntry.getOldId() != null) {
            objectIds.add(diffEntry.getOldId().toObjectId());
        }
        if (diffEntry.getNewId() != null) {
            objectIds.add(diffEntry.getNewId().toObjectId());
        }
        return objectIds.toArray(new ObjectId[]{});
    }

    public FileMode[] getModes() {
        List<FileMode> modes = new ArrayList<FileMode>();
        if (diffEntry.getOldMode() != null) {
            modes.add(diffEntry.getOldMode());
        }
        if (diffEntry.getOldMode() != null) {
            modes.add(diffEntry.getOldMode());
        }
        return modes.toArray(new FileMode[]{});
    }

    FileDiff(final RevCommit c, final DiffEntry entry) {
        diffEntry = entry;
        commit = c;
    }

    private static class FileDiffForMerges extends FileDiff {

        private String path;
        private ChangeType change;
        private ObjectId[] blobs;
        private FileMode[] modes;

        private FileDiffForMerges(final RevCommit c) {
            super(c, null);
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public ChangeType getChange() {
            return change;
        }

        @Override
        public ObjectId[] getBlobs() {
            return blobs;
        }

        @Override
        public FileMode[] getModes() {
            return modes;
        }
    }
}
