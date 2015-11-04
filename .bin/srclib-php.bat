@echo off
"%JAVA_HOME%/bin/java.exe" -Xmx4g -classpath "%~dp0/srclib-php.jar" com.sourcegraph.toolchain.php.Main %*
