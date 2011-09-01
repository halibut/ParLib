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

package org.instabetter.parlib.job

/**
 * Trait for running per-Task code on the server
 * @tparam T type of Task to be run by the client
 * @tparam R type of Result computed by the client for each Task T
 */
trait ServerRunnable[T,R] {

    /**
     * Executes for each task after it is computed by the client
     * @param task the Task T
     * @param result the result of the computation performed by the client
     */
    def postComputeTask(task:T,result:R):Unit
    
    /**
     * Whether or not to perform the postComputeTask sequentially (defaults to true, but
     * can be overridden in implementing classes.
     */
    val synchronizePostComputeTask:Boolean = true
}


/**
 * No Op implementation of Server runnable code that does nothing
 * on each task.
 */
class NoOpServerRunnable[T,R] extends ServerRunnable[T,R]{
    override def postComputeTask(task:T,result:R):Unit = {}
    
    override val synchronizePostComputeTask:Boolean = false
}