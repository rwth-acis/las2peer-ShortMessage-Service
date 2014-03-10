LAS2peer-ShortMessage-Service
=============================

This is a middleware service for LAS2peer that provides methods to send short
messages via a LAS2peer network. It is stateless, so there exist no session
dependent values and it uses the LAS2peer shared storage for persistence.
This makes it possible to run and use the service either at each node that
joins a LAS2peer network or to just call the service from a LAS2peer instance
that joined a network that contains at least one node hosting this service.

Usage Hints

If you are new to LAS2peer and only want to start an instance
hosting this service, you can make use of the start-script from the scripts
directory that come with this project.

Since there currently exists no user manager application, you will have to
add each user as an XML-file to the "startup" directory. This directory will
be uploaded when you execute the start scripts. To produce agent XML-files,
you will have to make use of the LAS2peer ServiceAgentGenerator. At GitHub,
there exists a start-script to use this tool in the LAS2peer-Sample-Project
of the RWTH-ACIS organization.
