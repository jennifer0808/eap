package cn.tzauto.octopus.secsLayer.domain;

/*
 * this is the class for all domain classes with string id.
 */
public class DomainObject 
{
	protected String id;
	
	public String toString(){
		return id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
