package com.luxe.pricing.schema.types;

/**
 * Federation reference stub. The router hands us { __typename: "RoomType", id };
 * fields beyond `id` are resolved by other @DgsData methods on this subgraph,
 * or fetched from the owning subgraph by the router.
 */
public class RoomType {
    private String id;

    public RoomType() {}
    public RoomType(String id) { this.id = id; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
