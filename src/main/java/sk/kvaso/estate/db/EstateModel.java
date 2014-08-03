package sk.kvaso.estate.db;

import java.sql.Timestamp;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "Estate")
public class EstateModel {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long ID;
	
	@Column
	private String STREET;
	
	@Column
	private int AREA;
	
	@Column
	private long PRICE;
	
	@Column
	private String TEXT;
	
	@Column
	private String URL;
	
	@OneToMany(orphanRemoval=true)
    @JoinColumn(name="ESTATE_ID", referencedColumnName="ID")
    private Set<PictureModel> PICTURES;
	
	@Column
	private Timestamp TIMESTAMP;

}
