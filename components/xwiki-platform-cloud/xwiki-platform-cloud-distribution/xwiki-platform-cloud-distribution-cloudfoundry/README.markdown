Introduction
============

This module builds a WAR that can be deployed on CloudFoundry. It uses the MySQL driver for storing data in the database, if a MySQL service is bound to the web application, otherwise it uses the embedded HSQLDB database.

How to deploy
-------------

Just run `vmc` in a directory with the CloudFoundry WAR and push the application.

If you bind a MySQL service to the application, then XWiki will use that service to store its data. Otherwise it will use the embedded HSQLDB.

Notes
-----

Deploying XWiki on standard CloudFoundry could cause several `Too many open files` exceptions. 

In order to prevent these errors on your local installation of CloudFoundry (or on your CloudFoundry micro instance) you can do the following:

* Go to `/var/vcap/packages/dea/lib/dea`. (On CloudFoundry micro you have to first login with the `vcap` user)
* Edit the file `agent.rb`.
* On line 538 replace the line `num_fds = limits['fds'] if limits['fds']` with `num_fds = 2048`.
* Restart your CloudFoundry server and redeploy XWiki.
