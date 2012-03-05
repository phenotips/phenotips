Introduction
============

This module provides a way for configuring Hibernate by overriding values of Hibernate properties found in the standard `hibernate.cfg.xml`

This configurator takes the `hibernate.cfg.xml` and rewrites it by changing the values associated to properties if there exist in the environment a property with the same name prefixed by `hibernate.`

For example in order to override the `connection.url` in the Hibernate configuration file, there should exist in the environment a property called `hibernate.connection.url`. In this case the value of this property will override the one that was present in the `hibernate.cfg.xml`

The configuration source used is the one of the `wiki-platform-cloud-configuration-default` that allows for property remapping.

