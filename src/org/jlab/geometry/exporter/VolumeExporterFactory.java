package org.jlab.geometry.exporter;

import javax.xml.parsers.ParserConfigurationException;

public class VolumeExporterFactory
{
	public static GdmlExporter createGdmlFactory() throws IllegalArgumentException
	{
		try {
			return new GdmlFile();
		}
		catch( ParserConfigurationException e )
		{
			e.printStackTrace();
		}
		return null;
	}
}
