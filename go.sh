#!/bin/bash
cd src
sudo javac -cp .:../jetty-all.jar *.java
sudo java -cp .:../jetty-all.jar LANPaint $1 $2 $3
cd ..
