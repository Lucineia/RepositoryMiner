package org.repositoryminer.persistence.dao;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/**
 * This class handles rm_code_analysis collection.
 */
public class CodeAnalysisDAO extends GenericDAO {

	private static final String COLLECTION_NAME = "rm_code_analysis";

	public CodeAnalysisDAO() {
		super(COLLECTION_NAME);
	}

	/**
	 * Retrieves an entire analysis by file and commit.
	 * 
	 * @param fileHash
	 *            the file hash.
	 * @param commit
	 *            the commit id.
	 * @param projection
	 *            the query projection.
	 * @return the code analysis.
	 */
	public Document findByFileAndCommit(long fileHash, String commit, Bson projection) {
		return findOne(Filters.and(Filters.eq("filehash", fileHash), Filters.eq("commit", commit)), projection);
	}

}