package org.instabetter.parlib
package job

import util.ResourceQueue
import scala.collection.Iterable



class InMemoryTaskProvider[T] extends TaskProvider[T] {
    
    private lazy val _tasks:ResourceQueue[T] = { 
        val taskPool = new ResourceQueue[T]()
        taskPool
    }
    
    def addTasks(tasks:Iterable[T]) { tasks.foreach(_tasks.add(_)) }
    def addTask(task:T){ _tasks.add(task) }
    def numTasks():Int = { _tasks.getCount() }
    def takeNextTask():Option[T] = { _tasks.removeNext }
    def removeTask(task:T){ _tasks.remove(task) }

}