package org.opentripplanner.api.ws;

import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathServiceFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.util.DateUtils;
import org.opentripplanner.util.GeoJSONBuilder;
import org.springframework.beans.factory.annotation.Required;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;
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
            @QueryParam("startDate") String startDate,
            @QueryParam("startTime") String startTime,
            @QueryParam("maxTime") Double maxTime) {
		
        GraphService graphService = pathServiceFactory.getPathService("").getGraphService();
        Graph graph = graphService.getGraph();
		
        Vertex origin = graph.nearestVertex(fromLat, fromLon);
        
        //long time = DateUtils.secPastMid(DateUtils.toDate(startDate, startTime));
        //long time = System.currentTimeMillis()/1000;
        Date d = DateUtils.parseDate(startDate);
        long time = d.getTime()/1000 + DateUtils.secPastMid(startTime);
        
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
        
        /*Geometry mainGeom = null;
        Set<Coordinate> visitedCoords = new HashSet<Coordinate>();
        for(State state : spt.getAllStates()) {
            
            if(visitedCoords.contains(state.getVertex().getCoordinate())) continue;
            visitedCoords.add(state.getVertex().getCoordinate());
        }
        
        Coordinate coords[] = new Coordinate[visitedCoords.size()];
        int i=0;
        for(Coordinate c : visitedCoords) coords[i++] = c; //new Coordinate(v.getX(), v.getY());
        
        //gf.createMultiPoint(coords).buffer(.001);   */     
               
        CoordinateReferenceSystem WGS84, mapCRS;
        MathTransform tr = null;
        try {
            WGS84 = CRS.decode("EPSG:4326", true);
            mapCRS = CRS.decode("EPSG:3857", true);
            tr = CRS.findMathTransform(WGS84, mapCRS);
           
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Set<Coordinate> visitedCoords = new HashSet<Coordinate>();
        Set<String> strCheck = new HashSet<String>();
        //int i=0;
        double fd = 1000 * (maxTime+1)/7200.0; 
        int factor = (int) fd;
        
        for(State state : spt.getAllStates()) {
            
            
            Coordinate c = state.getVertex().getCoordinate();
            DirectPosition dp = new GeneralDirectPosition(c.x, c.y), mapCoord = null;
            try {
                mapCoord = tr.transform(dp, null);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            c = new Coordinate(mapCoord.getCoordinate()[0], mapCoord.getCoordinate()[1]);
            String str = Math.floor(c.x/factor)+","+Math.floor(c.y/factor);
            //String str = String.format("%.2f,%.2f", c.x, c.y);
            if(strCheck.contains(str)) continue;
            strCheck.add(str);
            //if(i++ % 100 ==0) System.out.println(str);
            visitedCoords.add(c);//state.getVertex().getCoordinate());

        }
        
        Coordinate coords[] = new Coordinate[visitedCoords.size()];
        
        int i=0;
        for(Coordinate c : visitedCoords) { 
            coords[i++] = c;
        }
        
        StringWriter sw = new StringWriter();
        GeoJSONBuilder json = new GeoJSONBuilder(sw);
        try {
            json.writeGeom(gf.createMultiPoint(coords).buffer(750));
        } catch (org.codehaus.jettison.json.JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }	
        return sw.toString();
    }            
}
