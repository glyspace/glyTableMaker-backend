package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.persistence.UserEntity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name="glycan_tags", uniqueConstraints=
     @UniqueConstraint(columnNames={"label", "userid"}))	    
@XmlRootElement (name="glycantag")
@JsonSerialize
public class GlycanTag {
	
	Long tagId;
	UserEntity user;
	String label;
	
    @Id
    @GeneratedValue
    @Column(name="glycan_tag_id", unique = true, nullable = false)
	public Long getTagId() {
		return tagId;
	}
	public void setTagId(Long keywordId) {
		this.tagId = keywordId;
	}
	
	@ManyToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
	@JoinColumn(nullable = false, name = "userid", foreignKey = @ForeignKey(name = "FK_VERIFY_USER"))
	public UserEntity getUser() {
		return user;
	}
	public void setUser(UserEntity user) {
		this.user = user;
	}
	
	@Column(nullable=false, length=128)
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}

}
