/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.analyst");

otp.analyst.IsochroneDemo = {

    map :               null,
    locationField :     null,
    timeSlider :        null,
    currentLocation :   null,
    reachableLayer :    null,

    initialize : function(config) {
        
        var thisMain = this;
           
        this.map = new OpenLayers.Map();
        var arrayOSM = ["http://otile1.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile2.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile3.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                        "http://otile4.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png"];
        var arrayAerial = ["http://oatile1.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile2.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile3.mqcdn.com/naip/${z}/${x}/${y}.png",
                           "http://oatile4.mqcdn.com/naip/${z}/${x}/${y}.png"];
            
        var baseOSM = new OpenLayers.Layer.OSM("MapQuest-OSM Tiles", arrayOSM);
        var baseAerial = new OpenLayers.Layer.OSM("MapQuest Open Aerial Tiles", arrayAerial);
       
        this.map.addLayer(baseOSM);
        this.map.addLayer(baseAerial);
        this.map.addControl(new OpenLayers.Control.LayerSwitcher());
        
        var initLocationProj = new OpenLayers.LonLat(-122.681944, 45.52).transform(
                new OpenLayers.Projection("EPSG:4326"), this.map.getProjectionObject());
            
        var markers = new OpenLayers.Layer.Vector(
            "Markers",
            {
                styleMap: new OpenLayers.StyleMap({
                    // Set the external graphic and background graphic images.
                    externalGraphic: "js/lib/openlayers/img/marker.png",
                    graphicWidth: 21,
                    graphicHeight: 25,
                    graphicXOffset: -10.5,
                    graphicYOffset: -25
                }),
            }
        );
        
        
        var marker = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(initLocationProj.lon, initLocationProj.lat)
        );
            
        var features = [];
        features.push(marker);
        markers.addFeatures(features);
        this.map.addLayer(markers);

        var dragFeature = new OpenLayers.Control.DragFeature(markers);
        dragFeature.onComplete = function(evt) {
            thisMain.currentLocation = new OpenLayers.LonLat(marker.geometry.x, marker.geometry.y).transform(
                thisMain.map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));       
            thisMain.locationUpdated();
        };

        this.map.addControl(dragFeature);
        dragFeature.activate();
        
        
        // set up the controls panel
        
        this.locationField = new Ext.form.TextField({
            fieldLabel: 'Current Location',
            anchor: '100%',
            readOnly: true
        });
        
        
        this.timeSlider = new Ext.slider.SingleSlider({
            fieldLabel: 'Max Time (min.)',
            value: 30,
            minValue: 0,
            maxValue: 120,
            plugins: new GeoExt.SliderTip()
        });

        var runButton = new Ext.Button({
            text: "Run",
            width: 100,
            handler: function(btn, evt) {
                //console.log("button");
                thisMain.isoQuery();
            }   
        });
        
                    
        var controlsPanel = new Ext.Panel({
            layout: 'form',
            title: 'Controls',
            padding: 10,
            width: 300,
            region: 'west',
            items: [ this.locationField, this.timeSlider, runButton ]
        });
        
        // set up the map panel
        
        var mapPanel = new GeoExt.MapPanel({
            map: this.map,
            title: 'Map',
            region: 'center'
        });
        
        
        // create the viewport

        new Ext.Viewport({
            layout: 'border',
            items: [ controlsPanel, mapPanel]
        });
                
                
        this.map.setCenter(initLocationProj, 10);
        this.currentLocation = initLocationProj.transform(
            this.map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326")); 
        this.locationUpdated();
    },

    isoQuery : function() {
    
        var thisMain = this;
        
        //console.log(currentLocation.lon + ", " + currentLocation.lat + ", " + timeSlider.getValue());
        var url = "/opentripplanner-api-webapp/ws/iso?fromLat=" + 
                   this.currentLocation.lat + "&fromLon=" + this.currentLocation.lon + "&maxTime=" + this.timeSlider.getValue();
        console.log(url);
        
        Ext.Ajax.request({
            url : '/opentripplanner-api-webapp/ws/iso',
            method: 'GET',
            params : {
                'fromLat' : this.currentLocation.lat,
                'fromLon' : this.currentLocation.lon,
                'maxTime' : this.timeSlider.getValue()*60
            },
            success: function ( result, request ) {
                //alert("success " + result.responseText);
                var geojson = new OpenLayers.Format.GeoJSON('Geometry');
                console.log('init geojson');
                var multiPt = geojson.read(result.responseText)[0];
                
                //var feat = new OpenLayers.Feature.Vector(
                //    new OpenLayers.Geometry.Point(initLocationProj.lon, initLocationProj.lat)
                //);
                //console.log("geom="+multiPt.geometry);
                var geom2 = multiPt.geometry.transform(
                    new OpenLayers.Projection("EPSG:4326"), thisMain.map.getProjectionObject());
                console.log("geom="+multiPt.geometry);
                
                if(thisMain.reachableLayer != null) thisMain.map.removeLayer(thisMain.reachableLayer);
                thisMain.reachableLayer = new OpenLayers.Layer.Vector("Reachable Set");
                var features = [ new OpenLayers.Feature.Vector(geom2) ];
                //features.push(marker);
                thisMain.reachableLayer.addFeatures(features);
                thisMain.map.addLayer(thisMain.reachableLayer);
                

            },
            failure: function ( result, request ) {
                alert("fail " + result.responseText);
            }
        });
    },

    locationUpdated : function() {
        this.locationField.setValue(this.currentLocation.lat + "," + this.currentLocation.lon);
    },

    CLASS_NAME: "otp.analyst.IsochroneDemo"

};

otp.analyst.IsochroneDemo = new otp.Class(otp.analyst.IsochroneDemo);
