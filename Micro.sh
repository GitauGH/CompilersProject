#!/bin/bash
java -cp "/C/Users/Me/Desktop/Computer Science/Compilers/Step 4/Step 4/antlr-4.7.2-complete.jar" org.antlr.v4.Tool Little.g4
javac -cp "/C/Users/Me/Desktop/Computer Science/Compilers/Step 4/Step 4/antlr-4.7.2-complete.jar" -Xlint:deprecation -Xlint:unchecked *.java
java -cp ".;C:\Users\Me\Desktop\Computer Science\Compilers\Step 4\Step 4\antlr-4.7.2-complete.jar" Driver < input/test20.micro
