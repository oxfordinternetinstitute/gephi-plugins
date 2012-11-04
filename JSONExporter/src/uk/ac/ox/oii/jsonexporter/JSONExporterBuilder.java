/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ox.oii.jsonexporter;

import org.gephi.io.exporter.api.FileType;
import org.gephi.io.exporter.spi.GraphExporter;
import org.gephi.io.exporter.spi.GraphFileExporterBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author shale
 */
@ServiceProvider(service = GraphFileExporterBuilder.class)
public class JSONExporterBuilder implements GraphFileExporterBuilder {

    public String getName() {
        return "json";
    }

    public FileType[] getFileTypes() {
        return new FileType[]{new FileType(".json", "JSON Graph")};
    }

    @Override
    public GraphExporter buildExporter() {
        return new JSONExporter();
    }
}
