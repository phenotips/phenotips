Introduction
============

This is a configuration source that is able to retrieve properties from the environment. In particular this configuration source looks for properties in the following places:

* The OS environment
* The JVM environment
* The `xwiki.properties` file

The configuration source is also able to perform property remapping. Suppose that you have a property called `remap.PROPERTY1=PROPERTY2`, then the value of `PROPERTY1` will be that of `PROPERTY2`.

For example, if we have the following:

* `foo=foo`
* `bar=bar`
* `remap.foo=bar`

then the value of `foo` will be `bar`.
