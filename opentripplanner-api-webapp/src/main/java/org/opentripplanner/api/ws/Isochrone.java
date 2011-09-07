package org.opentripplanner.api.ws;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import net.sf.json.JSONException;

import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.util.GeoJSONBuilder;
import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;

@Path("/iso")
@XmlRootElement
@Autowire
public class Isochrone {
    
    private PathServiceFactory pathServiceFactory;

    @Required
    public void setPathServiceFactory(PathServiceFactory pathServiceFactory) {
        this.pathServiceFactory = pathServiceFactory;
    }
    
    @GET
    @Produces( { MediaType.APPLICATION_JSON })
    public String getIsochrone(
            @QueryParam("fromLat") Float fromLat,
            @QueryParam("fromLon") Float fromLon,
            @QueryParam("maxTime") Double maxTime) {
		
        Graph graph = pathServiceFactory.getPathService("").getGraphService().getGraph();
		
        Vertex origin = graph.nearestVertex(fromLat, fromLon);
        TraverseOptions options = new TraverseOptions();
        Dijkstra dijkstra = new Dijkstra(graph, origin, options, null);
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(maxTime, Integer.MAX_VALUE);
        
        Coordinate coords[] = new Coordinate[spt.getVertexCount()];
        
        int i=0;
        for(State state : spt.getAllStates()) {
		    coords[i++] = new Coordinate(state.getVertex().getX(), state.getVertex().getY());
        }		

        GeometryFactory gf = new GeometryFactory();
        StringWriter sw = new StringWriter();
        GeoJSONBuilder json = new GeoJSONBuilder(sw);
        try {
            json.writeGeom(gf.createMultiPoint(coords));
        } catch (org.codehaus.jettison.json.JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }	
        return sw.toString();
    }            
}
