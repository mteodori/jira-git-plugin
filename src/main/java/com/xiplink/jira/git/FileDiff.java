/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package com.xiplink.jira.git;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.treewalk.TreeWalk;
/**
 * Taken from egit ui
 */
public class FileDiff {	
	
	private static ObjectId[] trees(final RevCommit commit) {
		final List<ObjectId> r = new ArrayList<ObjectId>(commit.getParentCount() + 1);
		for (RevCommit parent : commit.getParents()) {
			if (parent != null && parent.getTree() != null)
			{
				// TODO why is this null?
				r.add(parent.getTree().getId());
			}
		}
		r.add(commit.getTree().getId());
		return r.toArray(new ObjectId[r.size()]);
	}

	/**
	 * 
	 * @param walk
	 * @param commit
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws CorruptObjectException
	 * @throws IOException
	 * @return Array of filediff todo
	 */
	public static FileDiff[] compute(final TreeWalk walk, final RevCommit commit)
			throws MissingObjectException, IncorrectObjectTypeException,
			CorruptObjectException, IOException {
		final List<FileDiff> r = new ArrayList<FileDiff>();

		walk.reset(trees(commit));
		final int nTree = walk.getTreeCount();
		final int myTree = nTree - 1;

		switch (nTree) {
		case 1:
			while (walk.next()) {
				final FileDiff d = new FileDiff(commit, walk.getPathString());
				d.change = "A";
				d.blobs = new ObjectId[] { walk.getObjectId(0) };
				r.add(d);
			}
			break;
		case 2:
			while (walk.next()) {
				final FileDiff d = new FileDiff(commit, walk.getPathString());
				final ObjectId id0 = walk.getObjectId(0);
				final ObjectId id1 = walk.getObjectId(1);
				d.change = "M";
				d.blobs = new ObjectId[] { id0, id1 };

				final int m0 = walk.getRawMode(0);
				final int m1 = walk.getRawMode(1);
				if (m0 == 0 && m1 != 0)
					d.change = "A";
				else if (m0 != 0 && m1 == 0)
					d.change = "D";
				else if (m0 != m1 && walk.idEqual(0, 1))
					d.change = "T";
				r.add(d);
			}
			break;
		default:
			while (walk.next()) {
				if (matchAnyParent(walk, myTree))
					continue;

				final FileDiff d = new FileDiff(commit, walk.getPathString());
				int m0 = 0;
				for (int i = 0; i < myTree; i++)
					m0 |= walk.getRawMode(i);
				final int m1 = walk.getRawMode(myTree);
				d.change = "M";
				if (m0 == 0 && m1 != 0)
					d.change = "A";
				else if (m0 != 0 && m1 == 0)
					d.change = "D";
				else if (m0 != m1 && walk.idEqual(0, myTree))
					d.change = "T";
				d.blobs = new ObjectId[nTree];
				for (int i = 0; i < nTree; i++)
					d.blobs[i] = walk.getObjectId(i);
				r.add(d);
			}
			break;
		}

		final FileDiff[] tmp = new FileDiff[r.size()];
		r.toArray(tmp);
		return tmp;
	}

	private static boolean matchAnyParent(final TreeWalk walk, final int myTree) {
		final int m = walk.getRawMode(myTree);
		for (int i = 0; i < myTree; i++)
			if (walk.getRawMode(i) == m && walk.idEqual(i, myTree))
				return true;
		return false;
	}

	/**
	 * 
	 */
	public final RevCommit commit;

	/**
	 * foo
	 */
	public final String path;

	/** [A|M|D|T] */
	public String change;

	public ObjectId[] blobs;

	FileDiff(final RevCommit c, final String p) {
		commit = c;
		path = p;
	}
}
