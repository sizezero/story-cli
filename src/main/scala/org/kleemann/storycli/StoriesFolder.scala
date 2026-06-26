package org.kleemann.storycli

import os.Path

class StoriesFolder(go: GlobalOptions) {
    val top:         Path = os.home / "stories"
    val development: Path = top / "development"
    val checkouts:   Path = if (go.isProduction) top / "checkouts" else development / "checkouts"
}

case class ServerStoriesFolder(go: GlobalOptions) extends StoriesFolder(go) {
    val repos:    Path = if (go.isProduction) top / "repos" else development / "repos"
    val template: Path = repos / "template.git"
    val stories:  Path = repos / "stories"
}

object StoriesFolder {
    /**
      * Tests the existence of a local folder so this method has side-effects.
      *
      * @param go
      * @return Either the client or the server StoriesFolder
      */
    def create(go: GlobalOptions): Either[StoriesFolder, ServerStoriesFolder] = {
        // need to create server to test even if we don't end up returning it
        val server = ServerStoriesFolder(go)
        if (os.exists(server.repos)) Right(server)
        else Left(StoriesFolder(go))
    }
}