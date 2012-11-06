/*
 Copyright Scott Hale, 2012
 * 
 
 Base on code from 
 Copyright 2008-2011 Gephi
 Authors : Mathieu Bastian <mathieu.bastian@gephi.org>
 Website : http://www.gephi.org

 Contributor(s):

 Portions Copyrighted 2011 Gephi Consortium.
 */
package uk.ac.ox.oii.sigmaexporter;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeValue;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeData;
import org.gephi.io.exporter.spi.ByteExporter;
import org.gephi.io.exporter.spi.CharacterExporter;
import org.gephi.io.exporter.spi.Exporter;
import org.gephi.project.api.Workspace;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import uk.ac.ox.oii.sigmaexporter.model.ConfigFile;
import uk.ac.ox.oii.sigmaexporter.model.GraphEdge;
import uk.ac.ox.oii.sigmaexporter.model.GraphElement;
import uk.ac.ox.oii.sigmaexporter.model.GraphNode;

public class SigmaExporter implements Exporter, LongTask {

    private ConfigFile config;
    private String path;
    private Workspace workspace;
    private ProgressTicket progress;
    private boolean cancel = false;

    @Override
    public boolean execute() {
        try {
            final File pathFile = new File(path);
            if (pathFile.getParentFile().exists()) {

                FileWriter writer = null;

                //Copy resource template
                try {
                    InputStream zipStream = SigmaExporter.class.getResourceAsStream("resources/network.zip"); //uk/ac/ox/oii/sigmaexporter/resources/network/index.html

                    //Path zipPath = Paths.get(path.getAbsolutePath()+"/network.zip");
                    //Files.copy(zipStream,zipPath);//NIO / JDK 7 Only

                    ZipHandler.extractZip(zipStream, pathFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }


                //Gson to handle JSON writing and escape
                Gson gson = new Gson();
                    
                //Write config.json
                try {
                    writer = new FileWriter(pathFile.getAbsolutePath() + "/network/config.json");
                    gson.toJson(config, writer);
                } catch (Exception e) {
                    e.printStackTrace();
                    new RuntimeException(e);
                } finally {
                    if (writer != null) {
                        writer.close();
                        writer = null;
                    }
                }


                //Write data.json
                try {
                    GraphModel graphModel = workspace.getLookup().lookup(GraphModel.class);
                    Graph graph = null;
                    graph = graphModel.getGraphVisible();

                    //Count the number of tasks (nodes + edges) and start the progress
                    int tasks = graph.getNodeCount() + graph.getEdgeCount();
                    Progress.start(progress, tasks);

                    HashSet<GraphElement> jNodes = new HashSet<GraphElement>();
                    Node[] nodeArray = graph.getNodes().toArray();
                    for (int i = 0; i < nodeArray.length; i++) {

                        Node n = nodeArray[i];
                        NodeData nd = n.getNodeData();
                        String id = nd.getId();
                        String label = nd.getLabel();
                        float x = nd.x();
                        float y = nd.y();
                        float size = nd.getSize();
                        String color = "rgb(" + (int) (nd.r() * 255) + "," + (int) (nd.g() * 255) + "," + (int) (nd.b() * 255) + ")";

                        GraphNode jNode = new GraphNode(id);
                        jNode.setLabel(label);
                        jNode.setX(x);
                        jNode.setY(y);
                        jNode.setSize(size);
                        jNode.setColor(color);

                        AttributeRow nAttr = (AttributeRow) nd.getAttributes();
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
                            jNode.putAttribute(name, val);

                        }

                        jNodes.add(jNode);

                        if (cancel) {
                            return false;
                        }
                        Progress.progress(progress);
                    }


                    //Export edges. Progress is incremented at each step.
                    HashSet<GraphElement> jEdges = new HashSet<GraphElement>();
                    Edge[] edgeArray = graph.getEdges().toArray();
                    for (int i = 0; i < edgeArray.length; i++) {
                        Edge e = edgeArray[i];
                        String sourceId = e.getSource().getNodeData().getId();
                        String targetId = e.getTarget().getNodeData().getId();
                        String weight = String.valueOf(e.getWeight());

                        GraphEdge jEdge = new GraphEdge(String.valueOf(e.getId()));
                        jEdge.setSource(sourceId);
                        jEdge.setTarget(targetId);
                        jEdge.putAttribute("weight", weight);

                        //TODO: Attributes of edge, including blended color!

                        jEdges.add(jEdge);

                        if (cancel) {
                            return false;
                        }
                        Progress.progress(progress);
                    }


                    writer = new FileWriter(pathFile.getAbsolutePath() + "/network/data.json");
                    HashMap<String, HashSet<GraphElement>> json = new HashMap<String, HashSet<GraphElement>>();
                    json.put("nodes", jNodes);
                    json.put("edges", jEdges);
                    
                    gson.toJson(json, writer);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    new RuntimeException(e);
                } finally {
                    if (writer != null) {
                        writer.close();
                        writer = null;
                    }
                }


                //Finish progress
                Progress.finish(progress);
                return true;
            } else {
                throw new Exception("Invalid or null settings.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public ConfigFile getConfigFile() {
        return config;
    }

    public List<String> getNodeAttributes() {
        List<String> attr = new ArrayList<String>();

        //GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        //GraphModel graphModel = graphController.getModel(workspace);
        //Graph graph = graphModel.getGraphVisible();
        GraphModel graphModel = workspace.getLookup().lookup(GraphModel.class);
        Graph graph = graphModel.getGraphVisible();
        AttributeRow ar = (AttributeRow) (graph.getNodes().toArray()[0].getNodeData().getAttributes());
        for (AttributeValue av : ar.getValues()) {
            attr.add(av.getColumn().getTitle());
        }
        return attr;
    }

    public void setConfigFile(ConfigFile cfg, String path) {
        this.config = cfg;
        this.path = path;
    }

    @Override
    public void setWorkspace(Workspace wrkspc) {
        this.workspace = wrkspc;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
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
