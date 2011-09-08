package org.opentripplanner.api.ws;

import java.awt.geom.Point2D;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import net.sf.json.JSONException;

import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.GeoJSONBuilder;
import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

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
		
        GraphService graphService = pathServiceFactory.getPathService("").getGraphService();
        Graph graph = graphService.getGraph();
		
        Vertex origin = graph.nearestVertex(fromLat, fromLon);
        long time = System.currentTimeMillis()/1000;
        
        TraverseOptions options = new TraverseOptions();
        
        if (graphService.getCalendarService() != null)
            options.setCalendarService(graphService.getCalendarService());
        options.setServiceDays(time);
        options.setTransferTable(graph.getTransferTable());
        options.setMaxWalkDistance(800);
        options.setOptimize(OptimizeType.QUICK);
        options.maxWeight = maxTime;
        
       
        GenericDijkstra dijkstra = new GenericDijkstra(graph, options);
        
        State initialState = new State(time, origin, options);
        
        BasicShortestPathTree spt = (BasicShortestPathTree) dijkstra.getShortestPathTree(initialState);        
        
        GeometryFactory gf = new GeometryFactory();
        
        Geometry mainGeom = null;
        Set<Coordinate> visitedCoords = new HashSet<Coordinate>();
        for(State state : spt.getAllStates()) {
            
            if(visitedCoords.contains(state.getVertex().getCoordinate())) continue;
            visitedCoords.add(state.getVertex().getCoordinate());
            /*Coordinate c = new Coordinate(state.getVertex().getX(), state.getVertex().getY());
            Point pt = gf.createPoint(c);
            Geometry toAdd = pt.buffer(.002);
            if(mainGeom == null) mainGeom = toAdd;
            else mainGeom = mainGeom.union(toAdd);*/
        }
        
        Coordinate coords[] = new Coordinate[visitedCoords.size()];
        int i=0;
        for(Coordinate c : visitedCoords) coords[i++] = c; //new Coordinate(v.getX(), v.getY());
        
        //gf.createMultiPoint(coords).buffer(.001);        
               

        StringWriter sw = new StringWriter();
        GeoJSONBuilder json = new GeoJSONBuilder(sw);
        try {
            json.writeGeom(gf.createMultiPoint(coords).buffer(.001));
        } catch (org.codehaus.jettison.json.JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }	
        return sw.toString();
    }            
}
