Filter Checkstyle violations to remove elements annotated with a given set of annotations.

This was primarily designed so that classes or methods marked as @Deprecated will not throw violations - there's no point refactoring something already marked for removal.

## Usage

The filter consists of two elements
1. AnnotationCheck: a "check" which records which violations are annotated with the annotations you have specified
2. AnnotationFilter: a filter which queries the state of the "check" to decide whether or not to include the violation

Your Checkstyle configuration must include both of these, for example:

```
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
    "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
    "https://checkstyle.org/dtds/configuration_1_3.dtd">
<module name="Checker">
    <module name="TreeWalker">
        <module name="TypeName"/>
        <module name="MethodLength"/>
        <module name="MethodName" />
        <module name="FinalLocalVariable" />
        <!-- more checks -->
        
        <module name="uk.co.michaelboyles.checkstyle.annotationfilter.AnnotationFilter">
            <property name="class" value="Deprecated"/>
            <property name="class" value="SuppressWarnings"/>
        </module>
    </module>
    <module name="uk.co.michaelboyles.checkstyle.annotationfilter.AnnotationCheck" />
</module>
```

Currently the filter just takes a class name, so there is no way to differentiate between two annotations with the same name in different packages.

The library JAR file must then be included on the classpath when Checkstyle runs. If you were running from the command line, it may look like this:

```
java -classpath checkstyle-8.14-all.jar:checkstyle-annotation-filter-1.0.jar com.puppycrawl.tools.checkstyle.Main -c checkstyle_rules.xml test.java
```

## License

This code was mostly adapted from SuppressWarningsFilter and SuppressWarningsHolder in Checkstyle, so is available under the same license: **LGPLv2.1**