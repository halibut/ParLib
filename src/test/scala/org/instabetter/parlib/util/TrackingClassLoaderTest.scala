package org.instabetter.parlib.util

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import scala.collection.mutable.ListBuffer
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TrackingClassLoaderTest extends FunSuite with ShouldMatchers {

    test("Class is tracked by class loader") { 
    	val classLoader = new TrackingClassLoader(List("java.*","scala.*"))
    	val clazz = classLoader.loadClass("org.instabetter.parlib.util.ClassLoaderTestClass2")
        
    	val trackedClasses = classLoader.getTrackedClassNames
    	
    	trackedClasses.contains("org.instabetter.parlib.util.ClassLoaderTestClass") should equal(true)
    	trackedClasses.contains("org.instabetter.parlib.util.ClassLoaderTestClass2") should equal {true}
    	trackedClasses.contains("java.lang.String") should equal {false}
    	trackedClasses.contains("scala.collection.Map") should equal {false}
    }

}

class ClassLoaderTestClass(){
    val string:java.lang.String = "Test"
}
class ClassLoaderTestClass2() extends ClassLoaderTestClass(){
    val map:scala.collection.Map[String,String] = Map()
}