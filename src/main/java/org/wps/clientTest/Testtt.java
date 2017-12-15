package org.wps.clientTest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.wps.WPSProject;
import org.wps.utils.GeoJsonFileUtils;

public class Testtt {

	public Testtt() {
		try {
			FeatureCollection<SimpleFeatureType, SimpleFeature> ref = GeoJsonFileUtils.geoJsonToFeatureCollection(new File("data_plouzane.json"));
			
			
			
			FeatureCollection<SimpleFeatureType, SimpleFeature> referenceLine =  WPSProject.drawRadial(ref, 100, 100, 1);
			GeoJsonFileUtils.featureCollectionToGeoJsonFile(referenceLine, "C:\\Users\\lecteur\\Desktop\\mydocs\\workspace\\WPSProject");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new Test();
	}

}
