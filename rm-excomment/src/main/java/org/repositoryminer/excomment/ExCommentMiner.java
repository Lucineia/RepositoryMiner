package org.repositoryminer.excomment;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.repositoryminer.domain.Commit;
import org.repositoryminer.domain.Reference;
import org.repositoryminer.domain.ReferenceType;
import org.repositoryminer.domain.Repository;
import org.repositoryminer.excomment.model.Comment;
import org.repositoryminer.excomment.model.Heuristic;
import org.repositoryminer.excomment.model.Pattern;
import org.repositoryminer.excomment.persistence.ExCommentDAO;
import org.repositoryminer.persistence.dao.CommitDAO;
import org.repositoryminer.persistence.dao.ReferenceDAO;
import org.repositoryminer.persistence.dao.RepositoryDAO;
import org.repositoryminer.util.HashingUtils;

import com.mongodb.client.model.Projections;

public class ExCommentMiner {

	private static final String[] COMMENTS_HEADER = { "idcomment", "total_pattern", "total_heuristic", "total_score",
			"comment", "path", "class", "method" };
	private static final String[] PATTERNS_HEADER = { "idcomment", "pattern", "pattern_score", "pattern_class", "theme",
			"tdtype" };
	private static final String[] HEURISTICS_HEADER = { "idcomment", "heuristic_description", "heuristic_status",
			"heuristic_score" };

	private String commentsCSV, patternsCSV, heuristicsCSV;
	private char delimiter = ';';
	private Repository repository;

	private CommitDAO commitPersist = new CommitDAO();
	private ReferenceDAO refPersist = new ReferenceDAO();
	private ExCommentDAO exCommPersist = new ExCommentDAO();

	// the comment id is used as key
	private Map<Integer, Comment> commentsMap;

	// keeps the relationship between files and comments
	private Map<String, List<Integer>> filesMap;

	public ExCommentMiner(Repository repository) {
		this.repository = repository;
	}

	public ExCommentMiner(String repositoryId) {
		RepositoryDAO repoHandler = new RepositoryDAO();
		this.repository = Repository
				.parseDocument(repoHandler.findById(repositoryId, Projections.include("scm", "path")));
	}

	public void setCommentsCSV(String commentsCSV) {
		this.commentsCSV = commentsCSV;
	}

	public void setPatternsCSV(String patternsCSV) {
		this.patternsCSV = patternsCSV;
	}

	public void setHeuristicsCSV(String heuristicsCSV) {
		this.heuristicsCSV = heuristicsCSV;
	}

	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}

	public void execute(String hash) throws IOException {
		persistAnalysis(hash, null);
	}

	public void execute(String name, ReferenceType type) throws IOException {
		Document refDoc = refPersist.findByNameAndType(name, type, repository.getId(), Projections.slice("commits", 1));
		Reference reference = Reference.parseDocument(refDoc);

		String commitId = reference.getCommits().get(0);
		persistAnalysis(commitId, reference);
	}

	private void persistAnalysis(String commitId, Reference ref) throws IOException {
		Commit commit = Commit.parseDocument(commitPersist.findById(commitId, Projections.include("commit_date")));

		readCSVs();

		List<Document> documents = new ArrayList<Document>(filesMap.size());
		for (Entry<String, List<Integer>> entry : filesMap.entrySet()) {
			Document doc = new Document();

			if (ref != null) {
				doc.append("reference", ref.getPath());
			}

			doc.append("commit", commit.getId());
			doc.append("commit_date", commit.getCommitDate());
			doc.append("repository", new ObjectId(repository.getId()));
			doc.append("filename", entry.getKey());
			doc.append("filehash", HashingUtils.encodeToCRC32(entry.getKey()));

			List<Comment> commentsAux = new ArrayList<Comment>(entry.getValue().size());
			for (Integer i : entry.getValue()) {
				commentsAux.add(commentsMap.get(i));
			}
			doc.append("comments", Comment.toDocumentList(commentsAux));

			documents.add(doc);
		}

		exCommPersist.insertMany(documents);
	}

	private List<CSVRecord> readCSV(String[] header, String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);

		CSVFormat format = CSVFormat.DEFAULT.withDelimiter(delimiter).withHeader(header).withSkipHeaderRecord();

		CSVParser csvParser = new CSVParser(fileReader, format);

		List<CSVRecord> records = csvParser.getRecords();

		fileReader.close();
		csvParser.close();

		return records;
	}

	private void readCSVs() throws IOException {
		commentsMap = new HashMap<Integer, Comment>();
		filesMap = new HashMap<String, List<Integer>>();

		readComments();
		readHeuristics();
		readPatterns();
	}

	private void readComments() throws IOException {
		List<CSVRecord> records = readCSV(COMMENTS_HEADER, commentsCSV);

		for (CSVRecord record : records) {
			Comment comment = new Comment(Integer.parseInt(record.get(0)),
					Float.parseFloat(record.get(1).replaceAll(",", ".")),
					Float.parseFloat(record.get(2).replaceAll(",", ".")),
					Float.parseFloat(record.get(3).replaceAll(",", ".")), record.get(4), record.get(6), record.get(7));

			String filename = FilenameUtils.normalize(record.get(5), true);
			filename = filename.substring(repository.getPath().length() + 1);

			if (!filesMap.containsKey(filename)) {
				filesMap.put(filename, new ArrayList<Integer>());
			}

			commentsMap.put(comment.getId(), comment);
			filesMap.get(filename).add(comment.getId());
		}
	}

	private void readPatterns() throws IOException {
		List<CSVRecord> records = readCSV(PATTERNS_HEADER, patternsCSV);

		for (CSVRecord record : records) {
			Pattern pattern = new Pattern(record.get(1), Float.parseFloat(record.get(2).replaceAll(",", ".")),
					record.get(3), record.get(4), record.get(5));

			Comment comment = commentsMap.get(Integer.parseInt(record.get(0)));
			if (comment == null) {
				continue;
			}

			comment.getPatterns().add(pattern);
		}
	}

	private void readHeuristics() throws IOException {
		List<CSVRecord> records = readCSV(HEURISTICS_HEADER, heuristicsCSV);

		for (CSVRecord record : records) {
			Heuristic heuristic = new Heuristic(record.get(1), Integer.parseInt(record.get(2)),
					Float.parseFloat(record.get(3).replaceAll(",", ".")));

			Comment comment = commentsMap.get(Integer.parseInt(record.get(0)));
			if (comment == null) {
				continue;
			}

			comment.getHeuristics().add(heuristic);
		}
	}

}