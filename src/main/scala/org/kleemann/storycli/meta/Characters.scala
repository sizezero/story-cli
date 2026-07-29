package org.kleemann.storycli.meta

case class Character(name: String, role: String)

object Characters {

    val filename = "characters.md"

    def extract(repo: os.Path): Either[String, List[Character]] =
        stream.Characters.extract(repo)

    def read(dir: os.Path): Either[String, List[Character]] =
        stream.Characters.read(dir)
}
