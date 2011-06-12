package org.instabetter.parlib.job

trait TaskProvider[T] {

    def addTasks(tasks:Iterable[T]);
    def addTask(task:T);
    def numTasks():Int;
    def takeNextTask():Option[T];
    def removeTask(task:T);
}