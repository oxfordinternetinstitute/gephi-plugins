/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ox.oii.sigmaexporter.model;

/**
 *
 * @author shale
 */
public class GraphNode  extends GraphElement{
	
	private String label;
	private double size;
	private double x;
	private double y;
	private String color;
			
	public GraphNode(String id) {
		super(id);
		label="";
		size=1;
		x = 100 - 200*Math.random();
		y = 100 - 200*Math.random();
		color="";
	}
	
	

	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	

}

