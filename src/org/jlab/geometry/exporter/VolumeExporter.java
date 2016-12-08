package org.jlab.geometry.exporter;

import org.jlab.detector.volume.Geant4Basic;

public interface VolumeExporter
{
	public void setVerbose( boolean aBool );
	
	public void addTopVolume( Geant4Basic aTopVol );
	
	public void writeFile( String aFileNameWithoutExtension );
}
