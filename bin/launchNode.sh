#!/bin/bash

# this script starts a LAS2peer node providing the ShortMessageService
# pls run the script from the root folder of your deployment, e. g. ./bin/launchNode.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9010 uploadStartupDirectory\(\'etc/startup\'\) startService\(\'i5.las2peer.services.shortMessageService.ShortMessageService@1.0\'\) startWebConnector interactive
