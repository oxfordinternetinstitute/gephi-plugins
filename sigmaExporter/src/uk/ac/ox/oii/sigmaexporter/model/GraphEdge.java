/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ox.oii.sigmaexporter.model;

public class GraphEdge extends GraphElement{
	
		
	private String label;
	private String source;
	private String target;
	
	public GraphEdge(String id) {
		super(id);
		label=id;
		source="";
		target="";
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	
	
	

}
