itsfactory.realtime.access
===============

Access server for ITS Factory real-time public transport data. 

# About project
The repository contains files for spring enabled WEB Server. The project builds a war file which can be deployed inside Tomcat (or other servlet container). The server exposes various APIs which can be used to fetch data hosted by ITS Factory. Currently the server expects data to be available in memcached instance (which address is configured in 
    src/main/webapp/WEB-INF/mvc-dispatcher-servlet.xml
An example configuration could be
```xml
	<bean id="memcached-datasource" class="fi.itsfactory.realtime.access.MemcachedDatasource" scope="prototype">
		<constructor-arg index="0" value="localhost:11211"></constructor-arg>
		<constructor-arg index="1" value="SIRI_VM_LATEST_ALL"></constructor-arg>
	</bean>
```
# Building the project
The project is built with maven3
    mvn package
The project however has dependency to [itsfactory.siri.bindings.v13](https://github.com/ITSFactory/itsfactory.siri.bindings.v13) project. You will have to compile the dependency first and install it to yor local maven repository, or the build will fail.

