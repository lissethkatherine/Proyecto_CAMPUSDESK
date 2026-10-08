package com.roles.usermanagement.domain.service;

public class UserRoles {
    public enum Role { ADMIN, CUSTOMER }

    public enum Authority {
        USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE, ROLE_ASSIGN, PERMISSION_ASSIGN, ROLE_MANAGE, PERMISSION_MANAGE,
        CUSTOMER_READ, CUSTOMER_CREATE, CUSTOMER_UPDATE, CUSTOMER_DELETE,
        PRODUCT_READ, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
        SALE_READ, SALE_CREATE, SALE_CANCEL, RANDOM_ORDER;

        public String value() {
            return this == RANDOM_ORDER ? "random_order" : name();
        }
    }
}