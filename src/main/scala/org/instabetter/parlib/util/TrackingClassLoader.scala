package org.instabetter.parlib
package util

import scala.collection.Set
import scala.collection.mutable.{HashMap}
import scala.io.Source

import java.io.{InputStream, ByteArrayOutputStream}

class TrackingClassLoader(val parent:ClassLoader, val ignoreFilters:Iterable[String])
        extends ClassLoader(parent) with Logging{
    
    def this(ignoreFilters:Iterable[String]){
        this(classOf[TrackingClassLoader].getClassLoader, ignoreFilters)
    }
    
    def this(){
        this(List())
    }
    
    private val _trackedClasses:HashMap[String,Class[_]] = HashMap()
    
    def getTrackedClassNames:Set[String] = {
        _trackedClasses.keySet
    }
    
    protected override def loadClass(className:String, resolve:Boolean):Class[_] = {
        this.synchronized{
	        trace("Loading class: {}",  className)
	
	        //Check to see if we've already loaded this class
	        //in our tracked list 
	        _trackedClasses.get(className).foreach{trackedClass =>
	            return trackedClass;
	        }
	        
	        //Check if it was already loaded
	        Option(findLoadedClass(className)).foreach{loadedClass =>
	            return loadedClass
	        }
	        
	        //Check to see if we should track this class or not
	        val track = ignoreFilters.forall(!className.matches(_))
	        loadAsNormalClass(className, track, resolve).foreach{newClass =>
	            return newClass
	        }
	        
	        //Try to load it as a system class
	        loadAsSystemClass(className).foreach{sysClass =>
	            return sysClass
	        }
	        
	        throw new ClassNotFoundException("Class "+className+" could not be loaded.") 
        }
    }
    
    private def loadClassBytesFromClasspath(className:String):Array[Byte] = {
        val classPathClassName = className.replace('.','/') + ".class"
        try {
	        val is = getResourceAsStream(classPathClassName)
            val os = new ByteArrayOutputStream();
            val buffer:Array[Byte] = new Array(is.available);
            is.read(buffer)
            os.write(buffer)
            return os.toByteArray();
        }
        catch {
            case err:Exception =>
                warn("Error loading class from classpath.", err)
                return null;
    	}
    }
    
    private def loadAsNormalClass(className:String, track:Boolean, resolve:Boolean):Option[Class[_]] = {
        try{
	        if(!track){
	            val ignoreClass = super.loadClass(className, resolve)
	            return Some(ignoreClass)
	        }
	        else{
	            val trackedClassBytes = loadClassBytesFromClasspath(className)
	            val trackedClass = defineClass(className, trackedClassBytes, 0, trackedClassBytes.length);
	            if (resolve) {
	                resolveClass(trackedClass);
	            }
	            _trackedClasses += (className -> trackedClass)
	            return Some(trackedClass)
	        }
        }
        catch{
            case _ => return None
        }
    }
    
    private def loadAsSystemClass(className:String):Option[Class[_]] = {
        try{
            val sysClass = findSystemClass(className)
            return Some(sysClass)
        }
        catch{
            case _ => return None
        }
    }
}
