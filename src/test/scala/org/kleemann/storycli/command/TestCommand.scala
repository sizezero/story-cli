package org.kleemann.storycli.command

import org.kleemann.storycli.GlobalOptions

class TestCommand extends munit.FunSuite {
    test("scratch") {
        assertEquals(1, 1)
    }

    test("empty") {
        assertEquals( 
            Command.parse(Nil), 
            Left("no command specified") )
    }

    test("default option is development") {
        // all of our tests run in development mode
        assertEquals( 
            Command.parse(List("help")), 
            Right(GlobalOptions(List("help"), false, "help", Nil)) )
    }

    test("explicit development") {
        assertEquals( 
            Command.parse(List("--development", "help")), 
            Right(GlobalOptions(List("--development", "help"), false, "help", Nil)) )
    }

    test("production and development conflict") {
        Command.parse(List("--development", "--production", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }

        Command.parse(List("--production", "--development", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }
    }

    test("all versions of help") {
        assertEquals( Command.parse(List("-h")),
            Right(GlobalOptions(List("-h"), false, "help", Nil)) )
        assertEquals( Command.parse(List("--help")),
            Right(GlobalOptions(List("--help"), false, "help", Nil)) )
        assertEquals( Command.parse(List("help")),
            Right(GlobalOptions(List("help"), false, "help", Nil)) )
    }

    test("--production can't be specified from a non-production location") {
        Command.parse(List("--production", "help")) match {
            case Left(_) => assert(true) // don't care what the error message is
            case _       => assert(false)
        }
    }

    test("unrecognized argument") {
        assertEquals( 
            Command.parse(List("***")), 
            Left("unrecognized argument: ***") )
    }
}
