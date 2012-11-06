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



                //Write config.json
                try {
                    writer = new FileWriter(pathFile.getAbsolutePath() + "/network/config.json");
                    //StringBuffer sb = new StringBuffer();
                    //sb.append("{\"type\":\"network\",\"data\":\"data.json\",\"version\":\"1.0\"");

                    /*props.put("type", "network");
                    props.put("data", "data.json");
                    props.put("version", "1.0");
                    Object[] keys = props.keySet().toArray();
                    for (Object key : keys) {
                        String val = props.get(key);
                        /*if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) {
                         boolean b = Boolean.valueOf(val);
                         }*/
                        /*if (val != null && val.length() >= 4 && val.substring(0, 4).equals("None")) {
                            props.remove(key);
                        }
                    }*/

                    Gson gson = new Gson();
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
                    writer = new FileWriter(pathFile.getAbsolutePath() + "/network/data.json");
                    GraphModel graphModel = workspace.getLookup().lookup(GraphModel.class);
                    Graph graph = null;
                    graph = graphModel.getGraphVisible();

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
        this.path=path;
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
