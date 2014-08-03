package sk.kvaso.estate.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Estate")
public class EstateModel {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	private long ID;
}
