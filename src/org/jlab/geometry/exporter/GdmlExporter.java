package org.jlab.geometry.exporter;

public interface GdmlExporter extends VolumeExporter
{
	public void setPositionLoc( String aLoc );
	public void setRotationLoc( String aLoc );
	
	public void addMaterialPreset( String aName );
	public void addMaterialPreset( String aName, String aMatRef );
	
	public void replaceAttribute( String aParentName,
			String aSearchNode, String aSearchAttribute, String aSearchValue,
			String aReplaceNode, String aReplaceAttribute, String aReplaceValue );
}
