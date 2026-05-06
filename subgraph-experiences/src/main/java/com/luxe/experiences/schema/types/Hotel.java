package com.luxe.experiences.schema.types;

/**
 * Federation reference stub. The router hands us { __typename: "Hotel", id };
 * fields beyond `id` are resolved by other @DgsData methods on this subgraph,
 * or fetched from the owning subgraph by the router.
 */
public class Hotel {
    private String id;

    public Hotel() {}
    public Hotel(String id) { this.id = id; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
