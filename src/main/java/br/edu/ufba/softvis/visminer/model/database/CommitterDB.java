package br.edu.ufba.softvis.visminer.model.database;

import java.io.Serializable;

import javax.persistence.*;

import java.util.List;


/**
 * @author Felipe Gustavo de Souza Gomes (felipegustavo1000@gmail.com)
 * @version 0.9
 * The persistent class for the committer database table.
 */
@Entity
@Table(name="committer")
@NamedQueries({
	@NamedQuery(name="CommitterDB.findByEmail", query="select c from CommitterDB c where c.email = :email"),
	@NamedQuery(name="CommitterDB.findByRepository", query="select c from CommitterDB c join c.repositories r where r.id = :id")
})
public class CommitterDB implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="COMMITTER_ID_GENERATOR", sequenceName="COMMITTER_SEQ")
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="COMMITTER_ID_GENERATOR")
	@Column(unique=true, nullable=false)
	private int id;

	@Column(unique=true, nullable=false, length=200)
	private String email;

	@Column(nullable=false, length=100)
	private String name;

	//bi-directional many-to-one association to CommitDB
	@OneToMany(mappedBy="committer")
	private List<CommitDB> commits;

	@ManyToMany
	@JoinTable(
		name="committer_contribute_repository",
		joinColumns={
			@JoinColumn(name="committer_id", nullable=false)
		},
		inverseJoinColumns={
			@JoinColumn(name="repository_id", nullable=false)
		}
	)
	private List<RepositoryDB> repositories;
	
	public CommitterDB() {
	}

	/**
	 * @param id
	 * @param email
	 * @param name
	 */
	public CommitterDB(int id, String email, String name) {
		super();
		this.id = id;
		this.email = email;
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the commits
	 */
	public List<CommitDB> getCommits() {
		return commits;
	}

	/**
	 * @param commits the commits to set
	 */
	public void setCommits(List<CommitDB> commits) {
		this.commits = commits;
	}

	/**
	 * @return the repositories
	 */
	public List<RepositoryDB> getRepositories() {
		return repositories;
	}

	/**
	 * @param repositories the repositories to set
	 */
	public void setRepositories(List<RepositoryDB> repositories) {
		this.repositories = repositories;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommitterDB other = (CommitterDB) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		return true;
	}

	
	
}