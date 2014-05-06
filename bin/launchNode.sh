#!/bin/bash

# this script starts a LAS2peer node providing the ShortMessageService
# pls run the script form the root folder of your deployment, e. g. ./bin/start_network.sh

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -s 9010 - uploadStartupDirectory\(\'config/startup\'\) startService\(\'i5.las2peer.services.shortMessageService.ShortMessageService\',\'shortMessageServicePass\'\) startHttpConnector interactive
