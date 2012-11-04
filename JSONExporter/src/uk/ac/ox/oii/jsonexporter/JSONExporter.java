/*
Copyright (C) 2012  Scott A. Hale
Website: http://www.scotthale.net/

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/license
 */
package uk.ac.ox.oii.jsonexporter;

import java.io.Writer;
import org.gephi.graph.api.Attributes;
import org.gephi.graph.api.EdgeIterable;
import org.gephi.graph.api.EdgeIterator;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.project.api.Workspace;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeValue;
import org.gephi.graph.api.NodeData;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.NodeIterator;

/**
 *
 * @author shale
 */
public class JSONExporter implements GraphExporter, LongTask, CharacterExporter {

    private boolean exportVisible = false;
    private Workspace workspace;
    private Writer writer;
    private ProgressTicket progress;
    private boolean cancel = false;
    //private File path;

    @Override
    public boolean execute() {
        try {
            if (writer != null) {//path.getParentFile().exists()
                GraphModel graphModel = workspace.getLookup().lookup(GraphModel.class);
                Graph graph = null;
                if (exportVisible) {
                    graph = graphModel.getGraphVisible();
                } else {
                    graph = graphModel.getGraph();
                }

                //Count the number of tasks (nodes + edges) and start the progress
                int tasks = graph.getNodeCount() + graph.getEdgeCount();
                Progress.start(progress, tasks);

                //FileWriter fwriter = new  FileWriter(writer);
                writer.write("{\"nodes\":[");


                //EdgeIterable eIt = graph.getEdges();
                //Export nodes. Progress is incremented at each step.
                Node[] nodeArray = graph.getNodes().toArray();
                for (int i = 0; i < nodeArray.length; i++) {
                    //NodeIterator nIt = graph.getNodes().iterator();
                    //while (nIt.hasNext()) {
                    Node n = nodeArray[i];//nIt.next();
                    NodeData nd = n.getNodeData();
                    String id = nd.getId();
                    String label = nd.getLabel();
                    float x = nd.x();
                    float y = nd.y();
                    float size = nd.getSize();
                    String color = "rgb(" + (int) (nd.r() * 255) + "," + (int) (nd.g() * 255) + "," + (int) (nd.b() * 255) + ")";

                    StringBuilder sb = new StringBuilder();
                    if (i != 0) {
                        sb.append(",\n");//No comma after last one (nor before first one)
                    }
                    sb.append("{\"id\":\"" + id + "\", \"label\":\"" + label + "\",");
                    sb.append("\"x\":" + x + ",\"y\":" + y + ",");
                    sb.append("\"size\":" + size + ",\"color\":\"" + color + "\",\"attributes\":{");


                    //Map<String,String> attr = new  HashMap<String,String>();
                    AttributeRow nAttr = (AttributeRow) nd.getAttributes();
                    boolean first = true;
                    for (int j = 0; j < nAttr.countValues(); j++) {
                        Object valObj = nAttr.getValue(j);
                        if (valObj == null) {
                            continue;
                        }
                        String val = valObj.toString();
                        AttributeColumn col = nAttr.getColumnAt(j);
                        if (col == null) {
                            continue;
                        }
                        String name = col.getTitle();
                        if (name.equalsIgnoreCase("Id") || name.equalsIgnoreCase("Label")
                                || name.equalsIgnoreCase("uid")) {
                            continue;
                        }
                        // attr.put(name,val);
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        sb.append("\"" + name + "\":\"" + val + "\"");
                    }
                    sb.append("}}");

                    writer.write(sb.toString());
                    if (cancel) {
                        return false;
                    }
                    Progress.progress(progress);
                }
                writer.write("],\"edges\":[");

                //Export edges. Progress is incremented at each step.
                Edge[] edgeArray = graph.getEdges().toArray();
                for (int i = 0; i < edgeArray.length; i++) {
                    //EdgeIterator eIt = graph.getEdges().iterator();
                    //while (eIt.hasNext()) {
                    Edge e = edgeArray[i];//eIt.next();
                    String sourceId = e.getSource().getNodeData().getId();
                    String targetId = e.getTarget().getNodeData().getId();
                    String weight = String.valueOf(e.getWeight());
                    //e.getEdgeData().r();gb of edge data
                    //Write to file
                    if (i != 0) {
                        writer.write(",\n");//No comma after last one
                    }
                    writer.write("{\"source\":\"" + sourceId + "\",\"target\":\"" + targetId + "\"");
                    writer.write(",\"weight\":\"" + weight + "\"");
                    writer.write(",\"id\":\"" + e.getId() + "\"}");
                    if (cancel) {
                        return false;
                    }
                    Progress.progress(progress);
                }
                writer.write("]}");

                //Finish progress
                Progress.finish(progress);
                return true;
            } else {
                throw new FileNotFoundException("Writer is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            /*try {
            /if (writer != null) {
            writer.close();
            }
            } catch (java.io.IOException e) {
            // failed to close file
            System.err.println(e);
            }*/
        }
    }

    /*     public File getPath() {
    return path;
    }
    
    public void setPath(File path) {
    this.path = path;
    }*/
    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void setExportVisible(boolean exportVisible) {
        this.exportVisible = exportVisible;
    }

    @Override
    public boolean isExportVisible() {
        return exportVisible;
    }

    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        this.progress = pt;
    }
}