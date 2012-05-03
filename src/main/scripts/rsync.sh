#!/bin/bash
jar cf clearnlp-$1.jar edu
rsync -avc clearnlp-$1.jar choijd@verbs.colorado.edu:/data/choijd/opt/clearnlp/lib
