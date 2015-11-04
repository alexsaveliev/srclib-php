package com.sourcegraph.toolchain.php;

import java.util.Collection;
import java.util.HashSet;

public class ClassInfo {

    Collection<String> extendsClasses = new HashSet<>();
    Collection<String> implementsClasses = new HashSet<>();
    Collection<String> usesTraits = new HashSet<>();

    Collection<String> definesMethods = new HashSet<>();
    Collection<String> implementsMethods = new HashSet<>();
}