#!/bin/bash
cd src
javac -cp .:../jetty-all.jar *.java
sudo java -cp .:../jetty-all.jar:/usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt/jre/lib/ext/jfxrt.jar LANPaint $1 $2 $3
cd ..
