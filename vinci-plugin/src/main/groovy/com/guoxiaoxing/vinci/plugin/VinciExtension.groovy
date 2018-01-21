package com.guoxiaoxing.vinci.plugin

class VinciExtension {

    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()
    List<String> ajcArgs = new ArrayList<>();

    public VinciExtension includeJarFilter(String... filters) {
        if (filters != null) {
            includeJarFilter.addAll(filters)
        }

        return this
    }

    public VinciExtension excludeJarFilter(String... filters) {
        if (filters != null) {
            excludeJarFilter.addAll(filters)
        }

        return this
    }

    public VinciExtension ajcArgs(String... ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }
        return this
    }
}

