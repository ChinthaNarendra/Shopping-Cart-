package com.ecom.util;

public enum OrderStatus {

    IN_PROGRESS(1, "In Progress"),
    ORDER_RECEIVED(2, "Received"),
    PRODUCT_PACKED(3, "Product Packed"),
    OUT_FOR_DELIVERY(4, "Out For Delivery"),
    DELIVERED(5, "Delivered"),
    CANCELLED(6, "Cancelled"),
    SUCCESS(7, "Success");

    private final Integer id;
    private final String name;

    OrderStatus(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Find enum by ID safely.
     */
    public static OrderStatus fromId(Integer id) {
        if (id == null) return null;
        for (OrderStatus st : values()) {
            if (st.id.equals(id)) {
                return st;
            }
        }
        return null;
    }

    /**
     * Find enum by display name (case-insensitive).
     */
    public static OrderStatus fromName(String name) {
        if (name == null) return null;
        for (OrderStatus st : values()) {
            if (st.name.equalsIgnoreCase(name)) {
                return st;
            }
        }
        return null;
    }
}
