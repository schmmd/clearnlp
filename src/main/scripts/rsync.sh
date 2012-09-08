#!/bin/bash
jar cf clearnlp-$1.jar com
rsync -avc clearnlp-$1.jar jdchoi@blake.cs.umass.edu:/home/jdchoi/clearnlp/lib
