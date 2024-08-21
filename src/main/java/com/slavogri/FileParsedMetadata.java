package com.slavogri;

import java.util.List;

record FileParsedMetadata (String packageName, List<String> dependantPackages, List<String> classes){}