package org.instabetter.parlib.util

import org.slf4j.{Logger,LoggerFactory}

trait Logging {

    private val logger:Logger = LoggerFactory.getLogger(getClass)
    
    def trace(msg:String){ logger.trace(msg) }
    def trace(msg:String, p:Any){ logger.trace(msg, p) }
    def trace(msg:String, p1:Any, p2:Any){ logger.trace(msg, p1, p2) }
    def trace(msg:String, throwable:Throwable){ logger.trace(msg, throwable)}
    
    def debug(msg:String){ logger.debug(msg) }
    def debug(msg:String, p:Any){ logger.debug(msg, p) }
    def debug(msg:String, p1:Any, p2:Any){ logger.debug(msg, p1, p2) }
    def debug(msg:String, throwable:Throwable){ logger.debug(msg, throwable)}
    
    def info(msg:String){ logger.info(msg) }
    def info(msg:String, p:Any){ logger.info(msg, p) }
    def info(msg:String, p1:Any, p2:Any){ logger.info(msg, p1, p2) }
    def info(msg:String, throwable:Throwable){ logger.info(msg, throwable)}
    
    def warn(msg:String){ logger.warn(msg) }
    def warn(msg:String, p:Any){ logger.warn(msg, p) }
    def warn(msg:String, p1:Any, p2:Any){ logger.warn(msg, p1, p2) }
    def warn(msg:String, throwable:Throwable){ logger.warn(msg, throwable)}
    
    def error(msg:String){ logger.error(msg) }
    def error(msg:String, p:Any){ logger.error(msg, p) }
    def error(msg:String, p1:Any, p2:Any){ logger.error(msg, p1, p2) }
    def error(msg:String, throwable:Throwable){ logger.error(msg, throwable)}

}