#!/bin/bash
cd src
sudo javac -cp .:../jetty-all.jar *.java
sudo java -cp .:../jetty-all.jar LanPaint $1 $2 $3
cd ..
