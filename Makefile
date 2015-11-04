ifeq ($(OS),Windows_NT)
	GRADLEW = .\gradlew.bat
else
	GRADLEW = ./gradlew
endif

SRC = $(shell /usr/bin/find ./src -type f)

.PHONY: default install test test-gen clean dist

default: install

build/libs/srclib-php-0.0.1-SNAPSHOT.jar: build.gradle ${SRC}
	${GRADLEW} jar

.bin/srclib-php.jar: build/libs/srclib-php-0.0.1-SNAPSHOT.jar
	cp build/libs/srclib-php-0.0.1-SNAPSHOT.jar .bin/srclib-php.jar

install: .bin/srclib-php.jar

test: .bin/srclib-php.jar
	src -v test -m program

test-gen: .bin/srclib-php.jar
	src -v test -m program --gen

clean:
	rm -f .bin/srclib-php.jar
	rm -rf build


dist:
	echo hello