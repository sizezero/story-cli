package org.kleemann.storycli

import os.Path

class StoriesFolder(go: GlobalOptions) {

    // these folders exist on both the client and server
    val top:         Path = os.home / "stories"
    val development: Path = top / "development"
    val checkouts:   Path = if (go.isProduction) top / "checkouts" else development / "checkouts"

    // these folders only exist on the server
    val serverRepos:    Path = if (go.isProduction) top / "repos" else development / "repos"
    val serverTemplate: Path = serverRepos / "template.git"
    val serverStories:  Path = serverRepos / "stories"

    val isServer: Boolean = os.exists(serverRepos)
}
