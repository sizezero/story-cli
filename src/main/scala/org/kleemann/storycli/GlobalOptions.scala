package org.kleemann.storycli

import os.{Path}

case class GlobalOptions(isProduction: Boolean, command: String) {
    // seems dumb but correct to hardcode these
    val userName = "robert"
    val serverName = "olivia"
}
