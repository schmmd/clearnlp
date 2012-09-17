#!/bin/bash
jar cf clearnlp-$1.jar com
rsync -avc clearnlp-$1.jar jdchoi@blake.cs.umass.edu:/iesl/canvas/jdchoi/clearnlp/lib
