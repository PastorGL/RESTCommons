package io.github.pastorgl.rest.entity;

public interface Token {
    String getAccess();

    String getRefresh();

    long getExpirationDate();
}
