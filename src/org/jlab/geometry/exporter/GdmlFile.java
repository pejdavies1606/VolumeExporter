package org.jlab.geometry.exporter;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.jlab.detector.units.Measurement;
import org.jlab.detector.units.SystemOfUnits.Length;
import org.jlab.detector.volume.Geant4Basic;

import eu.mihosoft.vrl.v3d.Vector3d;

/**
 * <h1> Exporter Utility </h1>
 * 
 * Writes a file in GDML format for a given Geant4Basic volume structure.
 * 
 * @author pdavies
 * @version 1.0.7
 */

public class GdmlFile implements GdmlExporter
{
	private DocumentBuilderFactory mDocFactory;
	private DocumentBuilder mDocBuilder;
	private Document mDoc;
	private Element mRoot, mDefine, mMaterials, mSolids, mStructure, mSetup;
	
	private boolean mVerbose = false;
	
	private String mPositionLoc = "local", mRotationLoc = "local";
	private String mDefaultMatRef = "mat_vacuum";
	private String mDesiredAngleUnit = "deg";
	private String mActualAngleUnit = "rad";
	
	
	public GdmlFile() throws ParserConfigurationException
	{
		mDocFactory = DocumentBuilderFactory.newInstance();
		mDocBuilder = mDocFactory.newDocumentBuilder();
		mDoc = mDocBuilder.newDocument();
		
		// / root
		mRoot = mDoc.createElement("gdml");
		mRoot.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		mRoot.setAttribute("xsi:noNameSpaceSchemaLocation", "http://cern.ch/service-spi/app/releases/GDML/Schema/gdml.xsd");
		mDoc.appendChild( mRoot );
		
		// /define List
		mDefine = mDoc.createElement("define");
		mRoot.appendChild( mDefine );
		
		// /materials List
		mMaterials = mDoc.createElement("materials");
		mRoot.appendChild( mMaterials );
		
		// /solids List
		mSolids = mDoc.createElement("solids");
		mRoot.appendChild( mSolids );
		
		// /structures List
		mStructure = mDoc.createElement("structure");
		mRoot.appendChild( mStructure );
		
		// /setup Setup
		mSetup = mDoc.createElement("setup");
		mSetup.setAttribute("name", "default");
		mSetup.setAttribute("version", "1.0");
		mRoot.appendChild( mSetup );
	}
	
	
	
	public void setVerbose( boolean aBool )
	{ // from VolumeExporter interface
		mVerbose = aBool;
	}
	
	
	
	public void setPositionLoc( String aLoc )
	{
		mPositionLoc = aLoc;
	}
	
	
	
	public void setRotationLoc( String aLoc )
	{
		mRotationLoc = aLoc;
	}
	
	
	
	public void setDesiredAngleUnit( String aAngleUnit ) throws IllegalArgumentException
	{
		switch( aAngleUnit )
		{
		case "deg":
		case "rad":
			mDesiredAngleUnit = aAngleUnit;
			break;
		default:
			throw new IllegalArgumentException("unknown unit: "+aAngleUnit );
		}
	}
	
	
	
	public void setActualAngleUnit( String aAngleUnit ) throws IllegalArgumentException
	{
		switch( aAngleUnit )
		{
		case "deg":
		case "rad":
			mActualAngleUnit = aAngleUnit;
			break;
		default:
			throw new IllegalArgumentException("unknown unit: "+aAngleUnit );
		}
	}
	
	
	
	public void setDefaultMaterial( String aMatRef )
	{
		this.mDefaultMatRef = aMatRef;
	}
	
	
	
	public void addTopVolume( Geant4Basic aTopVol )
	{ // from VolumeExporter interface
		
		if(mVerbose) System.out.println("adding top volume with the following parameters:");
		if(mVerbose) System.out.println("  position location=\t"+ mPositionLoc );
		if(mVerbose) System.out.println("  rotation location=\t"+ mRotationLoc );
		if(mVerbose) System.out.println("  material=\t\t"+ mDefaultMatRef );
		if(mVerbose) System.out.println("  actual angle unit=\t"+ mActualAngleUnit );
		if(mVerbose) System.out.println("  desired angle unit=\t"+ mDesiredAngleUnit );
		
		this.addMaterialPreset( mDefaultMatRef );
		this.addLogicalTree( aTopVol, mDefaultMatRef );
		this.addPhysicalTree( aTopVol );
		this.addWorld( aTopVol.getName() );
	}
	
	
	
	public void writeFile( String aFilename )
	{
		try {
			this.write( aFilename );
		} catch (IllegalArgumentException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void addPosition( String aName, Vector3d aPosition, String aUnits )
	{
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String aName");
		if( aPosition == null )
			throw new IllegalArgumentException("empty double[]");
		if( aUnits.isEmpty() )
			throw new IllegalArgumentException("empty String aUnits");
		
		Element position = mDoc.createElement("position");
		position.setAttribute("name", aName );
		position.setAttribute("x", Double.toString( aPosition.x ) );
		position.setAttribute("y", Double.toString( aPosition.y ) );
		position.setAttribute("z", Double.toString( aPosition.z ) );
		position.setAttribute("unit", aUnits );
		mDefine.appendChild( position );
		
		if(mVerbose) { System.out.println("added position \""+ aName +"\""); }
	}
	
	
	
	public void addRotation( String aName, double[] aRotation, String aOrder, String aUnits )
	{
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String aName");
		if( aRotation == null )
			throw new IllegalArgumentException("empty double[]");
		if( aOrder.isEmpty() )
			throw new IllegalArgumentException("empty String aOrder");
		if( aUnits.isEmpty() )
			throw new IllegalArgumentException("empty String aUnits");
		
		
		Element rotation = mDoc.createElement("rotation");
		rotation.setAttribute("name", aName );
		switch( aOrder )
		{
		case "xyz":
			rotation.setAttribute("x", Double.toString( aRotation[0] ) );
			rotation.setAttribute("y", Double.toString( aRotation[1] ) );
			rotation.setAttribute("z", Double.toString( aRotation[2] ) );
			break;
			
		case "yzx":
			rotation.setAttribute("y", Double.toString( aRotation[0] ) );
			rotation.setAttribute("z", Double.toString( aRotation[1] ) );
			rotation.setAttribute("x", Double.toString( aRotation[2] ) );
			break;
			
		case "zxy":
			rotation.setAttribute("z", Double.toString( aRotation[0] ) );
			rotation.setAttribute("x", Double.toString( aRotation[1] ) );
			rotation.setAttribute("y", Double.toString( aRotation[2] ) );
			break;
			
		case "zyx":
			rotation.setAttribute("z", Double.toString( aRotation[0] ) );
			rotation.setAttribute("y", Double.toString( aRotation[1] ) );
			rotation.setAttribute("x", Double.toString( aRotation[2] ) );
			break;
			
		case "yxz":
			rotation.setAttribute("y", Double.toString( aRotation[0] ) );
			rotation.setAttribute("x", Double.toString( aRotation[1] ) );
			rotation.setAttribute("z", Double.toString( aRotation[2] ) );
			break;
			
		default:
			throw new IllegalArgumentException("unknown order \""+ aOrder +"\"");
		}
		rotation.setAttribute("unit", aUnits );
		mDefine.appendChild( rotation );
		
		if(mVerbose) { System.out.println("added position \""+ aName +"\""); }
	}
	
	
	
	public void addMaterial( String aName, int aZ, double aDensity, String aDensityUnit, double aAtom, String aAtomUnit ) throws IllegalArgumentException 
	{		
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String aName");
		if( aZ < 1 )
			throw new IllegalArgumentException("zero/negative aZ");
		if( aDensity < 0.0)
			throw new IllegalArgumentException("negative density");
		if( aDensityUnit.isEmpty() )
			throw new IllegalArgumentException("empty String aDensityUnit");
		if( aAtom < 0.0 )
			throw new IllegalArgumentException("negative aAtom");
		if( aAtomUnit.isEmpty() )
			throw new IllegalArgumentException("empty String aAtomUnit");
		
		// /materials/material
		Element material = mDoc.createElement("material");
		material.setAttribute("name", aName );
		material.setAttribute("Z", Integer.toString( aZ ) );
		mMaterials.appendChild( material );
		
		// /materials/material/density
		Element density = mDoc.createElement("D");
		density.setAttribute("unit", aDensityUnit );
		density.setAttribute("value", Double.toString( aDensity ) );
		material.appendChild( density );

		// /materials/material/atom
		Element atom = mDoc.createElement("atom");
		atom.setAttribute("unit", aAtomUnit );
		atom.setAttribute("value", Double.toString( aAtom ) );
		material.appendChild( atom );
		
		if(mVerbose) { System.out.println("added material \""+ aName +"\""); }
	}
	
	
	
	public void addMaterialPreset( String aMatRef ) throws IllegalArgumentException
	{
		if( aMatRef.isEmpty() )
			throw new IllegalArgumentException("empty String aMatRef");
		
		switch( aMatRef )
		{		
		case "mat_vacuum":
			this.addMaterial( aMatRef, 1, 0.0, "g/cm3", 0.0, "g/mole");
			break;
			
		default:
			throw new IllegalArgumentException("material: \""+ aMatRef +"\"");
		}
	}
	
	
	
	public void addMaterialPreset( String aName, String aMatRef ) throws IllegalArgumentException
	{
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String aName");
		if( aMatRef.isEmpty() )
			throw new IllegalArgumentException("empty String aMatRef");
		
		switch( aMatRef )
		{		
		case "mat_vacuum":
			this.addMaterial( aName, 1, 0.0, "g/cm3", 0.0, "g/mole");
			break;
			
		default:
			throw new IllegalArgumentException("material \""+ aMatRef +"\" does not exist");
		}
	}
	
	
	
	public void addSolid( Geant4Basic aSolid ) throws IllegalArgumentException, NullPointerException
	{
		if( aSolid == null )
			throw new IllegalArgumentException("empty Geant4Basic"); // should this be NullPointerException?
		
		// /solids/solid
		String type = aSolid.getType().toLowerCase();
		Element solid = mDoc.createElement( type );
		String solRef = "sol_"+ aSolid.getName();
		solid.setAttribute("name", solRef );
		
		// types defined here: http://gdml.web.cern.ch/GDML/doc/GDMLmanual.pdf
		
		List<Measurement> solParams = aSolid.getDimensions();
		
		switch( type )
		{
		case "box":
			
			if( solParams.size() == 1 ) // cube
			{
				solid.setAttribute("x", Double.toString( solParams.get(0).value ) );
				solid.setAttribute("y", Double.toString( solParams.get(0).value ) );
				solid.setAttribute("z", Double.toString( solParams.get(0).value ) );
			}
			else if( solParams.size() == 3 )// regular cuboid
			{
				solid.setAttribute("x", Double.toString( solParams.get(0).value ) );
				solid.setAttribute("y", Double.toString( solParams.get(1).value ) );
				solid.setAttribute("z", Double.toString( solParams.get(2).value ) );
			}
			else
				throw new NullPointerException("incorrect number of parameters for type: \""+ type +"\"");
			break;
			
		case "eltube": // cylinder along Z axis
			
			if( solParams.size() == 3 )
			{
				solid.setAttribute("dx", Double.toString( solParams.get(0).value ) );
				solid.setAttribute("dy", Double.toString( solParams.get(1).value ) );
				solid.setAttribute("dz", Double.toString( solParams.get(2).value ) );
			}
			else
				throw new NullPointerException("incorrect number of parameters for type: \""+ type +"\"");
			break;
			
		case "orb": // sphere
			
			if( solParams.size() == 1 )
				solid.setAttribute("r", Double.toString( solParams.get(0).value ));
			else
				throw new NullPointerException("incorrect number of parameters for type: \""+ type +"\"");
			break;
			
		case "tube": // hollow tube segment
			
			if( solParams.size() == 5 )
			{
				solid.setAttribute("rmin", 	   Double.toString( solParams.get(0).value ) );
				solid.setAttribute("rmax", 	   Double.toString( solParams.get(1).value ) );
				solid.setAttribute("z",        Double.toString( solParams.get(2).value ) );
				solid.setAttribute("startphi", Double.toString( solParams.get(3).value ) );
				solid.setAttribute("deltaphi", Double.toString( solParams.get(4).value ) );
				solid.setAttribute("aunit", mDesiredAngleUnit );
			}
			else
				throw new NullPointerException("incorrect number of parameters for type: \""+ type +"\"");
			break;
			
		/*case "polyhedra": // pgon
			
			if( solParams.size() == 7 )
			{
				solid.setAttribute("", 	   Double.toString( solParams.get(0).value ) );
			}
			break;*/
			
		default:
			throw new IllegalArgumentException("type: \""+ type +"\"");
		}
		
		solid.setAttribute("lunit", Length.unit() );
		mSolids.appendChild( solid );
		
		if(mVerbose) { System.out.println("added solid \""+ solRef +"\""); }
	}
	
	
	
	public void addLogicalVolume( String aMaterialRef, Geant4Basic aSolid ) throws NullPointerException, IllegalArgumentException
	{		
		if( aMaterialRef.isEmpty() )
			throw new IllegalArgumentException("empty String");
		if( aSolid == null )
			throw new IllegalArgumentException("empty Geant4Basic");
		
		// logical volumes combine a solid with a material, but are not rendered
		
		String solName = aSolid.getName();
		
		// check that solid exists
		String solRef = "sol_"+ solName;
		Element sol = _findChildByName( mSolids, solRef );
		if( sol == null )
			throw new NullPointerException("could not find solid \""+ solRef +"\"");
		
		// /structures/volume Logical Volume
		Element logVol = mDoc.createElement( "volume" );
		logVol.setAttribute("name", "vol_"+ solName );
		mStructure.appendChild( logVol );

		// /structures/volume/materialref Reference to Material
		Element materialref = mDoc.createElement("materialref");
		materialref.setAttribute("ref", aMaterialRef );
		logVol.appendChild( materialref );

		// /structures/volume/solidref Reference to Solid
		Element solidref = mDoc.createElement("solidref");
		solidref.setAttribute("ref", solRef );
		logVol.appendChild( solidref );
		
		if(mVerbose) { System.out.println("added logical volume \""+ "vol_"+ solName +"\""); }
	}
	
	
	
	public void addLogicalVolume( Geant4Basic aSolid ) throws NullPointerException, IllegalArgumentException
	{		
		if( aSolid == null )
			throw new IllegalArgumentException("empty Geant4Basic");
		
		// logical volumes combine a solid with a material, but are not rendered
		
		String solName = aSolid.getName();
		
		// check that solid exists
		String solRef = "sol_"+ solName;
		Element sol = _findChildByName( mSolids, solRef );
		if( sol == null )
		{
			throw new NullPointerException("could not find solid \""+ solRef +"\"");
		}
		
		// /structures/volume Logical Volume
		Element logVol = mDoc.createElement( "volume" );
		logVol.setAttribute("name", "vol_"+ solName );
		mStructure.appendChild( logVol );

		// /structures/volume/solidref Reference to Solid
		Element solidref = mDoc.createElement("solidref");
		solidref.setAttribute("ref", solRef );
		logVol.appendChild( solidref );
		
		if(mVerbose) { System.out.println("added logical volume \""+ "vol_"+ solName +"\""); }
	}
	
	
	
	public void addLogicalTree( Geant4Basic aNode, String aMatRef ) // recursively iterate over all children to add logical volumes in the correct order (children first)
	{ // global material aMatRef
		List<Geant4Basic> children = aNode.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			Geant4Basic child = children.get( i );
			this.addLogicalTree( child, aMatRef ); // recursive
		}
		this.addSolid( aNode );
		this.addLogicalVolume( aMatRef, aNode );
	}

	
	
	public void addLogicalTree( Geant4Basic aNode ) // recursively iterate over all children to add logical volumes in the correct order (children first)
	{ // global material aMatRef		
		List<Geant4Basic> children = aNode.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			Geant4Basic child = children.get( i );
			this.addLogicalTree( child ); // recursive
		}
		this.addSolid( aNode );
		this.addLogicalVolume( aNode );
	}
		
	
	
	public void addPhysicalVolume( String aParentName, Geant4Basic aSolid ) throws NullPointerException, IllegalArgumentException
	{
		// Physical Volumes always have a position and a rotation in space, but this can be defined from a global or local reference 
		if( aParentName.isEmpty() )
			throw new IllegalArgumentException("empty String aParentName");
		if( aSolid == null)
			throw new IllegalArgumentException("empty Geant4Basic");
		
		// physical volumes are rendered, and need to be added as a child to a logical volume
		// physvols can be given a direct position, or a positionref that uses a global position in the define block, same with rotations

		// /structures/volume/physvol Physical Volume
		Element physvol = mDoc.createElement("physvol");
		// need to add physvol to a logvol given by parent ref
		String parentLogVolRef = "vol_"+ aParentName;
		// check that parent logical volume exists
		Element parentLogVol = _findChildByName( mStructure, parentLogVolRef );
		if( parentLogVol == null ) {
			throw new NullPointerException("could not find logical volume \""+ parentLogVolRef +"\"");
		}
		parentLogVol.appendChild( physvol );

		// /structures/volume/physvol/volumeref Reference to Volume
		Element volumeref = mDoc.createElement("volumeref");
		String selfLogVolRef = "vol_"+ aSolid.getName();
		// check that self logical volume exists
		Element selfLogVol = _findChildByName( mStructure, selfLogVolRef );
		if( selfLogVol == null )
		{
			throw new NullPointerException("could not find logical volume \""+ selfLogVolRef +"\"");
		}
		volumeref.setAttribute("ref", selfLogVolRef );
		physvol.appendChild( volumeref );
		
		// /structure/volume/physvol/position
		Vector3d pos = aSolid.getLocalPosition();
		
		// no need to write a position tag if nothing moves
		boolean posAllZero = ( pos.x == 0.0 && pos.y == 0.0 && pos.z == 0.0 );
		
		if( !posAllZero )
		{
			switch( mPositionLoc )
			{
			case "local":
				Element position = mDoc.createElement( "position" );
				//position.setAttribute( "name", "pos_"+ aSolid.getName() +"_in_"+ aParentName );
				position.setAttribute( "name", "pos_"+ aSolid.getName() );
				position.setAttribute("x", Double.toString( pos.x ) );
				position.setAttribute("y", Double.toString( pos.y ) );
				position.setAttribute("z", Double.toString( pos.z ) );
				position.setAttribute("unit", Length.unit() );
				physvol.appendChild( position );
				break;
				
			case "global":
				Element positionRef = mDoc.createElement( "positionref" );
				String positionName = "pos_"+ aSolid.getName() +"_in_"+ aParentName;
				this.addPosition( positionName, aSolid.getLocalPosition(), Length.unit() );
				positionRef.setAttribute("ref", positionName );
				physvol.appendChild( positionRef );
				break;
				
			default:
				throw new IllegalArgumentException("positionLoc: \""+ mPositionLoc +"\"");
			}
		}
		
		// /structure/volume/physvol/rotation
		double[] rot = aSolid.getLocalRotation();
		boolean rotAllZero = true;
		
		for( int i = 0; i < 3; i++) {
			if( rot[i] != 0.0 ) {
				rotAllZero = false;
				break;
			}
		}
		
		if( !rotAllZero ) // no need to write a blank line that doesn't do anything
		{
			double[] solRotation = aSolid.getLocalRotation();
			if( mDesiredAngleUnit == "deg" && mActualAngleUnit == "rad" )
			{
				for( int i = 0; i < 3; i++) { solRotation[i] = Math.toDegrees( solRotation[i] ); }
			}
			else if( mDesiredAngleUnit == "rad" && mActualAngleUnit == "deg" )
			{
				for( int i = 0; i < 3; i++) { solRotation[i] = Math.toRadians( solRotation[i] ); }
			}
			
			switch( mRotationLoc )
			{
			case "local":
				Element rotation = mDoc.createElement( "rotation" );
				rotation.setAttribute( "name", "rot_"+ aSolid.getName() +"_in_"+ aParentName );
				rotation.setAttribute("x", Double.toString( solRotation[0] ) );
				rotation.setAttribute("y", Double.toString( solRotation[1] ) );
				rotation.setAttribute("z", Double.toString( solRotation[2] ) );
				rotation.setAttribute("unit", mDesiredAngleUnit );
				physvol.appendChild( rotation );
				break;
				
			case "global":
				Element rotationRef = mDoc.createElement( "rotationref" );
				String rotationName = "rot_"+ aSolid.getName() +"_in_"+ aParentName;
				this.addRotation( rotationName, solRotation, aSolid.getLocalRotationOrder(), mDesiredAngleUnit );
				rotationRef.setAttribute("ref", rotationName );
				physvol.appendChild( rotationRef );
				break;
				
			default:
				throw new IllegalArgumentException("rotationLoc: \""+ mRotationLoc +"\"");
			}
		}
		
		if(mVerbose) { System.out.println("added physical volume \""+ selfLogVolRef +"\" to logical volume \""+ parentLogVolRef +"\""); }
	}
	

	
	public void addPhysicalTree( Geant4Basic aNode ) 
	{				
		List<Geant4Basic> children = aNode.getChildren();
		for( int i = 0; i < children.size(); i++ )
		{
			Geant4Basic child = children.get( i );

			int[] id = child.getId();
			boolean addPhysVol = true;
			
			if( id.length != 0 )
			{
				//System.out.println("id="+id[0]);
				if( id[0] == 0 )
					addPhysVol = false;
			}
			
			if( addPhysVol )
			{
				try 
				{
					this.addPhysicalVolume( aNode.getName(), child ); // always "rad" for Geant4Basic
					
				} catch( NullPointerException e ) {
					e.printStackTrace();
				} catch( IllegalArgumentException e ) {
					e.printStackTrace();
				}
			}
			
			this.addPhysicalTree( child ); // tail recursive?
		}
	}
			
	
	
	public void addWorld( String aLogVolName ) throws IllegalArgumentException 
	{		
		if( aLogVolName.isEmpty() )
			throw new IllegalArgumentException("empty String");

		// /setup/world World
		Element world = mDoc.createElement("world");
		String logVolRef = "vol_"+ aLogVolName;
		// check that logical volume exists
		Element LogVol = _findChildByName( mStructure, logVolRef );
		if( LogVol == null ) {
			throw new NullPointerException("could not find logical volume \""+ logVolRef +"\"");
		}
		world.setAttribute("ref", logVolRef );
		mSetup.appendChild( world );
		
		if(mVerbose) { System.out.println("added world from logical volume \""+ logVolRef +"\""); }
	}
	
	
	
	public void write( String aName ) throws TransformerConfigurationException, TransformerException, IllegalArgumentException 
	{		
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String");
	
		String filename = aName +".gdml";
		
		// write contents to gdml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(mDoc);

		StreamResult result = new StreamResult( new File( filename ) );
		//StreamResult result = new StreamResult( System.out );

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(source, result);

		System.out.println("wrote file \""+ filename +"\"");
	}
	
	
	
	// special case: find logical volumes ("vol_") whose name contains aSearch, and change the material reference to aMatRef
	//"structure", "volume", "name", "vol_aVolName", "materialref", "ref", "mat_aMatRef"
	public void replaceAttribute( String aParentName,
			String aSearchNode, String aSearchAttribute, String aSearchValue,
			String aReplaceNode, String aReplaceAttribute, String aReplaceValue ) throws NullPointerException, IllegalArgumentException
	{
		if( aParentName.isEmpty() )
			throw new IllegalArgumentException("empty String aParentName");
		if( aSearchNode.isEmpty() )
			throw new IllegalArgumentException("empty String aSearchNode");
		if( aSearchAttribute.isEmpty() )
			throw new IllegalArgumentException("empty String aSearchAttribute");
		if( aSearchValue.isEmpty() )
			throw new IllegalArgumentException("empty String aSearchValue");
		if( aReplaceNode.isEmpty() )
			throw new IllegalArgumentException("empty String aReplaceNode");
		if( aReplaceAttribute.isEmpty() )
			throw new IllegalArgumentException("empty String aReplaceAttribute");
		if( aReplaceValue.isEmpty() )
			throw new IllegalArgumentException("empty String aReplaceValue");
		
		//if( mVerbose ) System.out.printf("\"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"\n", aParentName, aSearchNode, aSearchAttribute, aSearchValue, aReplaceNode, aReplaceAttribute, aReplaceValue );
		
		//if( aReplaceValue.contains( aSearchValue ) )
			//throw new IllegalArgumentException("aReplaceValue=\""+aReplaceValue+"\" must not contain aSearchValue=\""+aSearchValue+"\"");
		
		Element parent = null;
		switch( aParentName )
		{
		case "structure":
			parent = mStructure;
			break;
		default:
			throw new IllegalArgumentException("unknown parent: "+aParentName );
		}
		
		// using XPath
		NodeList matchVolNodeList = _findChildrenByNameContains( parent, "name", aSearchValue );
		
		if( matchVolNodeList.getLength() == 0 )
		{
			if( mVerbose ) System.out.println("replaceAttribute() no matches for \""+aSearchValue+"\"");
		}
		else
		{
			if( mVerbose ) System.out.println("replaceAttribute() matches for \""+ aSearchValue +"\": "+ matchVolNodeList.getLength() );
			
			for( int i = 0; i < matchVolNodeList.getLength(); i++ )
			{
				String volName = matchVolNodeList.item(i).getAttributes().getNamedItem( aSearchAttribute ).getNodeValue();
				//if( mVerbose ) System.out.printf("i%d \"%s\"\n", i, volName );
				
				NodeList volChildNodeList = matchVolNodeList.item(i).getChildNodes();
				//if( mVerbose ) System.out.printf("found %d children\n", volChildNodeList.getLength() );
				
				if( volChildNodeList.getLength() == 0 )
				{
					if( mVerbose ) System.out.println("ignored: "+ volName +": has no nodes" );
				}
				else
				{
					int matRefIndex = -1;
					for( int j = 0; j < volChildNodeList.getLength(); j++ )
					{
						matRefIndex = j;
						//if( mVerbose ) System.out.printf(" j%d \"%s\"\n", j, volChildNodeList.item(j).getNodeName() );
						if( volChildNodeList.item(j).getNodeName() == aReplaceNode ) break;
					}
					Node matRefNode = volChildNodeList.item( matRefIndex ).getAttributes().getNamedItem( aReplaceAttribute );
					
					if( matRefNode == null )
					{
						if( mVerbose ) System.out.println("skipped: "+ volName +": does not have node "+ aReplaceNode );
					}
					else
					{
						String oldMatRef = matRefNode.getNodeValue();
						matRefNode.setNodeValue( aReplaceValue );
						String newMatRef = matRefNode.getNodeValue();
						if( mVerbose ) System.out.println("changed: "+ volName +": "+ oldMatRef +" -> "+ newMatRef );
					}
				}
			}
		}
	}
	
	
	
	public void replaceVolumeMaterial( String aVolName, String aMatRef )
	{
		replaceAttribute( "structure", "volume", "name", aVolName, "materialref", "ref", aMatRef );
	}
	
	
	
	private Element _findChildByName( Element aParent, String aName ) throws IllegalArgumentException
	{
		if( aName.isEmpty() )
			throw new IllegalArgumentException("empty String");
		if( aParent == null )
			throw new IllegalArgumentException("empty Element");
		
		NodeList childNodes = aParent.getChildNodes();
		//System.out.println("GdmlFile: _findChildByName(): begin search in parent <"+ aParent.getNodeName() +"> for child with name=\""+ aName +"\">");
		
		for( int i = 0; i < childNodes.getLength(); i++)
		{
			Element child = (Element) childNodes.item( i );
			String childName = child.getAttribute("name");
			
			//Node childNode = childNodes.item( i );
			//String childNodeName = childNode.getAttributes().getNamedItem("name").getNodeValue();
			
			//System.out.println(" checking child <"+ child.getNodeName() +" name=\""+ childName +"\">");
			//System.out.println("childName="+ childName);
			//System.out.println("aName="+ aName);
			
			if( childName.equals(aName) ) // don't use childName == aName, which checks references (pointers) of the objects, and not their logical value!
			{
				//System.out.println(" found child");
				return child;
			}
		}
		//System.out.println(" no child found");
		return null;
	}
	
	
	
	private NodeList _findChildrenByNameContains(
			Element aParent,
			String aSearchNodeAttribute,
			String aSearchNodeAttributeSubName
			) throws IllegalArgumentException
	{		
		if( aParent == null )
			throw new IllegalArgumentException("empty Element");
		if( aSearchNodeAttribute.isEmpty() )
			throw new IllegalArgumentException("empty String aSearchNodeAttribute");
		if( aSearchNodeAttributeSubName.isEmpty() )
			throw new IllegalArgumentException("empty String aSearchNodeAttributeSubName");
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		try
		{
			String xPathSearch = "//*[contains(@"+aSearchNodeAttribute+",'"+aSearchNodeAttributeSubName+"')]"; // search for substring in current node tree (that includes all sub nodes!)
			//System.out.println("XPathSearch=\""+ xPathSearch +"\"");
			NodeList matchVolNodeList = (NodeList) xpath.evaluate( xPathSearch, aParent, XPathConstants.NODESET );
			//if( mVerbose ) System.out.println("XPath search in <"+aParent.getTagName()+"> for elements with attribute \""+aSearchNodeAttribute+"\" containing \""+aSearchNodeAttributeSubName+"\" matched "+matchVolNodeList.getLength()+" nodes");
			return matchVolNodeList;
		}
		catch( XPathExpressionException e )
		{
			e.printStackTrace();
		}
		
		return null;
	}
}
