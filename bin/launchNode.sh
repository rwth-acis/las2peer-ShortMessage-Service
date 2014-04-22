#!/bin/bash

# this script launches a LAS2peer node providing the ShortMessageService

cd ..

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -s 9010 - uploadStartupDirectory\(\'config/startup\'\) startService\(\'i5.las2peer.services.shortMessageService.ShortMessageService\',\'shortMessageServicePass\'\) startHttpConnector\(\'9080\'\) interactive
