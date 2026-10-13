package com.railway.reservation.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Graph data structure for railway routes.
 * Station = vertex, direct train route = edge.
 */
public class RouteGraph {

    private final Map<String, Set<String>> routes = new HashMap<>();

    public void addRoute(String source, String destination) {
        routes.computeIfAbsent(source, k -> new LinkedHashSet<>()).add(destination);
    }

    public List<String> getDestinations(String source) {
        return new ArrayList<>(routes.getOrDefault(source, Set.of()));
    }

    public Map<String, Set<String>> getRoutes() {
        return routes;
    }
}
