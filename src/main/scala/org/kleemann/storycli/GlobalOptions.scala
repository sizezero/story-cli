package org.kleemann.storycli

import os.{Path}
/**
  * 
  *
  * @param args all command line arguments
  * @param isProduction true if the program is running in production mode. This is a combination of the program's installation location and the command line arguments.
  * @param command the command to be executed
  * @param rest command line arguments that occur after command; this is a sublist of args
  */
case class GlobalOptions(args: List[String], isProduction: Boolean, command: String, rest: List[String]) {
    // seems dumb but correct to hardcode these
    val userName = "robert"
    val serverName = "olivia"

    val productionOption: String = if (isProduction) "--production" else "--development"
}
