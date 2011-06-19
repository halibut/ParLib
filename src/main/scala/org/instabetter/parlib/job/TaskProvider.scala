/*
 * Copyright (C) 2011 instaBetter Software <http://insta-better.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.instabetter.parlib
package job

import event._

/**
 * Manages a collection of tasks that need to be completed.
 * Implementations of this class are thread-safe, which is required
 * because task providers will be accessed from multiple actors.
 */
trait TaskProvider[T] extends CollectionEventProvider{

    /**
     * @param tasks the tasks to add to the provider
     */
    def addTasks(tasks:Iterable[T]);
    
    /**
     * @param task the task to add to the provider
     */
    def addTask(task:T);
    
    /**
     * @return the number of tasks remaining in the provider.
     */
    def numTasksRemaining():Int;
    
    /**
     * @return an Option[T] that will contain the next task in the provider
     * or None if the provider has no tasks available. 
     */
    def takeNextTask():Option[T];
    
    /**
     * Removes a task from the provider
     * @param task the task to remove
     */
    def removeTask(task:T);
    
}