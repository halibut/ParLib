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
 * Code to be executed by a client for each Task
 * @tparam T the type of Task T; the data to perform the computation on
 * @tparam R the result of the computation
 */
trait ClientRunnable[-T,+R] {

    /**
     * The code to run on the client for each task.
     * @param task the data to perform the computation on
     * @return the result of the computation
     */
    def runOnClient(task:T):R;
    
    val clientClassName = this.getClass().getName()
}


final class RunOnClientFunc[-T,+R](val func:(T)=>R) extends ClientRunnable[T,R]{
    
    def runOnClient(task:T):R = {
        func(task)
    }
    
    override val clientClassName = func.getClass().getName()
}