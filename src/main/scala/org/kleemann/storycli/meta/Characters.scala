package org.kleemann.storycli.meta

case class Character(name: String, role: String)

object Characters {

    val filename = "characters.md"

    def extract(repo: os.Path): Either[String, List[Character]] =
        pipe(extractFile(repo,filename), list.Character.create)

    def read(dir: os.Path): Either[String, List[Character]] =
        pipe(readFile(dir / filename), list.Character.create)
}
