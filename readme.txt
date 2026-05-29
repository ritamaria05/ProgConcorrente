Exercise 1

Directory Structure:
/exercise1
    1_1implementation.txt
    1_2implementation.txt
    1_3implementation.txt
    1_4implementation.txt
/exercise2
    /process-simulator
    ex2.1
    ex2.2
    ex2.3
    ex2.4
/src
    /main
        /scala
            /exercise3
                tickets1.scala
                tickets2.scala
report.pdf
readme.txt

---
Exercise 1:

The CCS models for exercise 1 were developed using PseuCo (https://pseuco.com)

How to execute:
1. Open PseuCo
2. Open the "Files" tab
3. Create a new CCS file
4. Copy the content of the desired .txt file (e.g. 1_1implementation.txt) and paste it into the editor
5. The corresponding Labeled Transition System (LTS) will be automatically generated on the left of the page

---
Exercise 2:

StressTest.scala - to test a burst of /run-simulation commands 
    (would be an example of a potentially failling program in ex2.1 and 2.2)

VolatileTest.scala - to test the volatile function in ex2.4

Main.scala, Routes.scala and ServerState.scala are the server implementation.


The execution of this code requires the following installed components:
- SBT : Version 1.0
- Scala : Version 2.13.12

To execute the files, follow the instructions below:
1. Open the terminal at the project's root directory
2. Go to the "exercise2/process-simulator/server" directory
2. Enter sbt shell by simply typing "sbt"
3. Type "compile" to compile the files
4. Type "run" to run one of the files
5. Select a file by typing the number associated with it ([1] or [2] or [3])


--

Exercise 3:

tickets1.scala: exercise 3.1 (basic actor model)
tickets2.scala: exercise 3.2 (actor model with child delegation)

The execution of this code requires the following installed components:
- SBT : Version 1.0
- Scala : Version 2.13.12
- Akka Actor : Version 2.8.5

To execute the files, follow the instructions below:
1. Open the terminal at the project's root directory ($ cd /path/to/project)
2. Enter sbt shell by simply typing "sbt" ($ sbt)
3. Type "compile" to compile the files (sbt > compile)
4. Type "run" to run one of the files (sbt > run)
5. Select a file by typing the number associated with it (TicketOfficeTest for exercise 3.1 and TicketOfficeChild for exercise 3.2)

The console will display the asynchronous logging of the actors.
