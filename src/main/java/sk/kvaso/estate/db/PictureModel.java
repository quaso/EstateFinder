package sk.kvaso.estate.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "Picture")
public class PictureModel {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long ID;
	
	@Lob
	@Column(length=100000)
	private byte[] DATA;
}
