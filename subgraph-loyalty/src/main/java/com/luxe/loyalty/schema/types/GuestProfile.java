package com.luxe.loyalty.schema.types;

/**
 * Federation reference stub. The router hands us { __typename: "GuestProfile", id };
 * fields beyond `id` are resolved by other @DgsData methods on this subgraph,
 * or fetched from the owning subgraph by the router.
 */
public class GuestProfile {
    private String id;

    public GuestProfile() {}
    public GuestProfile(String id) { this.id = id; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
