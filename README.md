las2peer-ShortMessage-Service
=============================

This is a middleware service for las2peer that provides methods to send short
messages via a las2peer network. It is stateless, so there exist no session
dependent values and it uses the las2peer shared storage for persistence.
This makes it possible to run and use the service either at each node that
joins a las2peer network or to just call the service from a las2peer instance
that joined a network that contains at least one node hosting this service.

Usage Hints
-------------------------------------

If you are new to las2peer and only want to start an instance
hosting this service, you need to build the projects default configuration with

  'ant' or 'ant jar'

Then you can make use of the start-script from the bin/ directory that comes
with this project.

  /bin/launchNode.sh
